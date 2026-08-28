package com.vaultshop.core.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyHandler {

    private Economy economy;

    /** Returns true if Vault + an economy plugin were found and hooked successfully. */
    public boolean setup(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    public void deposit(OfflinePlayer player, double amount) {
        economy.depositPlayer(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (economy == null) return String.format("%.2f", amount);
        return economy.format(amount);
    }

    public Economy raw() {
        return economy;
    }
}
