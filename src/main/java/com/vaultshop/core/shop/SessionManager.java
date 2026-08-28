package com.vaultshop.core.shop;

import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Small in-memory tracker for transient per-player shop state. Nothing here is persisted. */
public class SessionManager {

    private final Map<UUID, Inventory> openSellSessions = new HashMap<>();
    private final Map<UUID, Long> awaitingSearch = new HashMap<>();
    private static final long SEARCH_TIMEOUT_MS = 30_000L;

    public void trackSellSession(UUID uuid, Inventory inv) {
        openSellSessions.put(uuid, inv);
    }

    public void clearSellSession(UUID uuid) {
        openSellSessions.remove(uuid);
    }

    public Map<UUID, Inventory> getOpenSellSessions() {
        return openSellSessions;
    }

    public void startAwaitingSearch(UUID uuid) {
        awaitingSearch.put(uuid, System.currentTimeMillis());
    }

    /** Returns true and consumes the state if the player has a live (non-expired) search prompt open. */
    public boolean consumeAwaitingSearch(UUID uuid) {
        Long started = awaitingSearch.remove(uuid);
        if (started == null) return false;
        return (System.currentTimeMillis() - started) <= SEARCH_TIMEOUT_MS;
    }

    public void cancelAwaitingSearch(UUID uuid) {
        awaitingSearch.remove(uuid);
    }
}
