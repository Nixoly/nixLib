package dev.nixoly.nixlib.version;

public final class ServerCapabilities {

    private final ServerType type;
    private final ServerVersion version;

    public ServerCapabilities(ServerType type, ServerVersion version) {
        this.type = type;
        this.version = version;
    }

    public ServerType type() { return type; }
    public ServerVersion version() { return version; }

    public boolean supportsFolia() {
        return type.isMultithreaded();
    }

    public boolean supportsAdventure() {
        return type.isPaperBased();
    }

    public boolean supportsTextDisplays() {
        return version.isAtLeast(ServerVersion.V1_19_4);
    }

    public boolean supportsBlockDisplays() {
        return version.isAtLeast(ServerVersion.V1_19_4);
    }

    public boolean supportsItemComponentApi() {
        return version.isAtLeast(ServerVersion.V1_20_5);
    }

    public boolean supportsPersistentDataContainer() {
        return version.isAtLeast(ServerVersion.V1_16_5);
    }

    public boolean supportsModernSounds() {
        return version.isAtLeast(ServerVersion.V1_19);
    }

    @Override
    public String toString() {
        return "ServerCapabilities{" + type + " " + version + "}";
    }
}
