> **ESTADO:** ✅ Resuelto en commit `6b6078d`.

#### Resumen
`PathInfo` y `Requirement` son holders anémicos con campos públicos mutables. El cacheo de `TagKey` en `PathInfo` es mutable y cualquiera puede nullificarlo. Convertirlos a records (o al menos hacer campos package-private con getters) mejora encapsulación y previene state corruption.

#### Problema
**Archivos:**
- `src/main/java/org/xam/config/PathInfo.java:8-22` — campos `public String id, name, mod_id, icon, perkEffect; public int perkAmplifier, minToSwitch; public List<String> dependencies, requirements; public TagKey<Item> armorTag, weaponsTag, toolsTag;`
- `src/main/java/org/xam/config/Requirement.java:7-11` — campos `public String id, name, type, target, count;`

#### Fix propuesto
Migrar a Java 17 records:

```java
// PathInfo.java
public record PathInfo(
    String id,
    String name,
    String modId,
    String icon,
    String perkEffect,
    int perkAmplifier,
    int minToSwitch,
    List<String> dependencies,
    List<String> requirementIds,
    // Cacheados (transient — no serializados a JSON)
    TagKey<Item> armorTag,
    TagKey<Item> weaponsTag,
    TagKey<Item> toolsTag,
    MobEffect perkEffectCached  // si issue #11 está hecho
) {
    public PathInfo {
        // Defensive copies
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
    }

    // Builder para parseo incremental en ConfigManager
    public static class Builder { ... }
}
```

```java
// Requirement.java
public record Requirement(
    String id,
    String name,
    String type,      // "craft" | "collect" | "kill" | "advancement"
    String target,
    int count
) {
    public Requirement {
        if (!Set.of("craft", "collect", "kill", "advancement").contains(type)) {
            throw new IllegalArgumentException("Unknown requirement type: " + type);
        }
    }
}
```

#### Consideraciones
- `ConfigManager.parseJson` debe cambiar de mutar campos a construir via Builder
- Todos los callers que hacen `path.id` pasan a `path.id()` (accessor de record)
- Si se requiere mutación temporal durante parseo (ej.: setear defaults después de parse), usar Builder pattern
- Gson puede deserializar records con el constructor canónico desde Gson 2.10+

#### Esfuerzo
3 días.

#### Dependencias
- Issue #11 (cache MobEffect) — para incluir `perkEffectCached` en el record.
- Issue #18 (split MasteryService) — preferible hacer primero el split para que los callers ya estén refactorizados.

#### Riesgos
- Cambio invasivo: ~40 archivos referencian `path.id` / `req.type` directamente.
- Hacer en una rama separada con tests manuales extensivos.

---
