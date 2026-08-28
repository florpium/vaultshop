package com.vaultshop.core.commands;

import com.vaultshop.core.VaultShop;
import com.vaultshop.core.shop.PriceManager;
import com.vaultshop.core.shop.SellGUI;
import com.vaultshop.core.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand implements CommandExecutor {

    private final VaultShop plugin;

    public SellCommand(VaultShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("hand")) {
            sellHand(player);
            return true;
        }

        SellGUI.open(plugin, player);
        return true;
    }

    private void sellHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        Material type = hand.getType();
        PriceManager pm = plugin.getPriceManager();

        if (type.isAir() || !pm.isSellable(type)) {
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.sold-none", "&cYou don't have any of that to sell.")));
            return;
        }

        int amount = hand.getAmount();
        double total = pm.getSellPrice(type) * amount;
        player.getInventory().setItemInMainHand(null);
        plugin.getEconomyHandler().deposit(player, total);
        player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.sold-one", "&aSold %amount%x %item% for %price%."))
                .replace("%amount%", String.valueOf(amount))
                .replace("%item%", ItemUtil.prettyName(type))
                .replace("%price%", plugin.getEconomyHandler().format(total)));
    }
}
