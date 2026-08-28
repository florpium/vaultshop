package com.vaultshop.core.shop;

import com.vaultshop.core.VaultShop;
import com.vaultshop.core.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI {

    public static final int PER_PAGE = 45;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_SEARCH = 48;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_CLOSE = 50;
    public static final int SLOT_NEXT = 53;

    private final VaultShop plugin;

    public ShopGUI(VaultShop plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, List<Material> items, int page, String searchTerm) {
        int maxPage = items.isEmpty() ? 0 : (items.size() - 1) / PER_PAGE;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        String title = ItemUtil.color(plugin.getConfig().getString("gui.shop-title", "&2&lServer Shop"));
        if (searchTerm != null) title = title + " §7(" + searchTerm + ")";
        // Vanilla inventory titles cap out around 32 visible chars; keep it short & safe.
        if (title.length() > 32) title = title.substring(0, 32);

        ShopHolder holder = new ShopHolder(items, page, searchTerm);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, items.size());
        for (int i = start; i < end; i++) {
            Material material = items.get(i);
            inv.setItem(i - start, buildSlotIcon(player, material));
        }
        // Last page may not fill all 45 slots - plug the gaps so nothing can be stashed there.
        ItemStack emptySlotFiller = ItemUtil.namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = end - start; slot < PER_PAGE; slot++) {
            inv.setItem(slot, emptySlotFiller);
        }

        // Fill the whole nav bar with filler first so there is never an empty top-inventory
        // slot a shift-click or drag could accidentally strand a real item in.
        ItemStack filler = ItemUtil.namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        if (page > 0) inv.setItem(SLOT_PREV, ItemUtil.namedItem(Material.ARROW, "&aPrevious Page"));
        inv.setItem(SLOT_SEARCH, ItemUtil.namedItem(Material.COMPASS, "&eSearch", "&7Click to search by name"));
        inv.setItem(SLOT_INFO, ItemUtil.namedItem(Material.PAPER, "&fPage " + (page + 1) + " / " + (maxPage + 1)));
        inv.setItem(SLOT_CLOSE, ItemUtil.namedItem(Material.BARRIER, "&cClose"));
        if (page < maxPage) inv.setItem(SLOT_NEXT, ItemUtil.namedItem(Material.ARROW, "&aNext Page"));

        player.openInventory(inv);
    }

    public ItemStack buildSlotIcon(Player player, Material material) {
        PriceManager pm = plugin.getPriceManager();
        double sell = pm.getSellPrice(material);
        double buy = pm.getBuyPrice(material);
        int owned = countOwned(player, material);
        return ItemUtil.buildIcon(material, sell, buy, owned,
                plugin.getEconomyHandler().format(sell), plugin.getEconomyHandler().format(buy));
    }

    public static int countOwned(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) total += stack.getAmount();
        }
        return total;
    }

    public List<Material> filter(String term) {
        List<Material> result = new ArrayList<>();
        String needle = term.toLowerCase();
        for (Material m : plugin.getPriceManager().getAllSellable()) {
            if (ItemUtil.prettyName(m).toLowerCase().contains(needle) || m.name().toLowerCase().contains(needle)) {
                result.add(m);
            }
        }
        return result;
    }
}
