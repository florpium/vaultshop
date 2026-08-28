package com.vaultshop.core.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {

    private ItemUtil() {}

    public static String prettyName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1).toLowerCase())
              .append(' ');
        }
        return sb.toString().trim();
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /**
     * Builds a display icon for the shop catalog: real item, custom name, and lore
     * showing sell/buy price plus how many the player currently has. This lore is what
     * shows up automatically when a player hovers the item in the GUI.
     */
    public static ItemStack buildIcon(Material material, double sellPrice, double buyPrice, int owned, String formattedSell, String formattedBuy) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&f" + prettyName(material)));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Sell price: &a" + formattedSell + " &7each"));
            lore.add(color("&7Buy price: &6" + formattedBuy + " &7each"));
            lore.add(color("&8You have: &7" + owned));
            lore.add("");
            lore.add(color("&eLeft-click &7to sell 1"));
            lore.add(color("&eShift-click &7to sell all you have"));
            lore.add(color("&eRight-click &7to buy 1"));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack namedItem(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String l : lore) loreList.add(color(l));
                meta.setLore(loreList);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
