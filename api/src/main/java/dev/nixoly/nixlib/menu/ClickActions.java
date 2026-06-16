package dev.nixoly.nixlib.menu;

import dev.nixoly.nixlib.gui.ClickContext;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class ClickActions {

    private final List<String> all;
    private final List<String> left;
    private final List<String> right;
    private final List<String> shiftLeft;
    private final List<String> shiftRight;
    private final List<String> middle;

    private ClickActions(List<String> all, List<String> left, List<String> right,
                         List<String> shiftLeft, List<String> shiftRight, List<String> middle) {
        this.all = all;
        this.left = left;
        this.right = right;
        this.shiftLeft = shiftLeft;
        this.shiftRight = shiftRight;
        this.middle = middle;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull ClickActions parse(@Nullable Object section) {
        if (!(section instanceof Map<?, ?> raw)) {
            return new ClickActions(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
        Map<String, Object> map = (Map<String, Object>) raw;
        return new ClickActions(
                lines(map, "all-click", "all_click", "all", "click"),
                lines(map, "left-click", "left_click", "left"),
                lines(map, "right-click", "right_click", "right"),
                lines(map, "shift-left-click", "shift_left_click", "shift-left", "shiftleft"),
                lines(map, "shift-right-click", "shift_right_click", "shift-right", "shiftright"),
                lines(map, "middle-click", "middle_click", "middle")
        );
    }

    public boolean isEmpty() {
        return all.isEmpty() && left.isEmpty() && right.isEmpty()
                && shiftLeft.isEmpty() && shiftRight.isEmpty() && middle.isEmpty();
    }

    public @NotNull Consumer<ClickContext> toHandler(@NotNull ActionRegistry registry,
                                                     @Nullable UnaryOperator<String> placeholders) {
        return click -> dispatch(registry, ActionContext.of(click, placeholders));
    }

    public void dispatch(@NotNull ActionRegistry registry, @NotNull ActionContext context) {
        registry.runAll(all, context);
        ClickType type = context.click() == null ? null : context.click().clickType();
        if (type == null) {
            return;
        }
        switch (type) {
            case LEFT -> registry.runAll(left, context);
            case RIGHT -> registry.runAll(right, context);
            case SHIFT_LEFT -> registry.runAll(shiftLeft, context);
            case SHIFT_RIGHT -> registry.runAll(shiftRight, context);
            case MIDDLE -> registry.runAll(middle, context);
            default -> {
            }
        }
    }

    private static List<String> lines(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return asList(map.get(key));
            }
            String lower = key.toLowerCase(Locale.ROOT);
            if (!lower.equals(key) && map.containsKey(lower)) {
                return asList(map.get(lower));
            }
        }
        return List.of();
    }

    private static List<String> asList(Object value) {
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
            return out;
        }
        if (value instanceof String single) {
            return List.of(single);
        }
        return List.of();
    }
}
