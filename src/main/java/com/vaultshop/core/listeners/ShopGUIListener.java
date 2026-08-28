package com.vaultshop.core.listeners;

import com.vaultshop.core.VaultShop;
import com.vaultshop.core.shop.PriceManager;
import com.vaultshop.core.shop.ShopGUI;
import com.vaultshop.core.shop.ShopHolder;
import com.vaultshop.core.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopGUIListener implements Listener {

    private final VaultShop plugin;

    public ShopGUIListener(VaultShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ShopHolder holder)) return;
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof ShopHolder)) {
            // Click landed in the player's own inventory (bottom half) while the shop is open.
            // Only worry about shift-clicks, which would otherwise try to shove a real item
            // into the display-only catalog above and strand it there. Everything else (normal
            // clicks rearranging their own inventory) is left completely alone.
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true); // catalog icons are display-only, never movable

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getSlot();
        if (slot < 0 || slot >= event.getClickedInventory().getSize()) return;

        List<Material> items = holder.getItems();
        int page = holder.getPage();

        if (slot >= ShopGUI.PER_PAGE) {
            handleNav(player, holder, slot);
            return;
        }

        int index = page * ShopGUI.PER_PAGE + slot;
        if (index >= items.size()) return;
        Material material = items.get(index);

        PriceManager pm = plugin.getPriceManager();
        ClickType click = event.getClick();

        if (click == ClickType.RIGHT) {
            buyOne(player, material);
        } else if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            sellAll(player, material);
        } else {
            sellOne(player, material);
        }

        // Refresh just this slot so the "you have: N" lore stays accurate.
        event.getInventory().setItem(slot, plugin.getShopGUI().buildSlotIcon(player, material));
    }

    private void handleNav(Player player, ShopHolder holder, int slot) {
        if (slot == ShopGUI.SLOT_PREV) {
            plugin.getShopGUI().open(player, holder.getItems(), holder.getPage() - 1, holder.getSearchTerm());
        } else if (slot == ShopGUI.SLOT_NEXT) {
            plugin.getShopGUI().open(player, holder.getItems(), holder.getPage() + 1, holder.getSearchTerm());
        } else if (slot == ShopGUI.SLOT_CLOSE) {
            player.closeInventory();
        } else if (slot == ShopGUI.SLOT_SEARCH) {
            player.closeInventory();
            plugin.getSessionManager().startAwaitingSearch(player.getUniqueId());
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.search-prompt", "&eType an item name in chat to search, or type 'cancel'.")));
        }
    }

    private void sellOne(Player player, Material material) {
        PriceManager pm = plugin.getPriceManager();
        ItemStack found = takeOne(player, material);
        if (found == null) {
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.sold-none", "&cYou don't have any of that to sell.")));
            return;
        }
        double price = pm.getSellPrice(material);
        plugin.getEconomyHandler().deposit(player, price);
        player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.sold-one", "&aSold %amount%x %item% for %price%."))
                .replace("%amount%", "1")
                .replace("%item%", ItemUtil.prettyName(material))
                .replace("%price%", plugin.getEconomyHandler().format(price)));
    }

    private void sellAll(Player player, Material material) {
        PriceManager pm = plugin.getPriceManager();
        int count = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
                player.getInventory().setItem(i, null);
            }
        }
        if (count == 0) {
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.sold-none", "&cYou don't have any of that to sell.")));
            return;
        }
        double total = pm.getSellPrice(material) * count;
        plugin.getEconomyHandler().deposit(player, total);
        player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.sold-one", "&aSold %amount%x %item% for %price%."))
                .replace("%amount%", String.valueOf(count))
                .replace("%item%", ItemUtil.prettyName(material))
                .replace("%price%", plugin.getEconomyHandler().format(total)));
    }

    private void buyOne(Player player, Material material) {
        PriceManager pm = plugin.getPriceManager();
        double price = pm.getBuyPrice(material);
        if (!plugin.getEconomyHandler().has(player, price)) {
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.buy-fail-money", "&cYou can't afford that! It costs %price%."))
                    .replace("%price%", plugin.getEconomyHandler().format(price)));
            return;
        }
        ItemStack toGive = new ItemStack(material, 1);
        if (!player.getInventory().addItem(toGive).isEmpty()) {
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.buy-fail-space", "&cYour inventory is full!")));
            return;
        }
        plugin.getEconomyHandler().withdraw(player, price);
        player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.bought-one", "&aBought %amount%x %item% for %price%."))
                .replace("%amount%", "1")
                .replace("%item%", ItemUtil.prettyName(material))
                .replace("%price%", plugin.getEconomyHandler().format(price)));
    }

    private ItemStack takeOne(Player player, Material material) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == material) {
                stack.setAmount(stack.getAmount() - 1);
                player.getInventory().setItem(i, stack.getAmount() <= 0 ? null : stack);
                return stack;
            }
        }
        return null;
    }
}
