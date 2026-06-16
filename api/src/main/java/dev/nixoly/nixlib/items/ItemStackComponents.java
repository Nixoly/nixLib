package dev.nixoly.nixlib.items;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

final class ItemStackComponents {

    private static final boolean AVAILABLE;
    private static final Object TOOLTIP_DISPLAY_TYPE;
    private static final Class<?> TOOLTIP_DISPLAY_CLASS;
    private static final Method SET_DATA;
    private static final Method GET_DATA;

    static {
        boolean available = false;
        Object tooltipType = null;
        Class<?> tooltipClass = null;
        Method setData = null;
        Method getData = null;
        try {
            Class<?> typesClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            tooltipType = typesClass.getField("TOOLTIP_DISPLAY").get(null);
            tooltipClass = Class.forName("io.papermc.paper.datacomponent.item.TooltipDisplay");
            Class<?> valuedClass = Class.forName("io.papermc.paper.datacomponent.DataComponentType$Valued");
            setData = ItemStack.class.getMethod("setData", valuedClass, Object.class);
            getData = ItemStack.class.getMethod("getData", valuedClass);
            available = tooltipType != null;
        } catch (Throwable ignored) {
        }
        AVAILABLE = available;
        TOOLTIP_DISPLAY_TYPE = tooltipType;
        TOOLTIP_DISPLAY_CLASS = tooltipClass;
        SET_DATA = setData;
        GET_DATA = getData;
    }

    private ItemStackComponents() {
    }

    static boolean isAvailable() {
        return AVAILABLE;
    }

    static void hideTooltip(ItemStack item, boolean hide) {
        if (!AVAILABLE || item == null) {
            return;
        }
        try {
            Object builder = TOOLTIP_DISPLAY_CLASS.getMethod("tooltipDisplay").invoke(null);
            Class<?> builderClass = builder.getClass();
            builderClass.getMethod("hideTooltip", boolean.class).invoke(builder, hide);
            preserveHiddenComponents(item, builder, builderClass);
            Object display = builderClass.getMethod("build").invoke(builder);
            SET_DATA.invoke(item, TOOLTIP_DISPLAY_TYPE, display);
        } catch (Throwable ignored) {
        }
    }

    private static void preserveHiddenComponents(ItemStack item, Object builder, Class<?> builderClass) {
        if (GET_DATA == null) {
            return;
        }
        try {
            Object existing = GET_DATA.invoke(item, TOOLTIP_DISPLAY_TYPE);
            if (existing == null) {
                return;
            }
            Object hidden = TOOLTIP_DISPLAY_CLASS.getMethod("hiddenComponents").invoke(existing);
            if (hidden == null) {
                return;
            }
            builderClass.getMethod("addHiddenComponents", java.util.Collection.class).invoke(builder, hidden);
        } catch (Throwable ignored) {
        }
    }
}
