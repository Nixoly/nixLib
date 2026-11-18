package dev.nixoly.nixlib.items;

import be.seeseemelk.mockbukkit.MockBukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

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

    @Test
    void reopenedSectionGetsItsOwnStack() {
        Map<String, Object> section = Map.of("material", "ENDER_PEARL", "name", "Teleport", "lore", List.of("to %player%"));
        UnaryOperator<String> text = line -> line.replace("%player%", "Nixoly");

        ItemStack first = ConfigItem.build(section, text);
        first.setAmount(5);
        ItemStack second = ConfigItem.build(section, text);

        assertThat(second).isNotSameAs(first);
        assertThat(second.getAmount()).isEqualTo(1);
        assertThat(plain(second.getItemMeta().lore().get(0))).isEqualTo("to Nixoly");
    }

    @Test
    void changedPlaceholderRendersAgain() {
        Map<String, Object> section = Map.of("material", "PAPER", "name", "Ping %ping%");

        ItemStack low = ConfigItem.build(section, line -> line.replace("%ping%", "20"));
        ItemStack high = ConfigItem.build(section, line -> line.replace("%ping%", "180"));

        assertThat(plain(low.getItemMeta().displayName())).isEqualTo("Ping 20");
        assertThat(plain(high.getItemMeta().displayName())).isEqualTo("Ping 180");
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
