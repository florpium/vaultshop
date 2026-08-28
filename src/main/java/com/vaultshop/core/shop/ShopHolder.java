package com.vaultshop.core.shop;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/** Identifies an open VaultShop browsing GUI and remembers what page/filter it's showing. */
public class ShopHolder implements InventoryHolder {

    private Inventory inventory;
    private final List<Material> items;
    private int page;
    private final String searchTerm; // null when not filtered

    public ShopHolder(List<Material> items, int page, String searchTerm) {
        this.items = items;
        this.page = page;
        this.searchTerm = searchTerm;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<Material> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public int getMaxPage(int perPage) {
        if (items.isEmpty()) return 0;
        return (items.size() - 1) / perPage;
    }
}
