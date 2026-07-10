> **ESTADO:** ✅ Resuelto en commit `5d83885`.

#### Resumen
`PlayerEventHandler.onPlayerTick` hace `ForgeRegistries.MOB_EFFECTS.getValue(effectRl)` cada 20 ticks por cada path mastered del jugador. El `MobEffect` solo cambia en config reload.

#### Problema
**Archivo:** `src/main/java/org/xam/event/PlayerEventHandler.java:122-134`

```java
// Corre cada 20 ticks (1 segundo) por jugador
for (String pathId : data.getMasteredPaths()) {
    PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
    if (path != null && path.perkEffect != null && !path.perkEffect.isEmpty()) {
        ResourceLocation effectRl = ResourceLocation.tryParse(path.perkEffect);
        if (effectRl != null) {
            net.minecraft.world.effect.MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectRl);
            if (effect != null) {
                player.addEffect(new MobEffectInstance(effect, 300, path.perkAmplifier, true, false, false));
            }
        }
    }
}
```

Con 10 paths mastered y 20 jugadores: 200 `ResourceLocation.tryParse` + 200 lookups de registro + 200 `addEffect` por segundo, todo trabajo innecesario.

#### Fix propuesto
1. Agregar campo cacheado en `PathInfo`:

```java
// PathInfo.java
public class PathInfo {
    // ... existing fields ...
    public transient net.minecraft.world.effect.MobEffect perkEffectCached;
}
```

2. Poblar el cache en `ConfigManager.parseJson` después de parsear el JSON:

```java
// ConfigManager.parseJson, después de parsear paths
for (PathInfo info : PATHS) {
    if (info.perkEffect != null && !info.perkEffect.isEmpty()) {
        ResourceLocation rl = ResourceLocation.tryParse(info.perkEffect);
        if (rl != null) {
            info.perkEffectCached = ForgeRegistries.MOB_EFFECTS.getValue(rl);
            if (info.perkEffectCached == null) {
                XamConstants.LOGGER.warn("Path {} has unknown perkEffect: {}", info.id, info.perkEffect);
            }
        }
    }
}
```

3. Simplificar el handler:

```java
// PlayerEventHandler.onPlayerTick
for (String pathId : data.getMasteredPaths()) {
    PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
    if (path != null && path.perkEffectCached != null) {
        player.addEffect(new MobEffectInstance(path.perkEffectCached, 400, path.perkAmplifier, true, false, false));
    }
}
```

Nota: subir duration de 300 a 400 ticks (20s) para que solape el refresh de 1s.

#### Esfuerzo
3 horas.

#### Dependencias
Ninguna. Si se hace el issue #19 (refactor de PathInfo a record), el campo `perkEffectCached` debe ser `transient` o excluirse del record.

---