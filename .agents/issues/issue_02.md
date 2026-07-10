> **ESTADO:** ✅ Resuelto en commit `7277e1b`.

#### Resumen
El comando `/xam reset` limpia `currentPath`, `masteredPaths` y `completedRequirements`, pero NO limpia `startedPaths`. Después de un "reset", el jugador sigue pudiendo usar items de cualquier path que haya iniciado previamente, derrotando el propósito del reset.

#### Problema
**Archivo:** `src/main/java/org/xam/command/XamCommand.java:74-83`

```java
player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
    data.setCurrentPath(null);
    data.getMasteredPaths().clear();
    data.clearCompletedRequirements();
    data.setDevMode(false);
    // FALTA: data.getStartedPaths().clear();
    MasteryService.sync(player);
    MasteryService.updateArmorModifiers(player);
    context.getSource().sendSuccess(() -> Component.literal("Progreso de maestría restablecido para " + player.getGameProfile().getName()), true);
});
```

#### Impacto
`MasteryService.isItemValid` (`MasteryService.java:306-320`) permite items de paths en `startedPaths`. Un jugador "reset" puede seguir crafteando y usando items de mods que inició antes del reset.

Grep confirma que `startedPaths.remove` y `getStartedPaths().remove` **no aparecen en ningún lugar** del codebase.

#### Fix propuesto
```java
// XamCommand.java:74-83
player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
    data.setCurrentPath(null);
    data.getMasteredPaths().clear();
+   data.getStartedPaths().clear();
    data.clearCompletedRequirements();
    data.setDevMode(false);
    MasteryService.sync(player);
    MasteryService.updateArmorModifiers(player);
    ...
});
```

Considerar también exponer `clearStartedPaths()` en `PlayerData` para mejor encapsulación.

#### Esfuerzo
5 minutos.

#### Dependencias
Ninguna.

#### Cómo verificar
1. OP le da a un jugador una path (`/xam select botania <player>`)
2. El jugador puede usar items de botania ✓
3. OP hace `/xam reset <player>`
4. El jugador NO debería poder usar items de botania (BUG actual: sí puede)

---