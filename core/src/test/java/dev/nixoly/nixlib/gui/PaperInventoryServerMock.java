package dev.nixoly.nixlib.gui;

import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.inventory.ChestInventoryMock;
import be.seeseemelk.mockbukkit.inventory.InventoryMock;
import org.bukkit.inventory.InventoryHolder;

// MockBukkit leaves Paper's Inventory#getHolder(boolean) unimplemented; menus rely on it on real servers.
final class PaperInventoryServerMock extends ServerMock {

    @Override
    public InventoryMock createInventory(InventoryHolder owner, int size, String title) {
        return new ChestInventoryMock(owner, size) {
            @Override
            public InventoryHolder getHolder(boolean useSnapshot) {
                return getHolder();
            }
        };
    }
}
