package com.vaultshop.core.commands;

import com.vaultshop.core.VaultShop;
import com.vaultshop.core.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final VaultShop plugin;

    public ShopCommand(VaultShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            plugin.getShopGUI().open(player, plugin.getPriceManager().getAllSellable(), 0, null);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("search") && args.length >= 2) {
            String term = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            var results = plugin.getShopGUI().filter(term);
            if (results.isEmpty()) {
                player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.search-none", "&cNo items matched '%term%'.")).replace("%term%", term));
                return true;
            }
            plugin.getShopGUI().open(player, results, 0, term);
            return true;
        }

        if (sub.equals("reload")) {
            if (!player.hasPermission("vaultshop.admin")) {
                player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to do that.")));
                return true;
            }
            plugin.reloadConfig();
            plugin.getPriceManager().load();
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.reload-done", "&aVaultShop config and prices reloaded.")));
            return true;
        }

        if (sub.equals("setprice") && args.length >= 4) {
            if (!player.hasPermission("vaultshop.admin")) {
                player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to do that.")));
                return true;
            }
            Material material = Material.matchMaterial(args[1]);
            if (material == null) {
                player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.invalid-material", "&cUnknown item: %input%")).replace("%input%", args[1]));
                return true;
            }
            boolean isBuy = args[2].equalsIgnoreCase("buy");
            double amount;
            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.invalid-price", "&cPrice must be a positive number.")));
                return true;
            }
            if (amount < 0) {
                player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.invalid-price", "&cPrice must be a positive number.")));
                return true;
            }
            plugin.getPriceManager().setPrice(material, isBuy, amount);
            player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.price-set", "&aSet %type% price of %item% to %price%."))
                    .replace("%type%", isBuy ? "buy" : "sell")
                    .replace("%item%", ItemUtil.prettyName(material))
                    .replace("%price%", plugin.getEconomyHandler().format(amount)));
            return true;
        }

        player.sendMessage(ItemUtil.color("&cUsage: /shop [search <term>|reload|setprice <material> <buy|sell> <amount>]"));
        return true;
    }
}
