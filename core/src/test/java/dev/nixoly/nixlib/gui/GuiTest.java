package dev.nixoly.nixlib.gui;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuiTest {

    private ServerMock server;
    private MockPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("nixlib");
        GuiManager.register(plugin);
    }

    @AfterEach
    void tearDown() {
        GuiManager.unregister();
        MockBukkit.unmock();
    }

    @Test
    void buildsInventoryWithExpectedSize() {
        Gui gui = new Gui("Test", 3);
        assertThat(gui.size()).isEqualTo(27);
        assertThat(gui.rows()).isEqualTo(3);
        assertThat(gui.inventory().getHolder()).isSameAs(gui);
    }

    @Test
    void rejectsInvalidRowCounts() {
        assertThatThrownBy(() -> new Gui("x", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Gui("x", 7)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clickHandlerFires() {
        AtomicInteger hits = new AtomicInteger();
        Gui gui = new Gui("Click", 1);
        gui.setItem(0, new ItemStack(Material.DIAMOND), ctx -> hits.incrementAndGet());

        PlayerMock player = server.addPlayer();
        gui.open(player);

        InventoryClickEvent evt = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL
        );
        server.getPluginManager().callEvent(evt);

        assertThat(hits).hasValue(1);
        assertThat(evt.isCancelled()).isTrue();
    }

    @Test
    void borderFillsEdgesOnly() {
        Gui gui = new Gui("Border", 3);
        gui.border(new ItemStack(Material.BLACK_STAINED_GLASS_PANE));

        assertThat(gui.inventory().getItem(0)).isNotNull();
        assertThat(gui.inventory().getItem(8)).isNotNull();
        assertThat(gui.inventory().getItem(9)).isNotNull();
        assertThat(gui.inventory().getItem(17)).isNotNull();
        assertThat(gui.inventory().getItem(18)).isNotNull();
        assertThat(gui.inventory().getItem(26)).isNotNull();
        assertThat(gui.inventory().getItem(10)).isNull();
        assertThat(gui.inventory().getItem(13)).isNull();
        assertThat(gui.inventory().getItem(16)).isNull();
    }

    @Test
    void pagedGuiAdvancesPages() {
        PagedGui paged = new PagedGui("Paged", 3);
        paged.pagination(18, new ItemStack(Material.ARROW), 26, new ItemStack(Material.ARROW));
        for (int i = 0; i < 25; i++) paged.addEntry(new ItemStack(Material.PAPER));

        PlayerMock player = server.addPlayer();
        paged.open(player, 0);

        int perPage = paged.totalPages();
        assertThat(perPage).isGreaterThanOrEqualTo(2);

        paged.nextPage();
        assertThat(paged.currentPage()).isEqualTo(1);
    }
}
