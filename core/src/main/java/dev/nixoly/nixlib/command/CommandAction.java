package dev.nixoly.nixlib.command;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CommandAction {

    void run(@NotNull CommandContext context);
}
