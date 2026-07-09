package org.xam.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketRateLimiter {
    private static final Map<UUID, Long> LAST_PATH_SELECT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CONFIG_REQUEST = new ConcurrentHashMap<>();
    private static final long PATH_SELECT_COOLDOWN_MS = 1000;      // 1 select/seg
    private static final long CONFIG_REQUEST_COOLDOWN_MS = 5000;   // 1 request/5 seg

    private PacketRateLimiter() {}

    public static boolean canSelectPath(UUID playerId) {
        return check(LAST_PATH_SELECT, playerId, PATH_SELECT_COOLDOWN_MS);
    }

    public static boolean canRequestConfig(UUID playerId) {
        return check(LAST_CONFIG_REQUEST, playerId, CONFIG_REQUEST_COOLDOWN_MS);
    }

    private static boolean check(Map<UUID, Long> map, UUID id, long cooldownMs) {
        long now = System.currentTimeMillis();
        Long last = map.get(id);
        if (last != null && (now - last) < cooldownMs) return false;
        map.put(id, now);
        return true;
    }

    public static void clear(UUID playerId) {
        LAST_PATH_SELECT.remove(playerId);
        LAST_CONFIG_REQUEST.remove(playerId);
    }
}
