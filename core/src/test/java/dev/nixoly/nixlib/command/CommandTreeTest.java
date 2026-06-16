package dev.nixoly.nixlib.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CommandTreeTest {

    private ServerMock server;
    private MockPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("nixlib");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private CommandSpec spec(AtomicInteger root, AtomicInteger reload, AtomicReference<String> selected) {
        return CommandSpec.builder("demo")
                .aliases(List.of("d"))
                .onRoot(ctx -> root.incrementAndGet())
                .sub(CommandSpec.sub("reload").permission("demo.admin").action(ctx -> reload.incrementAndGet()))
                .sub(CommandSpec.sub("select")
                        .arg(List.of("heart", "star"))
                        .arg(ctx -> List.of("Alice", "Bob"))
                        .action(ctx -> selected.set(ctx.argOr(1, "") + "/" + ctx.argOr(2, ""))))
                .build();
    }

    @Test
    void rootActionRunsWithNoArgs() {
        AtomicInteger root = new AtomicInteger();
        NixCommand cmd = new NixCommand(spec(root, new AtomicInteger(), new AtomicReference<>()));
        PlayerMock player = server.addPlayer();

        cmd.execute(player, "demo", new String[0]);

        assertThat(root).hasValue(1);
    }

    @Test
    void subActionReceivesArguments() {
        AtomicReference<String> selected = new AtomicReference<>();
        NixCommand cmd = new NixCommand(spec(new AtomicInteger(), new AtomicInteger(), selected));
        PlayerMock player = server.addPlayer();

        cmd.execute(player, "demo", new String[]{"select", "heart", "Alice"});

        assertThat(selected.get()).isEqualTo("heart/Alice");
    }

    @Test
    void unknownSubFallsBackToRoot() {
        AtomicInteger root = new AtomicInteger();
        NixCommand cmd = new NixCommand(spec(root, new AtomicInteger(), new AtomicReference<>()));
        PlayerMock player = server.addPlayer();

        cmd.execute(player, "demo", new String[]{"nonsense"});

        assertThat(root).hasValue(1);
    }

    @Test
    void permissionGatedSubDoesNotRunWithoutPermission() {
        AtomicInteger reload = new AtomicInteger();
        NixCommand cmd = new NixCommand(spec(new AtomicInteger(), reload, new AtomicReference<>()));
        PlayerMock player = server.addPlayer();

        cmd.execute(player, "demo", new String[]{"reload"});
        assertThat(reload).hasValue(0);

        player.addAttachment(plugin, "demo.admin", true);
        cmd.execute(player, "demo", new String[]{"reload"});
        assertThat(reload).hasValue(1);
    }

    @Test
    void tabListsSubNamesFilteredByPermission() {
        NixCommand cmd = new NixCommand(spec(new AtomicInteger(), new AtomicInteger(), new AtomicReference<>()));
        PlayerMock player = server.addPlayer();

        assertThat(cmd.tabComplete(player, "demo", new String[]{""})).containsExactly("select");

        player.addAttachment(plugin, "demo.admin", true);
        assertThat(cmd.tabComplete(player, "demo", new String[]{""}))
                .containsExactlyInAnyOrder("reload", "select");
    }

    @Test
    void tabSuggestsPositionalArguments() {
        NixCommand cmd = new NixCommand(spec(new AtomicInteger(), new AtomicInteger(), new AtomicReference<>()));
        PlayerMock player = server.addPlayer();

        assertThat(cmd.tabComplete(player, "demo", new String[]{"select", ""}))
                .containsExactly("heart", "star");
        assertThat(cmd.tabComplete(player, "demo", new String[]{"select", "h"}))
                .containsExactly("heart");
        assertThat(cmd.tabComplete(player, "demo", new String[]{"select", "heart", ""}))
                .containsExactly("Alice", "Bob");
    }

    @Test
    void permissionDeniedHandlerRunsWhenRootBlocked() {
        AtomicInteger denied = new AtomicInteger();
        CommandSpec spec = CommandSpec.builder("locked")
                .permission("locked.use")
                .onRoot(ctx -> {})
                .onPermissionDenied(ctx -> denied.incrementAndGet())
                .build();
        NixCommand cmd = new NixCommand(spec);
        PlayerMock player = server.addPlayer();

        cmd.execute(player, "locked", new String[0]);
        assertThat(denied).hasValue(1);

        player.addAttachment(plugin, "locked.use", true);
        cmd.execute(player, "locked", new String[0]);
        assertThat(denied).hasValue(1);
    }
}
