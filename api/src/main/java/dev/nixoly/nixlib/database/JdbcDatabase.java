package dev.nixoly.nixlib.database;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongConsumer;

public final class JdbcDatabase extends HikariDatabase {

    private JdbcDatabase(HikariConfig config, DatabaseType type) {
        super(config, type);
    }

    public static Builder builder(DatabaseType type) {
        return new Builder(type);
    }

    /**
     * Fills a builder from a plugin config section. Recognised keys: {@code type}, {@code file},
     * {@code host}, {@code port}, {@code database}, {@code username}, {@code password},
     * {@code pool-name}, {@code jdbc-url}, a {@code pool} section ({@code maximum-size},
     * {@code minimum-idle}, {@code connection-timeout-ms}, {@code idle-timeout-ms},
     * {@code max-lifetime-ms}) and a {@code properties} section passed to the driver.
     *
     * <p>Keys that do not apply to the chosen type are ignored, so one config can hold both file and
     * server settings. A relative {@code file} resolves against {@code dataFolder}. Nested sections may
     * be maps or Bukkit {@link ConfigurationSection}s, so {@code section.getValues(false)} works as is.
     *
     * @throws IllegalArgumentException when {@code type} is missing or unknown, or a number is malformed
     */
    public static Builder fromConfig(Map<String, ?> section, Path dataFolder) {
        String typeName = text(section, "type");
        if (typeName == null) {
            throw new IllegalArgumentException("database config has no 'type'");
        }
        Builder builder = builder(DatabaseType.parse(typeName))
                .jdbcUrl(text(section, "jdbc-url"))
                .host(text(section, "host"))
                .database(text(section, "database"))
                .credentials(text(section, "username"), text(section, "password"))
                .poolName(text(section, "pool-name"));
        String file = text(section, "file");
        if (file != null) {
            builder.file(dataFolder.resolve(file));
        }
        readNumber(section, "port", port -> builder.port(Math.toIntExact(port)));

        Map<String, ?> pool = subsection(section, "pool");
        readNumber(pool, "maximum-size", size -> builder.maximumPoolSize(Math.toIntExact(size)));
        readNumber(pool, "minimum-idle", idle -> builder.minimumIdle(Math.toIntExact(idle)));
        readNumber(pool, "connection-timeout-ms", ms -> builder.connectionTimeout(Duration.ofMillis(ms)));
        readNumber(pool, "idle-timeout-ms", ms -> builder.idleTimeout(Duration.ofMillis(ms)));
        readNumber(pool, "max-lifetime-ms", ms -> builder.maxLifetime(Duration.ofMillis(ms)));

        subsection(section, "properties").forEach((key, value) -> builder.property(key, String.valueOf(value)));
        return builder;
    }

    @Override
    public String dialect() {
        return type().id();
    }

    private static String text(Map<String, ?> section, String key) {
        Object value = section.get(key);
        return value == null ? null : value.toString();
    }

    private static void readNumber(Map<String, ?> section, String key, LongConsumer target) {
        Object value = section.get(key);
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            target.accept(number.longValue());
            return;
        }
        try {
            target.accept(Long.parseLong(value.toString().trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a whole number, got '" + value + "'");
        }
    }

    private static Map<String, ?> subsection(Map<String, ?> section, String key) {
        Object value = section.get(key);
        if (value == null) {
            return Map.of();
        }
        if (value instanceof ConfigurationSection nested) {
            return nested.getValues(false);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), v));
            return copy;
        }
        throw new IllegalArgumentException(key + " must be a section, got '" + value + "'");
    }

    /** String and path setters take null as "not set", which keeps the default. */
    public static final class Builder {

        private final DatabaseType type;
        private final Map<String, String> properties;
        private String jdbcUrl;
        private Path file;
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;
        private String poolName;
        private int maximumPoolSize;
        private int minimumIdle = 1;
        private Duration connectionTimeout;
        private Duration idleTimeout = Duration.ofMinutes(5);
        private Duration maxLifetime;

        private Builder(DatabaseType type) {
            this.type = Objects.requireNonNull(type, "type");
            this.properties = new LinkedHashMap<>(type.dataSourceProperties());
            this.port = type.defaultPort();
            this.maximumPoolSize = type.defaultPoolSize();
            // Callers of a file database queue for its single connection, so waiting is the normal case.
            // A network database that takes longer than a few seconds is usually unreachable.
            this.connectionTimeout = type.fileBased() ? Duration.ofSeconds(30) : Duration.ofSeconds(5);
        }

        /** Uses this URL as is instead of building one from file or host settings. */
        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder file(Path file) {
            this.file = file;
            return this;
        }

        /** Server host for network databases, {@code localhost} when not set. */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("port out of range: " + port);
            }
            this.port = port;
            return this;
        }

        public Builder database(String database) {
            this.database = database;
            return this;
        }

        public Builder credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        /** The name Hikari prints in the console and worker threads carry. Defaults to the plugin name. */
        public Builder poolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        /** Ignored for SQLite, which always runs on a single connection. */
        public Builder maximumPoolSize(int size) {
            if (size < 1) {
                throw new IllegalArgumentException("maximum pool size must be at least 1, got " + size);
            }
            this.maximumPoolSize = size;
            return this;
        }

        public Builder minimumIdle(int idle) {
            if (idle < 0) {
                throw new IllegalArgumentException("minimum idle must not be negative, got " + idle);
            }
            this.minimumIdle = idle;
            return this;
        }

        /** How long a caller waits for a connection before failing. Hikari's floor is 250 ms. */
        public Builder connectionTimeout(Duration timeout) {
            if (timeout.toMillis() < 250L) {
                throw new IllegalArgumentException("connection timeout must be at least 250 ms, got " + timeout);
            }
            this.connectionTimeout = timeout;
            return this;
        }

        public Builder idleTimeout(Duration timeout) {
            this.idleTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        public Builder maxLifetime(Duration lifetime) {
            this.maxLifetime = Objects.requireNonNull(lifetime, "lifetime");
            return this;
        }

        /** Sets a driver property, overriding the type's default. A null value drops the default. */
        public Builder property(String name, String value) {
            if (value == null) {
                properties.remove(name);
            } else {
                properties.put(name, value);
            }
            return this;
        }

        /**
         * Opens the pool.
         *
         * @throws DatabaseException     when the driver is not on the classpath or the database cannot be reached
         * @throws IllegalStateException when neither a JDBC URL nor the file or database name is set
         */
        public JdbcDatabase build() {
            return new JdbcDatabase(hikariConfig(), type);
        }

        HikariConfig hikariConfig() {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(resolveUrl());
            cfg.setDriverClassName(loadableDriver());
            cfg.setUsername(username);
            cfg.setPassword(password);
            cfg.setPoolName(poolName);

            // SQLite allows one writer at a time; more connections only turn into SQLITE_BUSY errors.
            int maxSize = type == DatabaseType.SQLITE ? 1 : maximumPoolSize;
            int minIdle = Math.min(minimumIdle, maxSize);
            cfg.setMaximumPoolSize(maxSize);
            cfg.setMinimumIdle(minIdle);
            cfg.setConnectionTimeout(connectionTimeout.toMillis());
            cfg.setValidationTimeout(Math.min(2_000L, connectionTimeout.toMillis()));
            // Hikari logs a warning for an idle timeout on a fixed-size pool, where it does nothing.
            if (minIdle < maxSize) {
                cfg.setIdleTimeout(idleTimeout.toMillis());
            }
            if (maxLifetime != null) {
                cfg.setMaxLifetime(maxLifetime.toMillis());
            }
            properties.forEach(cfg::addDataSourceProperty);
            return cfg;
        }

        private String resolveUrl() {
            if (jdbcUrl != null) {
                return jdbcUrl;
            }
            if (type.fileBased()) {
                if (file == null) {
                    throw new IllegalStateException(type.id() + " needs a file or a JDBC URL");
                }
                createParentDirectory(file);
                return type.jdbcUrl(file);
            }
            if (database == null) {
                throw new IllegalStateException(type.id() + " needs a database name or a JDBC URL");
            }
            return type.jdbcUrl(host == null ? "localhost" : host, port, database);
        }

        // A plugin's data folder does not exist before its first save, and SQLite will not create it.
        private static void createParentDirectory(Path file) {
            Path parent = file.toAbsolutePath().getParent();
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new DatabaseException("cannot create database folder " + parent, e);
            }
        }

        private String loadableDriver() {
            for (String className : type.driverClassNames()) {
                if (isLoadable(className)) {
                    return className;
                }
            }
            throw new DatabaseException(type.id() + " driver " + type.driverClassName()
                    + " is not on the classpath. Add " + type.driverArtifact()
                    + " to plugin.yml libraries or shade it into the plugin.");
        }

        // Same loaders in the same order as HikariConfig#setDriverClassName, so a pass here means Hikari
        // finds the class too instead of failing with its own error.
        private static boolean isLoadable(String className) {
            ClassLoader context = Thread.currentThread().getContextClassLoader();
            return (context != null && isVisible(className, context))
                    || isVisible(className, HikariConfig.class.getClassLoader());
        }

        private static boolean isVisible(String className, ClassLoader loader) {
            try {
                Class.forName(className, false, loader);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
