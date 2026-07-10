> **ESTADO:** ✅ Resuelto en commits `5a89d1c`..`f7c7f31`.
> Ver `REFACTOR_SMOKE_TESTS.md` para el checklist de regresión.

#### Resumen
`MasteryEditorScreen` es una god class que mezcla layout, rendering, input dispatch, context menus, list management, validación, serialización JSON y navegación de pantallas. El método `init()` maneja 12+ instance fields de coordenadas pixel. Una nested class `ConfirmDeleteScreen` debería ser su propio archivo.

#### Problema
**Archivo:** `src/main/java/org/xam/client/gui/MasteryEditorScreen.java` (1,518 líneas)

**Responsabilidades mezcladas:**
1. Layout (12+ campos de coordenadas: `iconX, iconY, titleX, titleY, titleW, modX, modY, modEditW, browseX, depsX, depsW, depsBtnX, minX, minW, minY, metadataFrameH, reqTitleY` — `MasteryEditorScreen.java:62-72`)
2. Rendering (`render()` cientos de líneas)
3. Input dispatch (`mouseClicked` ~200 líneas)
4. Context menus
5. List management (paths + requirements)
6. Validación (`saveConfig`, `MasteryEditorScreen.java:1102-1136`)
7. JSON serialization
8. Screen navigation
9. Nested `ConfirmDeleteScreen` (`MasteryEditorScreen.java:1145-1518`, 373 líneas)

#### Fix propuesto
Extraer 4 colaboradores:

1. **`MasteryEditorLayout`** — data class con todas las coordenadas pixel, computadas una vez en `init()` basadas en `width/height/isNarrow`:

```java
// Nuevo archivo: src/main/java/org/xam/client/gui/MasteryEditorLayout.java
public final class MasteryEditorLayout {
    public final int iconX, iconY, iconSize;
    public final int titleX, titleY, titleW;
    public final int modX, modY, modEditW, browseX;
    public final int depsX, depsW, depsBtnX;
    public final int minX, minW, minY;
    public final int metadataFrameH;
    public final int reqTitleY, reqListY, reqListH;

    public MasteryEditorLayout(int screenWidth, int screenHeight, boolean isNarrow) {
        // Toda la matemática de coordenadas aquí, una sola vez
    }
}
```

2. **`MasteryEditorModel`** — state de la pantalla (lista local de paths, paths eliminados, dirty flag, etc.) con mutadores explícitos.

3. **`MasteryEditorValidator`** — validación de campos (id no vacío y válido, mod_id no vacío, requirements válidos):

```java
public final class MasteryEditorValidator {
    public ValidationResult validate(PathInfo path) { ... }
    public ValidationResult validate(Requirement req) { ... }
    public ValidationResult validateAll(List<PathInfo> paths) { ... }
}
```

4. **`ConfirmDeleteScreen`** como archivo separado en `src/main/java/org/xam/client/gui/ConfirmDeleteScreen.java`.

`MasteryEditorScreen` queda solo como orquestador: delega a los 4 colaboradores.

#### Esfuerzo
1-2 semanas (es el refactor más grande del plan, pero el de mayor ROI a mediano plazo).

#### Dependencias
- Issue #9 (MasteryGuard) — para mantener consistencia en el patrón de extracción.
- Issue #10 (PathIcons) — para eliminar dependencia de MasteryEditorScreen en lógica de icons.

#### Riesgos
- Alto riesgo de regression visual. Hacer commits incrementales con tests manuales en cada paso.
- Considerar snapshot tests del issue #8.4 del PDF antes/después para detectar cambios visuales.

---