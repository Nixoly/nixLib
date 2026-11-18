# nixLib

A small-ish utility library for Paper/Folia/Canvas plugins. Born out of the
"every project ends up rewriting the same config loader and GUI builder"
pain. It is NMS-free, ships its own annotation-driven config system and runs
the same code on plain Bukkit, Folia and CraftCanvasMC.

Target server range: **Minecraft 1.16.5 - latest**. Modern features (Folia
regions, newer persistent data API) require **1.19.4+**, older versions fall
back through `ServerCapabilities`.

## Modules

- `nixlib-api` - the actual library. Add this to your plugin.
- `nixlib-core` - Bukkit-side implementations (BukkitScheduler, GUI listener,
  PlaceholderAPI bridge). Shade it together with `api`.
- `nixlib-folia` - Folia/Canvas region-aware scheduler. Optional. Shade it if
  you want to support multithreaded servers.

## Getting it (Gradle)

GitHub Releases (modular jars — primary):

```kotlin
repositories {
    mavenCentral()
    exclusiveContent {
        forRepositories(
            ivy {
                url = uri("https://github.com/Nixoly/nixLib/releases/download/1.1.0")
                patternLayout { artifact("[artifact]-[revision].[ext]") }
                metadataSources { artifact() }
            }
        )
        filter { includeGroup("dev.nixoly.nixLib") }
    }
}

dependencies {
    implementation("dev.nixoly.nixLib:api:1.1.0")
    implementation("dev.nixoly.nixLib:core:1.1.0")
    implementation("dev.nixoly.nixLib:folia:1.1.0")
}
```

Every semver tag push runs `.github/workflows/release.yml` — builds `api`, `core`, `folia` and uploads all jars to GitHub Releases plus GitHub Packages (`https://maven.pkg.github.com/Nixoly/nixLib`).

Local dev: `./gradlew publishAllToMavenLocal` then `mavenLocal()` with the same `dev.nixoly.nixLib:<module>:<version>` coordinates.

Relocate it into your own package when shading so two plugins using nixLib at
different versions don't fight each other:

```kotlin
tasks.shadowJar {
    relocate("dev.nixoly.nixlib", "your.plugin.libs.nixlib")
}
```

## Bootstrap

Call `NixLib.bootstrap(this)` once in `onEnable()` and `NixLib.shutdown()` in
`onDisable()`. That wires up the scheduler and version detection.

```java
public final class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        NixLib.bootstrap(this);
    }

    @Override
    public void onDisable() {
        NixLib.shutdown();
    }
}
```

## Config

Annotation-driven. Fields are filled from disk, defaults are written back, and
the file keeps its comments after a save. Schema version + per-step migration
is built in.

```java
@ConfigVersion(value = 2, key = "config-version")
public final class MyConfig extends Config {

    @Path("messages.prefix")
    @Comment("Prefix shown in front of every plugin message.")
    public String prefix = "&6[MyPlugin]&r ";

    @Path("limits.max-homes")
    @Comment({"How many /home entries a player may have.", "Capped at 50."})
    @Range(min = 1, max = 50)
    public int maxHomes = 3;

    @Path("server-type")
    @OneOf({"survival", "creative", "skyblock"})
    public String serverType = "survival";

    @Override
    protected void registerMigrations(MigrationRegistry registry) {
        registry.register(1, 2, ctx -> {
            // old key 'max_homes' -> nested limits.max-homes
            Object legacy = ctx.data().remove("max_homes");
            if (legacy != null) Nodes.set(ctx.data(), "limits.max-homes", legacy);
        });
    }
}
```

```java
MyConfig cfg = new MyConfig();
cfg.load(getDataFolder().toPath().resolve("config.yml"));
for (String w : cfg.warnings()) getLogger().warning(w);
```

If a key is missing, mistyped or fails its validator, the default value is
kept and a warning is added to `cfg.warnings()`. The plugin never crashes
because somebody wrote `max-homes: banana`.

## Scheduler

One interface, three runtimes. Pick a method based on *what the task touches*,
not what server it runs on - nixLib routes the rest.

```java
Scheduler sched = NixLib.get().scheduler();

sched.runGlobalLater(() -> Bukkit.broadcast(...), 20L);
sched.runAt(player.getLocation(), () -> player.teleport(spawn));
sched.runFor(entity, () -> entity.remove());
sched.runAsync(() -> writeToDatabase());
```

On paper/spigot it falls back to the standard BukkitScheduler. On Folia and
Canvas it uses the region-aware scheduler so you don't `IllegalStateException`
yourself on every teleport.

## World (Folia / Canvas safe block reads)

Packet listeners and async tasks must not call `World#rayTraceBlocks` or other
block APIs on Folia and Canvas — the Netty thread is not the owning region
thread and you get `getCurrentWorldData() is null` style failures.

`WorldThreadAccess` tells you whether the current thread may read blocks for an
entity or chunk. On Paper/Spigot that is the main thread; on Folia/Canvas it
uses Moonrise `TickThread.isTickThreadFor`.

`CachedBlockRayTrace` wraps a solid-block line-of-sight probe with a per-player
cache:

```java
Scheduler sched = NixLib.get().scheduler();

// Read is cache-only on every thread: it never raycasts inline
boolean blockTargeted = CachedBlockRayTrace.solidBlockInReach(player, 5.0);

// Keep cache warm from movement / use packets while gliding
CachedBlockRayTrace.refreshIfStale(sched, player, 5.0);

// On quit
CachedBlockRayTrace.clear(player.getUniqueId());
```

`solidBlockInReach` never calls `rayTraceBlocks` itself — it only reads the
per-player cache, so it is free to call from a server, region, or Netty thread.
The raycast happens exclusively inside `refreshIfStale`, which probes inline on
a safe thread and otherwise schedules the read onto the owning region/main
thread. Each cache entry remembers the eye position and view angles from its
last probe: while the player keeps looking the same way (under 2 degrees of
rotation and half a block of movement), no new raycast fires at all, and even
after the view changes probes are limited to one per 250 ms per entity — on
every thread, inline included. A stale entry whose view snapshot still matches
keeps serving its cached answer; when the cache is missing or older than
300 ms with a changed view, `solidBlockInReach` returns `true` (assume
block-target) so callers can skip detections instead of throwing or
false-flagging.

## GUI

```java
Gui gui = new Gui("Shop", 3);
gui.border(ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
gui.setItem(13,
    ItemBuilder.of(Material.DIAMOND).name("&bBuy diamond").lore("&7Costs 100 coins").build(),
    ctx -> {
        ctx.player().sendMessage(ColorUtils.parse("&aBought!"));
        ctx.close();
    });
gui.open(player);
```

For paginated content there is `PagedGui`. Register the listener once with
`GuiManager.register(this)` after `NixLib.bootstrap`.

A few things happen for you so menus stay cheap and keep the client in sync:

- Opening or closing a menu from inside a click handler (`gui.open`, `ctx.close()`,
  `GuiNavigation.close(player)`) runs on the next tick. Bukkit does not allow it in the middle of
  `InventoryClickEvent`, and if one click asks for several menus only the last one opens. Call
  `GuiNavigation.close(player)` instead of `player.closeInventory()` in your own handlers.
- When a menu closes for good, the player's inventory and cursor are resent on the next tick so
  nothing the client predicted during the menu survives into its own inventory.
- Each player gets one handled click per 150 ms; faster clicks are cancelled without running
  handlers, and the double-click that follows a click never repeats it. Change it per menu with
  `gui.clickCooldown(Duration.ofMillis(...))`, `Duration.ZERO` turns it off.
- `Gui.from(inventory)` tells you whether an inventory is a menu without the block state copy
  Paper's `getHolder()` makes, so it is safe in listeners that see every chest click.
- `ConfigItem.build` reuses an item built in the last minute when the section renders to the same
  text, so reopening a menu only rebuilds the items whose placeholders changed. You always get your
  own copy of the stack.

Large database-backed menus can build only the visible slice with
`entryWindow(firstEntry, totalEntries)`. Add that page's entries, then open the
matching page; `totalPages()` still reflects the full result count without
allocating an item for every row.

## Database

HikariCP pools with a small query layer. The pool name is what Hikari prints in the console and
what the worker threads are called; set it to whatever the plugin wants, otherwise it defaults to
the plugin name. Nothing is logged under nixLib's name.

```java
JdbcDatabase db = JdbcDatabase.builder(DatabaseType.SQLITE)
        .file(getDataFolder().toPath().resolve("data.db"))
        .poolName("Homes")
        .build();

JdbcDatabase remote = JdbcDatabase.builder(DatabaseType.POSTGRESQL)
        .host("db.local")
        .database("survival")
        .credentials("mc", "secret")
        .maximumPoolSize(8)
        .poolName(getName())
        .build();
```

Or straight from `config.yml`:

```yaml
database:
  type: mariadb          # sqlite, h2, mysql, mariadb, postgresql, sqlserver
  file: data.db          # sqlite / h2, relative to the data folder
  host: localhost
  port: 3306
  database: survival
  username: mc
  password: secret
  pool:
    maximum-size: 10
    connection-timeout-ms: 5000
  properties:
    useSSL: false
```

```java
JdbcDatabase db = JdbcDatabase.fromConfig(getConfig().getConfigurationSection("database").getValues(false),
        getDataFolder().toPath()).poolName(getName()).build();
```

| Type | Driver to add | Bundled with Paper |
|---|---|---|
| `sqlite` | `org.xerial:sqlite-jdbc` | yes |
| `mysql` | `com.mysql:mysql-connector-j` | yes |
| `mariadb` | `org.mariadb.jdbc:mariadb-java-client` | no |
| `postgresql` | `org.postgresql:postgresql` | no |
| `h2` | `com.h2database:h2` | no |
| `sqlserver` | `com.microsoft.sqlserver:mssql-jdbc` | no |

Drivers that Paper does not ship go into plugin.yml `libraries:` or the shaded jar. A missing
driver fails with a message naming the coordinate instead of a Hikari stack trace.

```java
db.execute("CREATE TABLE IF NOT EXISTS homes (owner TEXT, name TEXT, world TEXT, x REAL, y REAL, z REAL)");

QueryBuilder q = QueryBuilder.select("name", "world", "x", "y", "z")
        .from("homes")
        .where("owner = ?", player.getUniqueId().toString());

List<Home> homes = db.queryMany(q.sql(), rs -> new Home(
        rs.getString("name"),
        rs.getString("world"),
        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z")
), q.parameters());
```

Transactions, batches, generated keys and upserts:

```java
db.transaction(connection -> {
    // one connection, committed when this returns, rolled back if it throws
    return null;
});

db.batch("INSERT INTO kills (killer, victim) VALUES (?, ?)", rows);   // one round trip, all or nothing
long id = db.insert("INSERT INTO reports (body) VALUES (?)", text);   // generated key

// INSERT ... ON CONFLICT / ON DUPLICATE KEY / MERGE, whichever the database speaks
db.upsert("stats", List.of("uuid"), List.of("uuid", "kills", "deaths"), uuid, kills, deaths);
```

Every call has an `*Async` twin running on the database's own workers. They are drained during
shutdown, and the close itself can stay off the server thread:

```java
db.closeAsync(Duration.ofSeconds(2));
```

SQLite always runs on one connection because the driver serializes writes. Network databases get
finite connect and socket timeouts so an unreachable server cannot hold shutdown indefinitely.
`SqliteDatabase` and `MysqlDatabase` still work as before and take an optional pool name.

## Other bits

- `ColorUtils` - Adventure-only. `parse("&aHi &#ff0000there")` and
  `miniMessage("<gradient:red:gold>nixLib</gradient>")` both return a
  `Component`. Repeated mixed legacy/MiniMessage translations use a bounded
  component cache. No `ChatColor` anywhere.
- `EventBus` - small internal bus for plugin-local events, with `@Subscribe`
  reflection registration. Not a replacement for Bukkit events, just
  convenient for game logic.
- `PlaceholderRegistry` + `PlaceholderApiBridge` - register placeholders once,
  expose them through PlaceholderAPI only if the plugin is installed.
- `LocationSerializer` / `ItemStackSerializer` (Base64) / `BlockDataSerializer`
  for stashing things in SQL columns or PDC values.

## Building from source

```bash
./gradlew clean test       # full test suite
./gradlew build            # produces api/core/folia jars
```

Toolchain Java 21, output bytecode `--release 17`.

## License

See [LICENSE](LICENSE). Personal and public plugins are fine, modifications
and forks are fine. Selling the library or redistributing paid copies is not.
Credits stay.
