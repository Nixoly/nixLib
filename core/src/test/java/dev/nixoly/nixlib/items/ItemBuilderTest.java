package dev.nixoly.nixlib.items;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemBuilderTest {

    private MockPlugin plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("nixlib");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nameAndAmountAreApplied() {
        ItemStack item = ItemBuilder.of(Material.DIAMOND_SWORD, 1)
                .name("&aLegendary")
                .build();

        assertThat(item.getType()).isEqualTo(Material.DIAMOND_SWORD);
        assertThat(item.getAmount()).isEqualTo(1);
        assertThat(item.getItemMeta().getDisplayName()).isEqualTo(ChatColor.GREEN + "Legendary");
    }

    @Test
    void loreAndEnchantStack() {
        ItemStack item = ItemBuilder.of(Material.STICK)
                .lore("&7Line one", "&7Line two")
                .enchant(Enchantment.DAMAGE_ALL, 5)
                .flags(ItemFlag.HIDE_ENCHANTS)
                .build();

        assertThat(item.getItemMeta().getLore()).hasSize(2);
        assertThat(item.getItemMeta().getEnchantLevel(Enchantment.DAMAGE_ALL)).isEqualTo(5);
        assertThat(item.getItemMeta().getItemFlags()).contains(ItemFlag.HIDE_ENCHANTS);
    }

    @Test
    void pdcValuesPersist() {
        ItemStack item = ItemBuilder.of(Material.PAPER)
                .pdcString(plugin, "id", "spell-fireball")
                .pdcInt(plugin, "cooldown", 30)
                .build();

        assertThat(NbtUtils.getString(plugin, item, "id")).contains("spell-fireball");
        assertThat(NbtUtils.getInt(plugin, item, "cooldown")).contains(30);
    }

    @Test
    void glowAddsHiddenEnchant() {
        ItemStack item = ItemBuilder.of(Material.APPLE).glow().build();

        assertThat(item.getItemMeta().hasEnchants()).isTrue();
        assertThat(item.getItemMeta().getItemFlags()).contains(ItemFlag.HIDE_ENCHANTS);
    }

    @Test
    void cloneStackIsIndependent() {
        ItemBuilder builder = ItemBuilder.of(Material.STONE).amount(5);
        ItemStack a = builder.build();
        ItemStack b = builder.cloneStack();
        b.setAmount(1);
        assertThat(a.getAmount()).isEqualTo(5);
        assertThat(b.getAmount()).isEqualTo(1);
    }
}
