package dev.nixoly.nixlib.gui;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class PagedGui extends Gui {

    private final List<GuiItem> entries = new ArrayList<>();
    private final List<Integer> contentSlots = new ArrayList<>();

    private int page;
    private int entryOffset;
    private int totalEntries = -1;
    private int previousSlot = -1;
    private int nextSlot = -1;
    private ItemStack previousItem;
    private ItemStack nextItem;
    private ItemStack previousFiller;
    private ItemStack nextFiller;

    public PagedGui(String title, int rows) {
        super(title, rows);
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < 8; c++) {
                contentSlots.add(r * 9 + c);
            }
        }
    }

    public PagedGui contentSlots(int... slots) {
        contentSlots.clear();
        Arrays.stream(slots).forEach(contentSlots::add);
        return this;
    }

    public PagedGui pagination(int previousSlot, ItemStack previousItem,
                               int nextSlot, ItemStack nextItem) {
        this.previousSlot = previousSlot;
        this.nextSlot = nextSlot;
        this.previousItem = previousItem;
        this.nextItem = nextItem;
        return this;
    }

    public PagedGui paginationFiller(ItemStack previousFiller, ItemStack nextFiller) {
        this.previousFiller = previousFiller;
        this.nextFiller = nextFiller;
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
        if (contentSlots.isEmpty() || count == 0) return 1;
        return (int) Math.ceil(count / (double) contentSlots.size());
    }

    public PagedGui open(org.bukkit.entity.Player player, int page) {
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

    private void render() {
        for (int slot : contentSlots) clear(slot);

        int perPage = contentSlots.size();
        int start = page * perPage - entryOffset;
        if (start >= 0 && start < entries.size()) {
            int end = Math.min(start + perPage, entries.size());
            for (int i = start; i < end; i++) {
                int slot = contentSlots.get(i - start);
                setItem(slot, entries.get(i));
            }
        }

        if (previousSlot >= 0) {
            if (page > 0) {
                setItem(previousSlot, previousItem, ctx -> previousPage());
            } else if (previousFiller != null) {
                setItem(previousSlot, previousFiller.clone());
            } else {
                clear(previousSlot);
            }
        }
        if (nextSlot >= 0) {
            if (page < totalPages() - 1) {
                setItem(nextSlot, nextItem, ctx -> nextPage());
            } else if (nextFiller != null) {
                setItem(nextSlot, nextFiller.clone());
            } else {
                clear(nextSlot);
            }
        }
    }
}
