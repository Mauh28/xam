#### Resumen
Dos packets cliente→servidor no tienen rate limiting y son vulnerables a amplificación:
- `SelectPathPacket`: 1 byte request → full player NBT response (serialización + broadcast)
- `RequestConfigPacket`: 1 byte request → 256 KB response → **amplificación 256×**

#### Problema

**`SelectPathPacket.java:33-43`** — sin rate limiting:
```java
// Cualquier cliente puede spammear esto
if (pkt.pathId != null) {
    PathInfo targetPath = ConfigManager.PATHS_MAP.get(pkt.pathId);
    if (targetPath == null || targetPath.requirements.isEmpty()
        || data.getMasteredPaths().contains(pkt.pathId)
        || !MasteryService.areDependenciesMastered(player, data, targetPath)) {
        MasteryService.sync(player);  // ← respuesta costosa en cada paquete
        return;
    }
}
```

**`RequestConfigPacket.java:21-28`** — sin rate limiting:
```java
// Cualquier cliente puede pedir el JSON completo en cualquier momento
context.enqueueWork(() -> {
    ServerPlayer player = context.getSender();
    if (player != null) {
        // Responde con SyncConfigPacket de hasta 256 KB
        XamNetwork.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SyncConfigPacket(ConfigManager.getConfigJson())
        );
    }
});
```

#### Impacto
Un cliente malicioso puede:
- Spammear `SelectPathPacket` → amplificar 1 byte en NBT completo del jugador (serialización + envío)
- Spammear `RequestConfigPacket` → amplificar 1 byte en 256 KB (256× ratio)
- En un servidor con 50 jugadores, esto puede saturar el uplink del servidor

#### Fix propuesto
Agregar cooldown por jugador. Opción más simple: map estático en el handler:

```java
// Nuevo archivo: src/main/java/org/xam/network/PacketRateLimiter.java
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
```

Usar en los handlers:
```java
// SelectPathPacket handler
if (!PacketRateLimiter.canSelectPath(player.getUUID())) {
    XamConstants.LOGGER.warn("Player {} rate-limited on SelectPathPacket", player.getName().getString());
    return;
}
```

```java
// RequestConfigPacket handler
if (!PacketRateLimiter.canRequestConfig(player.getUUID())) {
    return; // Silently drop — no logging to avoid log spam
}
```

Limpiar en `PlayerLoggedOutEvent`:
```java
// PlayerEventHandler
@SubscribeEvent
public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
    PacketRateLimiter.clear(event.getEntity().getUUID());
}
```

#### Esfuerzo
4 horas.

#### Dependencias
Ninguna.

---