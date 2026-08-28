package com.vaultshop.core.listeners;

import com.vaultshop.core.VaultShop;
import com.vaultshop.core.shop.PriceManager;
import com.vaultshop.core.shop.SellHolder;
import com.vaultshop.core.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SellGUIListener implements Listener {

    private final VaultShop plugin;

    public SellGUIListener(VaultShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        finalizeSell(player, event.getInventory(), holder);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Inventory inv = plugin.getSessionManager().getOpenSellSessions().get(player.getUniqueId());
        if (inv != null && inv.getHolder() instanceof SellHolder holder) {
            finalizeSell(player, inv, holder);
        }
    }

    /** Idempotent: safe to call from close, quit, and plugin-disable without double-selling. */
    public void finalizeSell(Player player, Inventory inv, SellHolder holder) {
        if (holder.isProcessed()) return;
        holder.setProcessed(true);
        plugin.getSessionManager().clearSellSession(holder.getOwner());

        PriceManager pm = plugin.getPriceManager();
        double total = 0.0;
        int soldCount = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;

            if (pm.isSellable(item.getType())) {
                total += pm.getSellPrice(item.getType()) * item.getAmount();
                soldCount += item.getAmount();
            } else {
                // Never destroy items we don't know how to price/sell - hand them straight back.
                giveOrDrop(player, item);
            }
        }
        inv.clear();

        if (soldCount == 0) {
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.sold-all-none", "&cYou didn't sell anything - the menu was empty.")));
            return;
        }

        plugin.getEconomyHandler().deposit(player, total);
        player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.sold-all-summary", "&aSold %count% items for a total of %total%!"))
                .replace("%count%", String.valueOf(soldCount))
                .replace("%total%", plugin.getEconomyHandler().format(total)));
    }

    private void giveOrDrop(Player player, ItemStack item) {
        var leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    /** Called from onDisable so items are never lost if the server stops while a menu is open. */
    public void finalizeAllOpenSessions() {
        for (var entry : plugin.getSessionManager().getOpenSellSessions().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Inventory inv = entry.getValue();
            if (player != null && inv.getHolder() instanceof SellHolder holder) {
                finalizeSell(player, inv, holder);
            }
        }
    }
}
