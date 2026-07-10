> **ESTADO:** ✅ Resuelto en commit `bfb221e`.

#### Resumen
El patrón `get capability → mustSelectPath → isItemValid → cancel + warn` se repite literalmente en 5 event handlers. Cualquier cambio de comportamiento requiere editar 5 archivos.

#### Problema
**Archivos afectados:**
- `src/main/java/org/xam/event/BlockEventHandler.java:19-56` (2 métodos: `onBreakSpeed`, `onBlockBreak`)
- `src/main/java/org/xam/event/ItemEventHandler.java:36-93` (3 métodos: `onRightClickItem`, `onRightClickBlock`, `onEntityInteract`)

Patrón duplicado:
```java
// ItemEventHandler.java:36-50 (y 4 lugares más idénticos)
player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
    if (MasteryService.mustSelectPath(player, data)) {
        event.setCanceled(true);
        MessageUtils.sendWarning(player, Component.translatable("xam.msg.must_select_path"));
        return;
    }
    ItemStack stack = event.getItemStack();
    if (!stack.isEmpty()) {
        MasteryService.checkAndRefreshPlayerData(player, data);
        if (!MasteryService.isItemValid(stack, data)) {
            event.setCanceled(true);
            MessageUtils.sendItemWarning(player, stack);
        }
    }
});
```

#### Fix propuesto
Crear helper `MasteryGuard`:

```java
// Nuevo archivo: src/main/java/org/xam/progression/MasteryGuard.java
package org.xam.progression;

public enum MasteryGuardResult {
    ALLOW,
    MUST_SELECT_PATH,
    ITEM_INVALID
}

public final class MasteryGuard {
    private MasteryGuard() {}

    public static MasteryGuardResult enforceItemRestriction(Player player, ItemStack stack) {
        return player.getCapability(PlayerDataProvider.PLAYER_DATA).map(data -> {
            if (MasteryService.mustSelectPath(player, data)) {
                return MasteryGuardResult.MUST_SELECT_PATH;
            }
            if (!stack.isEmpty()) {
                MasteryService.checkAndRefreshPlayerData(player, data);
                if (!MasteryService.isItemValid(stack, data)) {
                    return MasteryGuardResult.ITEM_INVALID;
                }
            }
            return MasteryGuardResult.ALLOW;
        }).orElse(MasteryGuardResult.ALLOW);
    }

    public static void applyCancel(Player player, ItemStack stack, MasteryGuardResult result,
                                    java.util.function.Consumer<String> warningSender) {
        switch (result) {
            case MUST_SELECT_PATH -> warningSender.accept("xam.msg.must_select_path");
            case ITEM_INVALID -> warningSender.accept("xam.msg.item_invalid");
            default -> {}
        }
    }
}
```

Cada handler se reduce a:
```java
// ItemEventHandler.java
@SubscribeEvent
public static void onRightClickItem(RightClickItem event) {
    if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
        ItemStack stack = event.getItemStack();
        MasteryGuardResult result = MasteryGuard.enforceItemRestriction(player, stack);
        if (result != MasteryGuardResult.ALLOW) {
            event.setCanceled(true);
            MasteryGuard.applyCancel(player, stack, result, key ->
                MessageUtils.sendWarning(player, Component.translatable(key)));
        }
    }
}
```

#### Esfuerzo
1 día (diseñar el helper + refactorizar 5 handlers + tests manuales en juego).

#### Dependencias
Ninguna.

---