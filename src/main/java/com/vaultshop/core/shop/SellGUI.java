package com.vaultshop.core.shop;

import com.vaultshop.core.VaultShop;
import com.vaultshop.core.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SellGUI {

    public static final int SIZE = 45;

    public static Inventory open(VaultShop plugin, Player player) {
        String title = ItemUtil.color(plugin.getConfig().getString("gui.sell-title", "&aSell Items - Close inventory to sell"));
        if (title.length() > 32) title = title.substring(0, 32);
        SellHolder holder = new SellHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);
        plugin.getSessionManager().trackSellSession(player.getUniqueId(), inv);
        player.openInventory(inv);
        return inv;
    }
}
