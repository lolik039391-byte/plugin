package ru.skup.plugin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BuyerMenuHolder implements InventoryHolder {

    private final int page;
    private final int totalPages;

    public BuyerMenuHolder(int page, int totalPages) {
        this.page = page;
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
