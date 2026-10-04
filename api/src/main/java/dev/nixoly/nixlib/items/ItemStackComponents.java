package dev.nixoly.nixlib.items;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Set;

// Paper's data component API (1.21.5+) through reflection, since nixLib compiles against older Paper.
// Every member is resolved once; building menu items calls hideTooltip for each filler pane.
final class ItemStackComponents {

    private static final Object TOOLTIP_DISPLAY_TYPE;
    private static final Method SET_DATA;
    private static final Method GET_DATA;
    private static final Method NEW_BUILDER;
    private static final Method BUILDER_HIDE_TOOLTIP;
    private static final Method BUILDER_SET_HIDDEN;
    private static final Method BUILDER_BUILD;
    private static final Method HIDDEN_COMPONENTS;

    static {
        Object tooltipType = null;
        Method setData = null;
        Method getData = null;
        Method newBuilder = null;
        Method hideTooltip = null;
        Method setHidden = null;
        Method build = null;
        Method hiddenComponents = null;
        try {
            tooltipType = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes").getField("TOOLTIP_DISPLAY").get(null);
            Class<?> displayClass = Class.forName("io.papermc.paper.datacomponent.item.TooltipDisplay");
            Class<?> builderClass = Class.forName("io.papermc.paper.datacomponent.item.TooltipDisplay$Builder");
            Class<?> valuedClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            setData = ItemStack.class.getMethod("setData", valuedClass, Object.class);
            getData = ItemStack.class.getMethod("getData", valuedClass);
            newBuilder = displayClass.getMethod("tooltipDisplay");
            hiddenComponents = displayClass.getMethod("hiddenComponents");
            hideTooltip = builderClass.getMethod("hideTooltip", boolean.class);
            setHidden = builderClass.getMethod("hiddenComponents", Set.class);
            build = builderClass.getMethod("build");
        } catch (ReflectiveOperationException ignored) {
            tooltipType = null;
        }
        TOOLTIP_DISPLAY_TYPE = tooltipType;
        SET_DATA = setData;
        GET_DATA = getData;
        NEW_BUILDER = newBuilder;
        BUILDER_HIDE_TOOLTIP = hideTooltip;
        BUILDER_SET_HIDDEN = setHidden;
        BUILDER_BUILD = build;
        HIDDEN_COMPONENTS = hiddenComponents;
    }

    private ItemStackComponents() {
    }

    static boolean isAvailable() {
        return TOOLTIP_DISPLAY_TYPE != null;
    }

    static void hideTooltip(ItemStack item, boolean hide) {
        if (!isAvailable() || item == null) {
            return;
        }
        try {
            Object builder = NEW_BUILDER.invoke(null);
            BUILDER_HIDE_TOOLTIP.invoke(builder, hide);
            Object existing = GET_DATA.invoke(item, TOOLTIP_DISPLAY_TYPE);
            if (existing != null) {
                Object hidden = HIDDEN_COMPONENTS.invoke(existing);
                if (hidden != null) {
                    BUILDER_SET_HIDDEN.invoke(builder, hidden);
                }
            }
            SET_DATA.invoke(item, TOOLTIP_DISPLAY_TYPE, BUILDER_BUILD.invoke(builder));
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
