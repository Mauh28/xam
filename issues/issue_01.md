#### Resumen
El método `drawFlatPanel` en `AbstractMasteryScreen` usa `sr` (canal red) en lugar de `sb` (canal blue) al calcular el color de la sombra, produciendo bordes rojizos barrosos en vez de neutros en todos los paneles del mod.

#### Problema
**Archivo:** `src/main/java/org/xam/client/gui/AbstractMasteryScreen.java:150`

```java
int shadowColor = (alpha << 24) | (sr << 16) | (sg << 8) | sr; // ponytail: simple dark shade border fallback
//                                                       ^^ BUG: debería ser sb
```

El método hermano `drawGradientPanel` en la línea 182 tiene la fórmula correcta:
```java
int shadowColor = (alpha << 24) | (sr << 16) | (sg << 8) | sb; // ← correcto
```

#### Impacto
Todos los screens que usan `drawFlatPanel` (`MasteryHubScreen`, `MasteryEditorScreen`, `PathSelectionScreen`, etc.) renderizan bordes inferior/derecho con canal blue = canal red, lo que produce sombras rojizas en vez de grises neutros.

#### Fix propuesto
```java
// AbstractMasteryScreen.java:150
- int shadowColor = (alpha << 24) | (sr << 16) | (sg << 8) | sr;
+ int shadowColor = (alpha << 24) | (sr << 16) | (sg << 8) | sb;
```

#### Esfuerzo
1 minuto.

#### Dependencias
Ninguna.

#### Cómo verificar
Abrir cualquier screen del mod (ej.: presionar `M` en juego) y verificar que los bordes de los paneles se ven gris neutro, no rojizo.

---