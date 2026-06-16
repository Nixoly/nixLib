package dev.nixoly.nixlib.items;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigItemTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void hideTooltipKeyIsHandledGracefully() {
        ItemStack item = ConfigItem.build(Map.of(
                "material", "BLACK_STAINED_GLASS_PANE",
                "name", " ",
                "hide-tooltip", true,
                "item-flags", List.of("HIDE_ATTRIBUTES")
        ));

        assertThat(item.getType()).isEqualTo(Material.BLACK_STAINED_GLASS_PANE);
        assertThat(item.getItemMeta().getItemFlags()).contains(ItemFlag.HIDE_ATTRIBUTES);
    }

    @Test
    void aliasKeysAreAccepted() {
        ItemStack item = ConfigItem.build(Map.of(
                "material", "PAPER",
                "hidetooltip", "true",
                "itemflags", List.of("HIDE_ATTRIBUTES")
        ));

        assertThat(item.getType()).isEqualTo(Material.PAPER);
        assertThat(item.getItemMeta().getItemFlags()).contains(ItemFlag.HIDE_ATTRIBUTES);
    }
}
