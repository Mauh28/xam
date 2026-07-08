#### Resumen
`MasteryService` tiene 376 líneas y mezcla 6 responsabilidades: refresh de capability, validación de items, lookups de advancements, progresión de requisitos, resolución de dependencias y manipulación de atributos de armor.

#### Problema
**Archivo:** `src/main/java/org/xam/progression/MasteryService.java` (376 líneas)

**Responsabilidades mezcladas:**
1. Capability refresh (`checkAndRefreshPlayerData`)
2. Item validity (`isItemValid`, `mustSelectPath`)
3. Advancement lookup (`isAdvancementCompleted`)
4. Requirement progression (`checkAndProgressRequirement`, `checkPathCompletion`)
5. Dependency resolution (`isDependencyMet`, `areDependenciesMastered`, `areRequirementDependenciesMet`) — con parsing inline de `"pathId:count"` strings
6. Armor attribute manipulation (`updateArmorModifiers`, 42 líneas de attribute juggling)

#### Fix propuesto
Extraer 3 colaboradores:

1. **`ArmorModifierService`** — attribute juggling:
```java
public final class ArmorModifierService {
    public void updateModifiers(ServerPlayer player) { ... }  // ~42 líneas aquí
}
```

2. **`DependencyResolver`** con value object `DependencySpec`:
```java
public final class DependencySpec {
    public enum Kind { COUNT, PERCENT, MASTERED }
    public final String pathId;
    public final Kind kind;
    public final int value; // count o percent

    public static DependencySpec parse(String spec) {
        // Parse "pathId:count" | "pathId:%" | "pathId:mastered"
        // Una sola vez, en config-load time
    }
}

public final class DependencyResolver {
    public boolean isMet(PlayerData data, DependencySpec spec) { ... }
    public boolean areAllMet(PlayerData data, List<DependencySpec> specs) { ... }
}
```

3. **`ItemValidityService`** — lo que hoy es `isItemValid` + `mustSelectPath`:
```java
public final class ItemValidityService {
    public boolean isValid(ItemStack stack, PlayerData data) { ... }
    public boolean mustSelectPath(PlayerData data) { ... }
}
```

`MasteryService` queda como orquestador de progression (responsabilidad 4) + advancement lookup (3).

#### Esfuerzo
1 semana.

#### Dependencias
- Issue #9 (MasteryGuard) — para evitar doble refactor de callers.
- Issue #12 (namespaceToPath map) — para optimizar ItemValidityService desde el día 1.

---