package com.vaultshop.core.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class SellHolder implements InventoryHolder {
    private Inventory inventory;
    private final UUID owner;
    private boolean processed = false;

    public SellHolder(UUID owner) { this.owner = owner; }

    @Override
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public UUID getOwner() { return owner; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}
