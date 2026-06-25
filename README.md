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

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Nixoly.nixLib:api:1.0.2")
    implementation("com.github.Nixoly.nixLib:core:1.0.2")
    implementation("com.github.Nixoly.nixLib:folia:1.0.2")
}
```

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
thread (no more than once per 75 ms per entity). When the cache is missing or
older than 100 ms, `solidBlockInReach` returns `true` (assume block-target) so
callers can skip detections instead of throwing or false-flagging.

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

For paginated content there is `PagedGui`. The listener is registered for you
when you call `NixLib.bootstrap` - no need to remember to add it to your
`PluginManager`.

## Database

Hikari + tiny query builder. SQLite and MySQL are wired up; bring your own
driver for anything else by extending `HikariDatabase`.

```java
Database db = new SqliteDatabase(new File(getDataFolder(), "data.db"));
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

## Other bits

- `ColorUtils` - Adventure-only. `parse("&aHi &#ff0000there")` and
  `miniMessage("<gradient:red:gold>nixLib</gradient>")` both return a
  `Component`. No `ChatColor` anywhere.
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
