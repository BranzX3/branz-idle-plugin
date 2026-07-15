package com.example.plugin.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility cache preventing double-click exploits across GUI interactions by enforcing a 200ms cooldown.
 */
public class DebounceCache {

    private static final Map<UUID, Long> LAST_CLICK_TIMES = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MILLIS = 200L;

    /**
     * Checks if the player is allowed to perform a click action right now.
     *
     * @return true if allowed, false if on cooldown (should cancel/ignore click)
     */
    public static boolean tryClick(UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = LAST_CLICK_TIMES.get(playerId);
        if (last != null && (now - last) < COOLDOWN_MILLIS) {
            return false;
        }
        LAST_CLICK_TIMES.put(playerId, now);
        return true;
    }

    public static void clearPlayer(UUID playerId) {
        LAST_CLICK_TIMES.remove(playerId);
    }
}
