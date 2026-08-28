package com.vaultshop.core.listeners;

import com.vaultshop.core.VaultShop;
import com.vaultshop.core.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.UUID;

public class SearchChatListener implements Listener {

    private final VaultShop plugin;

    public SearchChatListener(VaultShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // consumeAwaitingSearch removes the flag immediately so only exactly one message
        // is ever intercepted, and expired prompts fall through to normal chat.
        if (!plugin.getSessionManager().consumeAwaitingSearch(uuid)) return;

        event.setCancelled(true);
        String term = event.getMessage().trim();

        // Must hop back to the main thread before touching any Bukkit API - this event is async.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (term.equalsIgnoreCase("cancel")) {
                player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.search-cancelled", "&7Search cancelled.")));
                return;
            }
            List<org.bukkit.Material> results = plugin.getShopGUI().filter(term);
            if (results.isEmpty()) {
                player.sendMessage(ItemUtil.color(plugin.getConfig().getString("messages.search-none", "&cNo items matched '%term%'."))
                        .replace("%term%", term));
                return;
            }
            plugin.getShopGUI().open(player, results, 0, term);
        });
    }
}
