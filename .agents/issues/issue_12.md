> **ESTADO:** ✅ Resuelto en commit `c576682`.

#### Resumen
`ItemUtils.getPathFromItemTags` hace DOS linear scans sobre `PATHS` en el hottest path del mod (cada evento de item: tick, click, craft, equip, tooltip). Un map `namespaceToPath` resolvería el primer scan en O(1).

#### Problema
**Archivo:** `src/main/java/org/xam/util/ItemUtils.java:56-79`

```java
public static String getPathFromItemTags(ItemStack stack) {
    if (stack.isEmpty()) return null;
    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
    if (rl == null) return null;

    // PRIMER scan: match por mod_id
    for (PathInfo path : ConfigManager.PATHS) {
        if (path.mod_id != null && path.mod_id.equals(rl.getNamespace())) {
            return path.id;
        }
    }

    // SEGUNDO scan: match por tag (fallback)
    for (PathInfo path : ConfigManager.PATHS) {
        if (path.armorTag != null) {
            if (stack.is(path.armorTag) || stack.is(path.weaponsTag) || stack.is(path.toolsTag)) {
                return path.id;
            }
        }
    }
    return null;
}
```

Llamado desde `MasteryService.isItemValid` (`MasteryService.java:262`), que es llamado desde:
- `LivingHurtEvent` (cada hit de combate)
- `BreakSpeed` + `BlockEvent.Break` (cada rotura de bloque)
- `RightClickItem` + `RightClickBlock` + `EntityInteract` (cada click)
- `PlayerEventHandler.onPlayerTick` (cada tick × mainhand + offhand × jugador)
- `ClientForgeEvents.onItemTooltip` (cada frame de hover)
- JEI recipe rendering
- Jade tooltips

**Es el hottest path del mod.**

#### Fix propuesto
1. Agregar map en `ConfigManager`:

```java
// ConfigManager.java
public static Map<String, PathInfo> NAMESPACE_TO_PATH = new HashMap<>();
```

2. Poblar en `parseJson` después de cargar `PATHS`:

```java
// ConfigManager.parseJson
NAMESPACE_TO_PATH.clear();
for (PathInfo info : PATHS) {
    if (info.mod_id != null && !info.mod_id.isEmpty()) {
        NAMESPACE_TO_PATH.put(info.mod_id, info);
    }
}
```

3. Reemplazar el primer scan en `ItemUtils`:

```java
// ItemUtils.getPathFromItemTags
public static String getPathFromItemTags(ItemStack stack) {
    if (stack.isEmpty()) return null;
    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
    if (rl == null) return null;

    // O(1) lookup
    PathInfo byNamespace = ConfigManager.NAMESPACE_TO_PATH.get(rl.getNamespace());
    if (byNamespace != null) return byNamespace.id;

    // Fallback: tag scan (raro, solo items cross-mod)
    for (PathInfo path : ConfigManager.PATHS) {
        if (path.armorTag != null) {
            if (stack.is(path.armorTag) || stack.is(path.weaponsTag) || stack.is(path.toolsTag)) {
                return path.id;
            }
        }
    }
    return null;
}
```

#### Esfuerzo
3 horas.

#### Dependencias
Ninguna.

#### Nota
El segundo scan (tag fallback) es más difícil de optimizar porque requeriría un index inverso `TagKey<Item> → List<PathInfo>`. Por ahora dejarlo como scan lineal (es raro que se ejecute porque casi todos los items matchean por namespace).

---