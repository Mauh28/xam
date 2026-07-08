#### Resumen
`ConfigManager` muta `PATHS/PATHS_MAP/UNIVERSAL_NAMESPACES` directamente y broadcastea reload bumpando `configVersion`, dependiendo de `MasteryService.checkAndRefreshPlayerData` para reconciliar state por-jugador en el siguiente tick. Esto deja una ventana de 1 tick donde `PlayerData.activePathModId` está stale.

#### Problema
**Archivo:** `src/main/java/org/xam/config/ConfigManager.java` + `src/main/java/org/xam/progression/MasteryService.java:36-60`

Patrón actual:
1. OP hace `/xam reload` o envía `UpdateConfigPacket`
2. `ConfigManager.loadConfigFromJson` parsea JSON, actualiza campos static, bumpa `configVersion`
3. En el siguiente tick de cada jugador, `MasteryService.checkAndRefreshPlayerData` nota que `data.configVersion < ConfigManager.configVersion` y reconcilia

**Problema:** 1 tick de stale state. Si un jugador hace click en ese tick, puede usar items inválidos o ver paths desactualizados.

#### Fix propuesto
Implementar observer pattern con evento de Forge:

```java
// Nuevo archivo: src/main/java/org/xam/config/ConfigReloadedEvent.java
package org.xam.config;

import net.minecraftforge.eventbus.api.Event;

public class ConfigReloadedEvent extends Event {
    private final int newVersion;
    public ConfigReloadedEvent(int newVersion) { this.newVersion = newVersion; }
    public int getNewVersion() { return newVersion; }
}
```

```java
// ConfigManager.loadConfigFromJson — al final
MinecraftForge.EVENT_BUS.post(new ConfigReloadedEvent(configVersion));
```

```java
// MasteryService
@SubscribeEvent
public static void onConfigReloaded(ConfigReloadedEvent event) {
    // Reconciliar TODOS los jugadores online syncrónicamente
    for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            checkAndRefreshPlayerData(player, data);
            sync(player);
        });
    }
}
```

#### Beneficios
1. No hay ventana de stale state
2. Lógica de reconciliación centralizada en un handler, no dispersa en checks por tick
3. Permite que otros subsistemas reaccionen al reload (ej.: JEI integration puede invalidar caches)
4. `checkAndRefreshPlayerData` puede simplificarse (solo corre en el evento)

#### Esfuerzo
2 días.

#### Dependencias
- Issue #18 (split MasteryService) — preferible hacer primero para que el handler viva en el servicio correcto.

---

## Resumen para milestone planning

| Milestone | Issues | Esfuerzo total | Outcome |
|-----------|--------|----------------|---------|
| **v1.0.1 — Quick fixes** | #1, #2, #3, #4, #5, #6, #7, #8 | 1 día | 2 bugs reales + 6 landmines arreglados |
| **v1.1 — Architecture cleanup** | #9, #10, #11, #12, #13, #14 | 1-2 semanas | Seguridad, performance, dedup |
| **v1.2 — i18n + validation** | #15, #16 | 1-2 semanas | Mod jugable en inglés, config robusta |
| **v2.0 — Major refactor** | #17, #18, #19, #20, #21, #22 | 4-6 semanas | Codebase mantenible para crecimiento futuro |

---

## Cómo usar este archivo

1. Andá a https://github.com/Mauh28/xam/issues/new
2. Copiá el contenido de un issue (entre los divisores `---`)
3. El título va en el campo "Title" del issue (lo que está después de `### ISSUE #N —`)
4. Los labels van en el campo "Labels" (crearlos primero si no existen — ver lista al inicio)
5. El resto del markdown va en el cuerpo del issue
6. Repetir para cada issue

**Tip:** Si vas a subir muchos issues de una, considerá usar `gh` CLI:
```bash
gh issue create --title "fix: typo sr → sb en drawFlatPanel" \
                --body-file issue_01.md \
                --label "quick-win,P1,bug,GUI"
```

Donde `issue_01.md` contiene el cuerpo de cada issue (sin el título ni los labels, que van como flags).