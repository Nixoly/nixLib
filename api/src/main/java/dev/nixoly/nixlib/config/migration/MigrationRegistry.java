package dev.nixoly.nixlib.config.migration;

import dev.nixoly.nixlib.config.ConfigException;

import java.util.HashMap;
import java.util.Map;

public final class MigrationRegistry {

    private final Map<Integer, Entry> steps = new HashMap<>();

    public MigrationRegistry register(int from, int to, MigrationStep step) {
        if (to != from + 1) {
            throw new IllegalArgumentException("migration must be sequential, got " + from + " -> " + to);
        }
        if (steps.containsKey(from)) {
            throw new IllegalStateException("duplicate migration from version " + from);
        }
        steps.put(from, new Entry(to, step));
        return this;
    }

    public void apply(int fromVersion, int targetVersion, Map<String, Object> data) {
        int current = fromVersion;
        int safety = 0;
        while (current < targetVersion) {
            Entry next = steps.get(current);
            if (next == null) {
                throw new ConfigException("no migration registered from v" + current
                        + "; cannot reach v" + targetVersion);
            }
            next.step.migrate(new MigrationContext(data, current, next.to));
            current = next.to;
            if (++safety > 256) {
                throw new ConfigException("migration loop detected near v" + current);
            }
        }
    }

    private record Entry(int to, MigrationStep step) {}
}
