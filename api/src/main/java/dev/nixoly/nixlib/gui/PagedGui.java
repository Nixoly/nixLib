package dev.nixoly.nixlib.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PagedGui extends Gui {

    private final List<GuiItem> entries = new ArrayList<>();
    private int[] contentSlots;

    private int page;
    private int entryOffset;
    private int totalEntries = -1;
    private int previousSlot = -1;
    private int nextSlot = -1;
    private GuiItem previousButton;
    private GuiItem nextButton;
    private GuiItem previousFiller;
    private GuiItem nextFiller;

    public PagedGui(String title, int rows) {
        super(title, rows);
        int inner = Math.max(0, rows - 2);
        this.contentSlots = new int[inner * 7];
        int i = 0;
        for (int r = 1; r <= inner; r++) {
            for (int c = 1; c < 8; c++) {
                contentSlots[i++] = r * 9 + c;
            }
        }
    }

    public PagedGui contentSlots(int... slots) {
        this.contentSlots = slots.clone();
        return this;
    }

    public PagedGui pagination(int previousSlot, ItemStack previousItem,
                               int nextSlot, ItemStack nextItem) {
        this.previousSlot = previousSlot;
        this.nextSlot = nextSlot;
        this.previousButton = GuiItem.of(previousItem, ctx -> previousPage());
        this.nextButton = GuiItem.of(nextItem, ctx -> nextPage());
        return this;
    }

    public PagedGui paginationFiller(ItemStack previousFiller, ItemStack nextFiller) {
        this.previousFiller = previousFiller == null ? null : GuiItem.of(previousFiller);
        this.nextFiller = nextFiller == null ? null : GuiItem.of(nextFiller);
        return this;
    }

    public PagedGui addEntry(ItemStack item) {
        entries.add(GuiItem.of(item));
        return this;
    }

    public PagedGui addEntry(ItemStack item, Consumer<ClickContext> handler) {
        entries.add(GuiItem.of(item, handler));
        return this;
    }

    public PagedGui entryWindow(int firstEntry, int totalEntries) {
        if (firstEntry < 0 || totalEntries < firstEntry) {
            throw new IllegalArgumentException("entry window must fit inside total entries");
        }
        this.entryOffset = firstEntry;
        this.totalEntries = totalEntries;
        return this;
    }

    public int currentPage() {
        return page;
    }

    public int totalPages() {
        int count = totalEntries >= 0 ? totalEntries : entries.size();
        int perPage = contentSlots.length;
        if (perPage == 0 || count == 0) return 1;
        return (count + perPage - 1) / perPage;
    }

    public PagedGui open(Player player, int page) {
        this.page = Math.max(0, Math.min(page, totalPages() - 1));
        render();
        super.open(player);
        return this;
    }

    public void goTo(int page) {
        int target = Math.max(0, Math.min(page, totalPages() - 1));
        if (target == this.page) return;
        this.page = target;
        render();
    }

    public void nextPage() { goTo(page + 1); }

    public void previousPage() { goTo(page - 1); }

    // Every slot is written exactly once, so a page turn sends one update per changed slot.
    private void render() {
        int perPage = contentSlots.length;
        int start = page * perPage - entryOffset;
        for (int i = 0; i < perPage; i++) {
            int index = start + i;
            place(contentSlots[i], index >= 0 && index < entries.size() ? entries.get(index) : null);
        }
        if (previousSlot >= 0) {
            place(previousSlot, page > 0 ? previousButton : previousFiller);
        }
        if (nextSlot >= 0) {
            place(nextSlot, page < totalPages() - 1 ? nextButton : nextFiller);
        }
    }

    private void place(int slot, GuiItem item) {
        if (item == null) {
            clear(slot);
        } else {
            setItem(slot, item);
        }
    }
}
