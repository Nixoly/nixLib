package dev.nixoly.nixlib.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CommandContext {

    private final CommandSender sender;
    private final String label;
    private final String[] args;

    public CommandContext(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        this.sender = sender;
        this.label = label;
        this.args = args;
    }

    public @NotNull CommandSender sender() {
        return sender;
    }

    public @NotNull String label() {
        return label;
    }

    public @NotNull String[] args() {
        return args;
    }

    public int size() {
        return args.length;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public @Nullable Player player() {
        return sender instanceof Player player ? player : null;
    }

    public @Nullable String arg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    public @NotNull String argOr(int index, @NotNull String fallback) {
        String value = arg(index);
        return value == null ? fallback : value;
    }
}
