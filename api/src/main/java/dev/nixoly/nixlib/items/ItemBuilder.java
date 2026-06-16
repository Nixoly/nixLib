package dev.nixoly.nixlib.items;

import dev.nixoly.nixlib.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class ItemBuilder {

    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(new ItemStack(material, amount));
    }

    public static ItemBuilder of(ItemStack source) {
        return new ItemBuilder(source.clone());
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(amount, item.getMaxStackSize())));
        return this;
    }

    public ItemBuilder name(String legacy) {
        return mutate(meta -> meta.displayName(ColorUtils.parse(legacy)));
    }

    public ItemBuilder name(Component component) {
        return mutate(meta -> meta.displayName(component));
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        return mutate(meta -> meta.lore(ColorUtils.parseAll(lines)));
    }

    public ItemBuilder loreComponents(List<Component> components) {
        return mutate(meta -> meta.lore(components));
    }

    public ItemBuilder appendLore(String... lines) {
        return mutate(meta -> {
            List<Component> existing = meta.lore();
            List<Component> merged = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            for (String line : lines) merged.add(ColorUtils.parse(line));
            meta.lore(merged);
        });
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        return mutate(meta -> meta.addEnchant(enchantment, level, true));
    }

    public ItemBuilder unsafeEnchant(Enchantment enchantment, int level) {
        item.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        return mutate(meta -> meta.addItemFlags(flags));
    }

    public ItemBuilder hideAllFlags() {
        return flags(ItemFlag.values());
    }

    public ItemBuilder unbreakable() {
        return mutate(meta -> meta.setUnbreakable(true));
    }

    public ItemBuilder damage(int damage) {
        return mutate(meta -> {
            if (meta instanceof Damageable d) d.setDamage(damage);
        });
    }

    public ItemBuilder customModelData(Integer data) {
        return mutate(meta -> meta.setCustomModelData(data));
    }

    public ItemBuilder glow() {
        return mutate(meta -> {
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
    }

    public ItemBuilder glow(boolean glow) {
        return glow ? glow() : this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        return mutate(meta -> meta.setUnbreakable(unbreakable));
    }

    public ItemBuilder itemModel(String namespacedKey) {
        return mutate(meta -> ItemMetaReflect.itemModel(meta, namespacedKey));
    }

    public ItemBuilder tooltipStyle(String namespacedKey) {
        return mutate(meta -> ItemMetaReflect.tooltipStyle(meta, namespacedKey));
    }

    public ItemBuilder hideTooltip(boolean hide) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return this;
        }
        boolean applied = ItemMetaReflect.hideTooltip(meta, hide);
        item.setItemMeta(meta);
        if (!applied) {
            ItemStackComponents.hideTooltip(item, hide);
        }
        return this;
    }

    public ItemBuilder color(org.bukkit.Color color) {
        return mutate(meta -> {
            if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leather) {
                leather.setColor(color);
            } else if (meta instanceof org.bukkit.inventory.meta.PotionMeta potion) {
                potion.setColor(color);
            }
        });
    }

    public ItemBuilder attribute(Attribute attribute, AttributeModifier modifier) {
        return mutate(meta -> meta.addAttributeModifier(attribute, modifier));
    }

    public ItemBuilder pdcString(Plugin plugin, String key, String value) {
        return mutate(meta -> meta.getPersistentDataContainer()
                .set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value));
    }

    public ItemBuilder pdcInt(Plugin plugin, String key, int value) {
        return mutate(meta -> meta.getPersistentDataContainer()
                .set(new NamespacedKey(plugin, key), PersistentDataType.INTEGER, value));
    }

    public ItemBuilder modify(Consumer<ItemMeta> editor) {
        return mutate(editor);
    }

    public ItemStack build() {
        return item;
    }

    public ItemStack cloneStack() {
        return item.clone();
    }

    private ItemBuilder mutate(Consumer<ItemMeta> op) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            op.accept(meta);
            item.setItemMeta(meta);
        }
        return this;
    }
}
