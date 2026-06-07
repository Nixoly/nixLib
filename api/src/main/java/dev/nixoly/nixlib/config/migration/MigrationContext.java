package dev.nixoly.nixlib.config.migration;

import dev.nixoly.nixlib.config.yaml.Nodes;

import java.util.Map;

public final class MigrationContext {

    private final Map<String, Object> root;
    private final int from;
    private final int to;

    MigrationContext(Map<String, Object> root, int from, int to) {
        this.root = root;
        this.from = from;
        this.to = to;
    }

    public int from() { return from; }

    public int to() { return to; }

    public Map<String, Object> root() { return root; }

    public Object get(String path) {
        return Nodes.get(root, path);
    }

    public boolean has(String path) {
        return Nodes.contains(root, path);
    }

    public void set(String path, Object value) {
        Nodes.set(root, path, value);
    }

    public void rename(String oldPath, String newPath) {
        Object value = Nodes.get(root, oldPath);
        if (value == null) return;
        Nodes.remove(root, oldPath);
        Nodes.set(root, newPath, value);
    }

    public boolean remove(String path) {
        return Nodes.remove(root, path);
    }
}
