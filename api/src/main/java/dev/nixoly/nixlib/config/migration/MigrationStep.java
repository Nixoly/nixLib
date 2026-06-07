package dev.nixoly.nixlib.config.migration;

@FunctionalInterface
public interface MigrationStep {

    void migrate(MigrationContext ctx);
}
