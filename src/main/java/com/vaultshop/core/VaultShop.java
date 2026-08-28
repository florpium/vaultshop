package com.vaultshop.core;

import com.vaultshop.core.commands.SellCommand;
import com.vaultshop.core.commands.ShopCommand;
import com.vaultshop.core.economy.EconomyHandler;
import com.vaultshop.core.listeners.SearchChatListener;
import com.vaultshop.core.listeners.SellGUIListener;
import com.vaultshop.core.listeners.ShopGUIListener;
import com.vaultshop.core.shop.PriceManager;
import com.vaultshop.core.shop.SessionManager;
import com.vaultshop.core.shop.ShopGUI;
import com.vaultshop.core.util.ItemUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultShop extends JavaPlugin {

    private EconomyHandler economyHandler;
    private PriceManager priceManager;
    private SessionManager sessionManager;
    private ShopGUI shopGUI;
    private SellGUIListener sellGUIListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economyHandler = new EconomyHandler();
        if (!economyHandler.setup(this)) {
            getLogger().warning(ItemUtil.color(getConfig().getString("messages.no-economy", "&cNo economy plugin hooked into Vault! Disabling shop.")));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        sessionManager = new SessionManager();
        priceManager = new PriceManager(this);
        priceManager.load();
        shopGUI = new ShopGUI(this);
        sellGUIListener = new SellGUIListener(this);

        getServer().getPluginManager().registerEvents(new ShopGUIListener(this), this);
        getServer().getPluginManager().registerEvents(sellGUIListener, this);
        getServer().getPluginManager().registerEvents(new SearchChatListener(this), this);

        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("sell").setExecutor(new SellCommand(this));

        getLogger().info("VaultShop enabled - hooked into Vault, " + priceManager.getAllSellable().size() + " sellable items loaded.");
    }

    @Override
    public void onDisable() {
        if (sellGUIListener != null) {
            sellGUIListener.finalizeAllOpenSessions();
        }
        if (priceManager != null) {
            priceManager.save();
        }
    }

    public EconomyHandler getEconomyHandler() { return economyHandler; }
    public PriceManager getPriceManager() { return priceManager; }
    public SessionManager getSessionManager() { return sessionManager; }
    public ShopGUI getShopGUI() { return shopGUI; }
}
