package dev.nixoly.nixlib.config.template;

import dev.nixoly.nixlib.version.SemanticVersion;

import java.util.Map;

public final class SyncResult {

    private final String mergedYaml;
    private final Map<String, Object> mergedData;
    private final boolean changed;
    private final SemanticVersion bundledVersion;
    private final SemanticVersion userVersion;

    SyncResult(String mergedYaml, Map<String, Object> mergedData, boolean changed,
               SemanticVersion bundledVersion, SemanticVersion userVersion) {
        this.mergedYaml = mergedYaml;
        this.mergedData = mergedData;
        this.changed = changed;
        this.bundledVersion = bundledVersion;
        this.userVersion = userVersion;
    }

    public String mergedYaml() { return mergedYaml; }

    public Map<String, Object> mergedData() { return mergedData; }

    public boolean changed() { return changed; }

    public SemanticVersion bundledVersion() { return bundledVersion; }

    public SemanticVersion userVersion() { return userVersion; }

    public boolean isUserBehind() {
        return userVersion.isOlderThan(bundledVersion);
    }
}
