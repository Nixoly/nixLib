package dev.nixoly.nixlib.items;

import dev.nixoly.nixlib.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class ConfigItem {

    private ConfigItem() {
    }

    public static ItemStack build(Map<String, Object> section) {
        return build(section, UnaryOperator.identity());
    }

    @SuppressWarnings("unchecked")
    public static ItemStack build(Map<String, Object> section, UnaryOperator<String> text) {
        if (section == null) {
            return new ItemStack(Material.STONE);
        }

        Material material = resolveMaterial(asString(section.get("material")));
        ItemStack item = createBase(section, material, text);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String name = asString(section.get("name"));
        if (name != null) {
            meta.displayName(translate(text.apply(name)));
        }

        List<String> lore = asStringList(section.get("lore"));
        if (lore != null) {
            List<Component> rendered = new ArrayList<>(lore.size());
            for (String line : lore) {
                rendered.add(translate(text.apply(line)));
            }
            meta.lore(rendered);
        }

        Integer amount = asInt(section.get("amount"));
        if (amount != null) {
            item.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        }

        if (asBool(section.get("unbreakable"), false)) {
            meta.setUnbreakable(true);
        }

        if (asBool(section.get("glow"), false)) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        applyEnchants(meta, asStringList(section.get("enchants")));
        applyItemFlags(meta, asStringList(firstPresent(section, "item-flags", "itemflags", "flags")));
        applyCustomModelData(meta, section.get("custom-model-data"));
        applyColor(meta, asString(section.get("color")));

        Integer damage = asInt(section.get("damage"));
        if (damage != null && meta instanceof Damageable damageable) {
            damageable.setDamage(damage);
        }

        String itemModel = asString(section.get("item-model"));
        if (itemModel != null) {
            ItemMetaReflect.itemModel(meta, itemModel);
        }

        String tooltipStyle = asString(section.get("tooltip-style"));
        if (tooltipStyle != null) {
            ItemMetaReflect.tooltipStyle(meta, tooltipStyle);
        }

        Object hideTooltip = firstValue(section, "hide-tooltip", "hidetooltip", "hide_tooltip");
        Boolean hideTooltipFallback = null;
        if (hideTooltip != null) {
            boolean hide = asBool(hideTooltip, false);
            if (!ItemMetaReflect.hideTooltip(meta, hide)) {
                hideTooltipFallback = hide;
            }
        }

        item.setItemMeta(meta);

        if (hideTooltipFallback != null) {
            ItemStackComponents.hideTooltip(item, hideTooltipFallback);
        }

        String snbt = asString(section.get("snbt"));
        if (snbt != null && !snbt.isBlank()) {
            item = applySnbt(item, snbt);
        }
        return item;
    }

    private static ItemStack createBase(Map<String, Object> section, Material material, UnaryOperator<String> text) {
        String texture = asString(section.get("texture"));
        if (texture != null && !texture.isBlank()) {
            return SkullBuilder.head().texture(texture.trim()).build();
        }
        String owner = asString(section.get("skull"));
        if (owner == null) {
            owner = asString(section.get("head"));
        }
        if (owner != null && !owner.isBlank()) {
            return SkullBuilder.head().ownerName(text.apply(owner)).build();
        }
        String hdb = asString(section.get("hdb"));
        if (hdb != null && !hdb.isBlank()) {
            ItemStack head = HeadDatabaseSupport.head(hdb);
            if (head != null) {
                return head;
            }
        }
        return new ItemStack(material);
    }

    private static Material resolveMaterial(String raw) {
        if (raw == null || raw.isBlank()) {
            return Material.STONE;
        }
        Material match = Material.matchMaterial(raw.trim());
        return match == null ? Material.STONE : match;
    }

    private static void applyEnchants(ItemMeta meta, List<String> enchants) {
        if (enchants == null) {
            return;
        }
        for (String entry : enchants) {
            int idx = entry.lastIndexOf(':');
            String name = idx > 0 ? entry.substring(0, idx) : entry;
            int level = 1;
            if (idx > 0) {
                Integer parsed = asInt(entry.substring(idx + 1).trim());
                if (parsed != null) {
                    level = parsed;
                }
            }
            Enchantment enchantment = resolveEnchant(name.trim());
            if (enchantment != null) {
                meta.addEnchant(enchantment, Math.max(1, level), true);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static Enchantment resolveEnchant(String name) {
        try {
            Enchantment byKey = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
            if (byKey != null) {
                return byKey;
            }
        } catch (Throwable ignored) {
        }
        try {
            return Enchantment.getByName(name.toUpperCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void applyItemFlags(ItemMeta meta, List<String> flags) {
        if (flags == null) {
            return;
        }
        for (String flag : flags) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flag.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyCustomModelData(ItemMeta meta, Object raw) {
        if (raw == null) {
            return;
        }
        if (raw instanceof Map<?, ?> map) {
            Object floats = ((Map<String, Object>) map).get("floats");
            List<String> floatList = asStringList(floats);
            if (floatList != null && !floatList.isEmpty()) {
                Integer first = asInt(floatList.get(0));
                if (first != null) {
                    meta.setCustomModelData(first);
                }
            }
            return;
        }
        Integer value = asInt(raw);
        if (value != null) {
            meta.setCustomModelData(value);
        }
    }

    private static void applyColor(ItemMeta meta, String raw) {
        Color color = parseColor(raw);
        if (color == null) {
            return;
        }
        if (meta instanceof LeatherArmorMeta leather) {
            leather.setColor(color);
        } else if (meta instanceof PotionMeta potion) {
            potion.setColor(color);
        }
    }

    private static Color parseColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.startsWith("#") && value.length() == 7) {
            try {
                return Color.fromRGB(Integer.parseInt(value.substring(1), 16));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        String[] parts = value.split(",");
        if (parts.length != 3) {
            return null;
        }
        Integer r = asInt(parts[0].trim());
        Integer g = asInt(parts[1].trim());
        Integer b = asInt(parts[2].trim());
        if (r == null || g == null || b == null) {
            return null;
        }
        return Color.fromRGB(clamp(r), clamp(g), clamp(b));
    }

    private static ItemStack applySnbt(ItemStack item, String snbt) {
        try {
            Object unsafe = Bukkit.class.getMethod("getUnsafe").invoke(null);
            unsafe.getClass().getMethod("modifyItemStack", ItemStack.class, String.class)
                    .invoke(unsafe, item, snbt);
        } catch (Throwable ignored) {
        }
        return item;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static Component translate(String input) {
        Component component = ColorUtils.translate(input);
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Object firstValue(Map<String, Object> section, String... keys) {
        for (String key : keys) {
            if (section.containsKey(key)) {
                return section.get(key);
            }
        }
        return null;
    }

    private static Object firstPresent(Map<String, Object> section, String... keys) {
        for (String key : keys) {
            Object value = section.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean asBool(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value.toString().trim());
    }

    private static List<String> asStringList(Object value) {
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
            List<String> out = new ArrayList<>(1);
            out.add(single);
            return out;
        }
        return null;
    }
}
