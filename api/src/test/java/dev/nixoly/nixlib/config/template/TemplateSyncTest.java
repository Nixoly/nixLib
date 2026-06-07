package dev.nixoly.nixlib.config.template;

import dev.nixoly.nixlib.config.yaml.YamlReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateSyncTest {

    private static final String TEMPLATE_V101 = """
            # Top banner comment
            license-key: "FILL_HERE"

            # Detection block
            detection:
              # how many strikes before action
              max-warnings: 6
              # new field in 1.0.1
              reset-time: 10
              # nested block introduced in 1.0.1
              delayed-punishment:
                enabled: false
                delay-ticks: 20

            # Lists default
            commands:
              - "kick %player%"

            config-version: "1.0.1"
            """;

    @Test
    void newTemplateKeyIsAddedWhileUserValuesArePreserved() {
        String userYaml = """
                license-key: "MY-KEY"
                detection:
                  max-warnings: 12
                commands:
                  - "ban %player%"
                config-version: "1.0.0"
                """;

        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml, SyncOptions.create());

        assertThat(result.isUserBehind()).isTrue();
        assertThat(result.bundledVersion().toString()).isEqualTo("1.0.1");
        assertThat(result.userVersion().toString()).isEqualTo("1.0.0");
        assertThat(result.mergedData().get("license-key")).isEqualTo("MY-KEY");

        Map<?, ?> detection = (Map<?, ?>) result.mergedData().get("detection");
        assertThat(detection.get("max-warnings")).isEqualTo(12);
        assertThat(detection.get("reset-time")).isEqualTo(10);
        assertThat(detection.get("delayed-punishment")).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        List<Object> commands = (List<Object>) result.mergedData().get("commands");
        assertThat(commands).containsExactly("ban %player%");
    }

    @Test
    void newKeyInsideExistingSectionIsAdded() {
        String userYaml = """
                license-key: "MY-KEY"
                detection:
                  max-warnings: 99
                  reset-time: 30
                commands:
                  - "kick %player%"
                config-version: "1.0.0"
                """;

        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml, SyncOptions.create());
        Map<?, ?> detection = (Map<?, ?>) result.mergedData().get("detection");
        assertThat(detection.get("max-warnings")).isEqualTo(99);
        assertThat(detection.get("reset-time")).isEqualTo(30);
        assertThat(detection.get("delayed-punishment")).isInstanceOf(Map.class);
        Map<?, ?> dp = (Map<?, ?>) detection.get("delayed-punishment");
        assertThat(dp.get("enabled")).isEqualTo(false);
        assertThat(dp.get("delay-ticks")).isEqualTo(20);
    }

    @Test
    void unknownUserKeysAreDroppedAndReported() {
        String userYaml = """
                license-key: "FILL_HERE"
                detection:
                  max-warnings: 6
                  reset-time: 10
                  delayed-punishment:
                    enabled: false
                    delay-ticks: 20
                  removed-old-key: "leftover"
                commands:
                  - "kick %player%"
                ancient-root-key: 42
                config-version: "1.0.1"
                """;
        List<String> warnings = new ArrayList<>();
        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml,
                SyncOptions.create().onWarning(warnings::add));

        assertThat(result.mergedYaml()).doesNotContain("removed-old-key");
        assertThat(result.mergedYaml()).doesNotContain("ancient-root-key");
        assertThat(warnings).anyMatch(w -> w.contains("detection.removed-old-key"));
        assertThat(warnings).anyMatch(w -> w.contains("ancient-root-key"));
    }

    @Test
    void emptyUserScalarIsKeptNotRefilled() {
        String userYaml = """
                license-key: ""
                detection:
                  max-warnings: 6
                  reset-time: 10
                  delayed-punishment:
                    enabled: false
                    delay-ticks: 20
                commands:
                  - "kick %player%"
                config-version: "1.0.1"
                """;
        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml, SyncOptions.create());
        assertThat(result.mergedData().get("license-key")).isEqualTo("");
    }

    @Test
    void emptyUserListIsKept() {
        String userYaml = """
                license-key: "MY"
                detection:
                  max-warnings: 6
                  reset-time: 10
                  delayed-punishment:
                    enabled: false
                    delay-ticks: 20
                commands: []
                config-version: "1.0.1"
                """;
        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml, SyncOptions.create());
        Object commands = result.mergedData().get("commands");
        assertThat(commands).isInstanceOf(List.class);
        assertThat((List<?>) commands).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyStringElementsInsideListSurviveSync() {
        String template = """
                messages:
                  - "first"
                config-version: "1.0.0"
                """;
        String userYaml = """
                messages:
                  - "first"
                  - ""
                  - "third"
                config-version: "1.0.0"
                """;
        SyncResult result = TemplateSync.synchronize(template, userYaml, SyncOptions.create());

        assertThat((List<Object>) result.mergedData().get("messages"))
                .containsExactly("first", "", "third");

        Map<String, Object> reparsed = YamlReader.read(result.mergedYaml());
        assertThat((List<Object>) reparsed.get("messages"))
                .containsExactly("first", "", "third");
    }

    @Test
    void allUserValuesArePreservedByDefault() {
        String userYaml = """
                license-key: "REAL"
                detection:
                  max-warnings: 99
                  reset-time: 60
                  delayed-punishment:
                    enabled: true
                    delay-ticks: 5
                commands:
                  - "ban %player%"
                  - "broadcast caught %player%"
                config-version: "1.0.1"
                """;

        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml, SyncOptions.create());
        assertThat(result.mergedData().get("license-key")).isEqualTo("REAL");
        Map<?, ?> detection = (Map<?, ?>) result.mergedData().get("detection");
        assertThat(detection.get("max-warnings")).isEqualTo(99);
        assertThat(detection.get("reset-time")).isEqualTo(60);
        Map<?, ?> dp = (Map<?, ?>) detection.get("delayed-punishment");
        assertThat(dp.get("enabled")).isEqualTo(true);
        assertThat(dp.get("delay-ticks")).isEqualTo(5);
        @SuppressWarnings("unchecked")
        List<Object> commands = (List<Object>) result.mergedData().get("commands");
        assertThat(commands).containsExactly("ban %player%", "broadcast caught %player%");
    }

    @Test
    void templateCommentsAreReproducedInOutput() {
        String userYaml = """
                license-key: "FILL_HERE"
                detection:
                  max-warnings: 6
                  reset-time: 10
                  delayed-punishment:
                    enabled: false
                    delay-ticks: 20
                commands:
                  - "kick %player%"
                config-version: "1.0.1"
                """;
        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml, SyncOptions.create());

        assertThat(result.mergedYaml()).contains("# Top banner comment");
        assertThat(result.mergedYaml()).contains("# how many strikes before action");
        assertThat(result.mergedYaml()).contains("# Detection block");
        assertThat(result.mergedYaml()).contains("# nested block introduced in 1.0.1");
    }

    @Test
    void typeMismatchFallsBackToTemplate() {
        String userYaml = """
                detection: "should be a map"
                commands: 42
                config-version: "1.0.1"
                """;
        List<String> warnings = new ArrayList<>();
        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml,
                SyncOptions.create().onWarning(warnings::add));

        assertThat(result.mergedData().get("detection")).isInstanceOf(LinkedHashMap.class);
        assertThat(result.mergedData().get("commands")).isInstanceOf(List.class);
        assertThat(warnings).isNotEmpty();
    }

    @Test
    void unquotedSemverIsHandledLikeAString() {
        String template = """
                license-key: "FILL_HERE"
                config-version: 1.0.6
                """;
        String user = """
                license-key: "MY-KEY"
                config-version: 1.0.5
                """;
        SyncResult result = TemplateSync.synchronize(template, user, SyncOptions.create());
        assertThat(result.userVersion().toString()).isEqualTo("1.0.5");
        assertThat(result.bundledVersion().toString()).isEqualTo("1.0.6");
        assertThat(result.isUserBehind()).isTrue();
        assertThat(result.mergedData().get("license-key")).isEqualTo("MY-KEY");
    }

    @Test
    void synchronizeFileWritesMergedYaml(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("config.yml");
        Files.writeString(target, """
                license-key: "MY-KEY"
                detection:
                  max-warnings: 12
                commands:
                  - "ban %player%"
                config-version: "1.0.0"
                """, StandardCharsets.UTF_8);

        SyncResult result = TemplateSync.synchronizeFile(
                new ByteArrayInputStream(TEMPLATE_V101.getBytes(StandardCharsets.UTF_8)),
                target,
                SyncOptions.create());

        assertThat(result.changed()).isTrue();
        String body = Files.readString(target, StandardCharsets.UTF_8);
        assertThat(body).contains("reset-time: 10");
        assertThat(body).contains("license-key: MY-KEY");
        assertThat(body).contains("config-version:");
    }

    @Test
    void runningTwiceOnUpToDateFileIsAStableNoop(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("config.yml");
        TemplateSync.synchronizeFile(
                new ByteArrayInputStream(TEMPLATE_V101.getBytes(StandardCharsets.UTF_8)),
                target,
                SyncOptions.create());
        String first = Files.readString(target, StandardCharsets.UTF_8);

        SyncResult again = TemplateSync.synchronizeFile(
                new ByteArrayInputStream(TEMPLATE_V101.getBytes(StandardCharsets.UTF_8)),
                target,
                SyncOptions.create());

        assertThat(again.changed()).isFalse();
        assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo(first);
    }

    @Test
    void unknownRootKeysStayBeforeVersionWhenVersionKeyLast() {
        String template = """
                discord:
                  enabled: true
                  command: discord
                config-version: "1.0.0"
                """;
        String userYaml = """
                discord:
                  enabled: true
                  command: discord
                website:
                  enabled: true
                  command: website
                config-version: "1.0.0"
                """;
        SyncResult result = TemplateSync.synchronize(template, userYaml,
                SyncOptions.create().freeformRoot(true));

        assertThat(result.mergedData().keySet())
                .containsExactly("discord", "website", "config-version");
        assertThat(result.mergedYaml().indexOf("website"))
                .isLessThan(result.mergedYaml().lastIndexOf("config-version"));
    }

    @Test
    void freeformRootDoesNotKeepUnknownWithoutFlag() {
        String template = """
                discord:
                  enabled: true
                config-version: "1.0.0"
                """;
        String userYaml = """
                discord:
                  enabled: true
                website:
                  enabled: true
                config-version: "1.0.0"
                """;
        List<String> warnings = new ArrayList<>();
        SyncResult result = TemplateSync.synchronize(template, userYaml,
                SyncOptions.create().onWarning(warnings::add));

        assertThat(result.mergedData()).doesNotContainKey("website");
        assertThat(warnings).anyMatch(w -> w.contains("website"));
    }

    @Test
    void dropUnknownKeysToggleKeepsExtraneousValues() {
        String userYaml = """
                license-key: "MY"
                detection:
                  max-warnings: 6
                  reset-time: 10
                  delayed-punishment:
                    enabled: false
                    delay-ticks: 20
                  custom-field: "kept"
                commands:
                  - "kick %player%"
                config-version: "1.0.1"
                """;
        SyncResult result = TemplateSync.synchronize(TEMPLATE_V101, userYaml,
                SyncOptions.create().dropUnknownKeys(false));

        Map<?, ?> detection = (Map<?, ?>) result.mergedData().get("detection");
        assertThat(detection.get("custom-field")).isEqualTo("kept");
    }

    private static final String PROFILES_TEMPLATE = """
            remove-violations-after: 600
            profiles:
              bundled-profile:
                checks: ["BundledCheck"]
                commands:
                  "1:1": "[ALERT]"
                  "3:3": "[WEBHOOK]"
            config-version: "1.0.0"
            """;

    @Test
    @SuppressWarnings("unchecked")
    void freeformPathKeepsOperatorAddedKeysAtAnyDepth() {
        String userYaml = """
                remove-violations-after: 900
                profiles:
                  bundled-profile:
                    checks: ["BundledCheck"]
                    commands:
                      "1:1": "[ALERT]"
                      "5:5": "[WEBHOOK]"
                      "9:9": "kick %player%"
                  my-custom-profile:
                    checks: ["OtherCheck"]
                    commands:
                      "2:2": "broadcast custom"
                wrong-typo: "should still warn"
                config-version: "1.0.0"
                """;
        List<String> warnings = new ArrayList<>();
        SyncResult result = TemplateSync.synchronize(PROFILES_TEMPLATE, userYaml,
                SyncOptions.create()
                        .freeform("profiles")
                        .onWarning(warnings::add));

        Map<String, Object> profiles = (Map<String, Object>) result.mergedData().get("profiles");
        assertThat(profiles).containsKeys("bundled-profile", "my-custom-profile");

        Map<String, Object> bundled = (Map<String, Object>) profiles.get("bundled-profile");
        Map<String, Object> bundledCommands = (Map<String, Object>) bundled.get("commands");
        assertThat(bundledCommands)
                .containsEntry("1:1", "[ALERT]")
                .containsEntry("5:5", "[WEBHOOK]")
                .containsEntry("9:9", "kick %player%")
                .doesNotContainKey("3:3");

        Map<String, Object> custom = (Map<String, Object>) profiles.get("my-custom-profile");
        Map<String, Object> customCommands = (Map<String, Object>) custom.get("commands");
        assertThat(customCommands).containsEntry("2:2", "broadcast custom");

        assertThat(warnings).anyMatch(w -> w.contains("wrong-typo"));
        assertThat(warnings).noneMatch(w -> w.contains("profiles."));
    }

    @Test
    @SuppressWarnings("unchecked")
    void mergeDuplicateKeysCollectsValuesIntoList() {
        String template = """
                profiles:
                  example:
                    commands:
                      "1:1": "[ALERT]"
                config-version: "1.0.0"
                """;
        String userYaml = """
                profiles:
                  example:
                    commands:
                      "1:1": "[ALERT]"
                      "1:1": "[WEBHOOK]"
                      "5:5": "kick %player%"
                config-version: "1.0.0"
                """;
        List<String> warnings = new ArrayList<>();
        SyncResult result = TemplateSync.synchronize(template, userYaml,
                SyncOptions.create()
                        .freeform("profiles")
                        .mergeDuplicateKeys()
                        .onWarning(warnings::add));

        Map<String, Object> profiles = (Map<String, Object>) result.mergedData().get("profiles");
        Map<String, Object> example = (Map<String, Object>) profiles.get("example");
        Map<String, Object> commands = (Map<String, Object>) example.get("commands");

        assertThat(commands).containsKey("1:1");
        Object oneOne = commands.get("1:1");
        assertThat(oneOne).isInstanceOf(List.class);
        assertThat((List<Object>) oneOne).containsExactly("[ALERT]", "[WEBHOOK]");
        assertThat(commands).containsEntry("5:5", "kick %player%");
    }

    @Test
    void strictReadStillRejectsDuplicateKeysByDefault() {
        String template = """
                profiles:
                  example:
                    commands:
                      "1:1": "[ALERT]"
                config-version: "1.0.0"
                """;
        String userYaml = """
                profiles:
                  example:
                    commands:
                      "1:1": "[ALERT]"
                      "1:1": "[WEBHOOK]"
                config-version: "1.0.0"
                """;
        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                        () -> TemplateSync.synchronize(template, userYaml, SyncOptions.create().freeform("profiles")))
                        .getMessage())
                .containsIgnoringCase("duplicate");
    }

    @Test
    @SuppressWarnings("unchecked")
    void freeformPathsAcceptsCollectionAndExactPathMatch() {
        String userYaml = """
                remove-violations-after: 600
                profiles:
                  bundled-profile:
                    checks: ["BundledCheck"]
                    commands:
                      "1:1": "[ALERT]"
                      "3:3": "[WEBHOOK]"
                      "7:7": "kick %player%"
                config-version: "1.0.0"
                """;
        List<String> warnings = new ArrayList<>();
        SyncResult result = TemplateSync.synchronize(PROFILES_TEMPLATE, userYaml,
                SyncOptions.create()
                        .freeform(List.of("profiles"))
                        .onWarning(warnings::add));

        Map<String, Object> profiles = (Map<String, Object>) result.mergedData().get("profiles");
        Map<String, Object> bundled = (Map<String, Object>) profiles.get("bundled-profile");
        Map<String, Object> commands = (Map<String, Object>) bundled.get("commands");
        assertThat(commands).containsEntry("7:7", "kick %player%");
        assertThat(warnings).isEmpty();
    }
}
