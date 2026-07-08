#### Resumen
`ConfigManager.PATHS_MAP` (HashMap) existe para lookup O(1) por id, pero el código itera la lista `PATHS` en 6+ lugares. Algunos de esos lugares corren cada frame de render.

#### Problema
Lugares detectados con linear scan donde existe O(1):

| Archivo:line | Contexto | Frecuencia |
|--------------|----------|------------|
| `MasteryHubScreen.java:93-98` | Buscar activePath | Cada frame de render |
| `MasteryHubScreen.java:553-576` | Buscar path por id en click | Cada click |
| `MasteryHubScreen.java:697-702` | Buscar path en keypress | Cada keypress |
| `XamCommand.java:279-284` | Validar que pathId existe | Cada comando |
| `MasteryService.java:41-47` | Lookup de path | Varias llamadas por tick |
| `MasteryService.java:119-124` | Lookup de path | Varias llamadas por tick |
| `MasteryService.java:250-254` | Lookup de path | Varias llamadas por tick |

Ejemplo del patrón repetido:
```java
// MasteryHubScreen.java:93-98 — corre cada frame
if (activePathId != null) {
    for (PathInfo p : ConfigManager.PATHS) {
        if (p.id.equals(activePathId)) {
            activePath = p;
            break;
        }
    }
}
```

#### Fix propuesto
Reemplazar cada loop con lookup O(1):

```java
// Antes:
for (PathInfo p : ConfigManager.PATHS) {
    if (p.id.equals(activePathId)) {
        activePath = p;
        break;
    }
}

// Después:
PathInfo activePath = activePathId != null ? ConfigManager.PATHS_MAP.get(activePathId) : null;
```

Y para validación de existencia en `XamCommand.java:279-284`:
```java
// Antes:
for (PathInfo p : ConfigManager.PATHS) {
    if (p.id.equals(pathId)) { ... }
}

// Después:
if (ConfigManager.PATHS_MAP.containsKey(pathId)) { ... }
```

#### Esfuerzo
30 minutos (editar 6-7 lugares + verificar compile).

#### Dependencias
Ninguna.

#### Bonus
`MasteryService.areRequirementDependenciesMet` (`MasteryService.java:230-236`) hace un **nested linear scan** (O(n²) sobre `path.requirements`). Considerar construir un `Map<String, Requirement>` en parse time. Esto es un issue separado.

---