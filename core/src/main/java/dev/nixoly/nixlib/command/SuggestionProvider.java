package dev.nixoly.nixlib.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface SuggestionProvider {

    @NotNull List<String> suggest(@NotNull CommandContext context);
}
