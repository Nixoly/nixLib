package dev.nixoly.nixlib.gui;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuiTest {

    private ServerMock server;
    private MockPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new PaperInventoryServerMock());
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
    void escClosingRunsBackHandler() {
        AtomicInteger back = new AtomicInteger();
        Gui gui = new Gui("Back", 1);
        gui.onBack(p -> back.incrementAndGet());

        PlayerMock player = server.addPlayer();
        gui.open(player);
        server.getPluginManager().callEvent(new InventoryCloseEvent(player.getOpenInventory()));
        server.getScheduler().performTicks(2);

        assertThat(back).hasValue(1);
    }

    @Test
    void transitionCloseDoesNotRunBackHandler() {
        AtomicInteger back = new AtomicInteger();
        Gui gui = new Gui("Back", 1);
        gui.onBack(p -> back.incrementAndGet());

        PlayerMock player = server.addPlayer();
        gui.open(player);
        GuiNavigation.beginTransition(player.getUniqueId());
        server.getPluginManager().callEvent(new InventoryCloseEvent(player.getOpenInventory()));
        GuiNavigation.endTransition(player.getUniqueId());
        server.getScheduler().performTicks(2);

        assertThat(back).hasValue(0);
    }

    @Test
    void suppressedCloseDoesNotRunBackHandler() {
        AtomicInteger back = new AtomicInteger();
        Gui gui = new Gui("Back", 1);
        gui.onBack(p -> back.incrementAndGet());

        PlayerMock player = server.addPlayer();
        gui.open(player);
        GuiNavigation.suppressBack(player.getUniqueId());
        server.getPluginManager().callEvent(new InventoryCloseEvent(player.getOpenInventory()));
        server.getScheduler().performTicks(2);

        assertThat(back).hasValue(0);
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

    @Test
    void entryWindowRendersOnlySuppliedPage() {
        PagedGui paged = new PagedGui("Window", 1);
        paged.contentSlots(0, 1);
        paged.entryWindow(2, 5);
        paged.addEntry(new ItemStack(Material.DIAMOND));
        paged.addEntry(new ItemStack(Material.EMERALD));

        PlayerMock player = server.addPlayer();
        paged.open(player, 1);

        assertThat(paged.totalPages()).isEqualTo(3);
        assertThat(paged.currentPage()).isEqualTo(1);
        assertThat(paged.inventory().getItem(0).getType()).isEqualTo(Material.DIAMOND);
        assertThat(paged.inventory().getItem(1).getType()).isEqualTo(Material.EMERALD);
    }

    @Test
    void spamClicksRunTheHandlerOnce() {
        AtomicInteger hits = new AtomicInteger();
        Gui gui = new Gui("Spam", 1);
        gui.setItem(0, new ItemStack(Material.DIAMOND), ctx -> hits.incrementAndGet());
        PlayerMock player = server.addPlayer();
        gui.open(player);

        InventoryClickEvent first = click(player, 0, ClickType.LEFT);
        InventoryClickEvent second = click(player, 0, ClickType.LEFT);

        assertThat(hits).hasValue(1);
        assertThat(first.isCancelled()).isTrue();
        assertThat(second.isCancelled()).isTrue();
    }

    @Test
    void zeroCooldownHandlesEveryClick() {
        AtomicInteger hits = new AtomicInteger();
        Gui gui = new Gui("Fast", 1).clickCooldown(Duration.ZERO);
        gui.setItem(0, new ItemStack(Material.DIAMOND), ctx -> hits.incrementAndGet());
        PlayerMock player = server.addPlayer();
        gui.open(player);

        click(player, 0, ClickType.LEFT);
        click(player, 0, ClickType.RIGHT);

        assertThat(hits).hasValue(2);
    }

    @Test
    void doubleClickDoesNotRepeatTheHandler() {
        AtomicInteger hits = new AtomicInteger();
        Gui gui = new Gui("Double", 1).clickCooldown(Duration.ZERO);
        gui.setItem(0, new ItemStack(Material.DIAMOND), ctx -> hits.incrementAndGet());
        PlayerMock player = server.addPlayer();
        gui.open(player);

        click(player, 0, ClickType.LEFT);
        InventoryClickEvent doubleClick = click(player, 0, ClickType.DOUBLE_CLICK);

        assertThat(hits).hasValue(1);
        assertThat(doubleClick.isCancelled()).isTrue();
    }

    @Test
    void menuOpenedFromAClickWaitsForTheNextTick() {
        Gui second = new Gui("Second", 1);
        Gui first = new Gui("First", 1);
        first.setItem(0, new ItemStack(Material.ARROW), ctx -> second.open(ctx.player()));
        PlayerMock player = server.addPlayer();
        first.open(player);

        click(player, 0, ClickType.LEFT);
        assertThat(player.getOpenInventory().getTopInventory()).isSameAs(first.inventory());

        server.getScheduler().performOneTick();
        assertThat(player.getOpenInventory().getTopInventory()).isSameAs(second.inventory());
    }

    @Test
    void lastMenuRequestedByAClickWins() {
        Gui skipped = new Gui("Skipped", 1);
        Gui target = new Gui("Target", 1);
        Gui start = new Gui("Start", 1);
        start.setItem(0, new ItemStack(Material.ARROW), ctx -> {
            ctx.close();
            skipped.open(ctx.player());
            target.open(ctx.player());
        });
        PlayerMock player = server.addPlayer();
        start.open(player);

        click(player, 0, ClickType.LEFT);
        server.getScheduler().performOneTick();

        assertThat(player.getOpenInventory().getTopInventory()).isSameAs(target.inventory());
    }

    @Test
    void closeFromAClickWaitsForTheNextTick() {
        Gui gui = new Gui("Close", 1);
        gui.setItem(0, new ItemStack(Material.BARRIER), ClickContext::close);
        PlayerMock player = server.addPlayer();
        gui.open(player);

        click(player, 0, ClickType.LEFT);
        assertThat(gui.isViewing(player)).isTrue();

        server.getScheduler().performOneTick();
        assertThat(gui.isViewing(player)).isFalse();
    }

    @Test
    void reopeningTheSameMenuKeepsItOpen() {
        AtomicInteger opens = new AtomicInteger();
        Gui gui = new Gui("Same", 1).onOpen(event -> opens.incrementAndGet());
        PlayerMock player = server.addPlayer();

        gui.open(player);
        gui.open(player);

        assertThat(opens).hasValue(1);
    }

    @Test
    void fromResolvesOnlyMenus() {
        Gui gui = new Gui("Lookup", 2);
        PlayerMock player = server.addPlayer();

        assertThat(Gui.from(gui.inventory())).isSameAs(gui);
        assertThat(Gui.from(player.getInventory())).isNull();
        assertThat(Gui.from(server.createInventory(null, 9, "plain"))).isNull();
        assertThat(Gui.from(null)).isNull();
    }

    @Test
    void quitForgetsPendingBackSuppression() {
        PlayerMock player = server.addPlayer();
        GuiNavigation.suppressBack(player.getUniqueId());

        server.getPluginManager().callEvent(new PlayerQuitEvent(player, "bye"));

        assertThat(GuiNavigation.consumeSuppressBack(player.getUniqueId())).isFalse();
    }

    private InventoryClickEvent click(PlayerMock player, int slot, ClickType type) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(), InventoryType.SlotType.CONTAINER, slot, type, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        return event;
    }
}
