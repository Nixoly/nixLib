package dev.nixoly.nixlib.menu;

import dev.nixoly.nixlib.gui.ClickContext;
import dev.nixoly.nixlib.gui.Gui;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public final class ActionContext {

    private final Player player;
    private final ClickContext click;
    private final UnaryOperator<String> placeholders;

    public ActionContext(@NotNull Player player, @Nullable ClickContext click,
                         @Nullable UnaryOperator<String> placeholders) {
        this.player = player;
        this.click = click;
        this.placeholders = placeholders;
    }

    public static @NotNull ActionContext of(@NotNull Player player) {
        return new ActionContext(player, null, null);
    }

    public static @NotNull ActionContext of(@NotNull Player player, @Nullable UnaryOperator<String> placeholders) {
        return new ActionContext(player, null, placeholders);
    }

    public static @NotNull ActionContext of(@NotNull ClickContext click, @Nullable UnaryOperator<String> placeholders) {
        return new ActionContext(click.player(), click, placeholders);
    }

    public @NotNull Player player() {
        return player;
    }

    public @Nullable ClickContext click() {
        return click;
    }

    public @Nullable Gui gui() {
        return click == null ? null : click.gui();
    }

    public @NotNull String apply(@Nullable String text) {
        if (text == null) {
            return "";
        }
        return placeholders == null ? text : placeholders.apply(text);
    }
}
