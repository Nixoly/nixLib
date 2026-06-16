package dev.nixoly.nixlib.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandRegistrarTest {

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

    private CommandSpec spec() {
        return CommandSpec.builder("demo")
                .aliases(List.of("d", "demoz"))
                .onRoot(ctx -> {})
                .build();
    }

    @Test
    void registersUnderLabelAndAliases() {
        CommandRegistrar.register(plugin, spec());

        assertThat(server.getCommandMap().getCommand("demo")).isInstanceOf(NixCommand.class);
        assertThat(server.getCommandMap().getCommand("d")).isInstanceOf(NixCommand.class);
        assertThat(server.getCommandMap().getCommand("demoz")).isInstanceOf(NixCommand.class);
    }

    @Test
    void registersNamespacedFallback() {
        CommandRegistrar.register(plugin, spec());

        assertThat(server.getCommandMap().getCommand("nixlib:demo")).isInstanceOf(NixCommand.class);
    }

    @Test
    void unregisterRemovesEverything() {
        CommandSpec spec = spec();
        CommandRegistrar.register(plugin, spec);

        CommandRegistrar.unregister(plugin, spec);

        assertThat(server.getCommandMap().getCommand("demo")).isNull();
        assertThat(server.getCommandMap().getCommand("d")).isNull();
    }

    @Test
    void reRegisterStaysResolvable() {
        CommandRegistrar.register(plugin, spec());
        CommandRegistrar.register(plugin, spec());

        assertThat(server.getCommandMap().getCommand("demo")).isInstanceOf(NixCommand.class);
    }
}
