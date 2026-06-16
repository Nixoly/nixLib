package dev.nixoly.nixlib.config.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class SyncOptions {

    private String versionKey = "config-version";
    private Consumer<String> warningSink = null;
    private boolean dropUnknownKeys = true;
    private boolean versionKeyLast = true;
    private boolean freeformRoot = false;
    private boolean mergeDuplicateKeys = false;
    private final List<String> freeformPaths = new ArrayList<>(0);

    public static SyncOptions create() {
        return new SyncOptions();
    }

    public SyncOptions versionKey(String key) {
        this.versionKey = (key == null || key.isBlank()) ? "config-version" : key;
        return this;
    }

    public SyncOptions onWarning(Consumer<String> sink) {
        this.warningSink = sink;
        return this;
    }

    public SyncOptions dropUnknownKeys(boolean drop) {
        this.dropUnknownKeys = drop;
        return this;
    }

    public SyncOptions versionKeyLast(boolean last) {
        this.versionKeyLast = last;
        return this;
    }

    public SyncOptions freeformRoot(boolean freeform) {
        this.freeformRoot = freeform;
        return this;
    }

    public SyncOptions freeform(String... paths) {
        if (paths == null) return this;
        for (String p : paths) {
            if (p != null && !p.isBlank()) {
                freeformPaths.add(p);
            }
        }
        return this;
    }

    public SyncOptions freeform(Collection<String> paths) {
        if (paths == null) return this;
        for (String p : paths) {
            if (p != null && !p.isBlank()) {
                freeformPaths.add(p);
            }
        }
        return this;
    }

    public SyncOptions mergeDuplicateKeys() {
        this.mergeDuplicateKeys = true;
        return this;
    }

    public SyncOptions mergeDuplicateKeys(boolean merge) {
        this.mergeDuplicateKeys = merge;
        return this;
    }

    String versionKey() { return versionKey; }

    boolean shouldDropUnknown() { return dropUnknownKeys; }

    boolean shouldVersionKeyLast() { return versionKeyLast; }

    boolean freeformRoot() { return freeformRoot; }

    boolean shouldMergeDuplicateKeys() { return mergeDuplicateKeys; }

    List<String> freeformPaths() { return Collections.unmodifiableList(freeformPaths); }

    boolean isFreeform(String path) {
        if (freeformPaths.isEmpty() || path == null) return false;
        for (String fp : freeformPaths) {
            if (path.equals(fp)) return true;
            if (path.startsWith(fp + ".")) return true;
        }
        return false;
    }

    void warn(String message) {
        if (warningSink != null) warningSink.accept(message);
    }
}
