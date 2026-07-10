> **ESTADO:** ✅ Resuelto en commit `5d83885`.

#### Resumen
El generador de IDs para nuevas paths usa `System.currentTimeMillis() % 1000`, que cicla cada segundo. Dos OPs creando paths dentro de la misma ventana de 1 segundo colisionan y `PATHS_MAP.put` sobreescribe silenciosamente.

#### Problema
**Archivo:** `src/main/java/org/xam/client/gui/MasteryEditorScreen.java:962-963`

```java
PathInfo p = new PathInfo();
p.id = "nueva_rama_" + (System.currentTimeMillis() % 1000);
```

#### Problemas adicionales
1. `currentTimeMillis() % 1000` cicla cada 1000ms → colisión casi garantizada si dos OPs clickean "nueva rama" en el mismo segundo
2. El prefijo `"nueva_rama_"` está hardcoded en español (debería ser un ID neutral, no localizado)

#### Fix propuesto
```java
// MasteryEditorScreen.java:962-963
PathInfo p = new PathInfo();
- p.id = "nueva_rama_" + (System.currentTimeMillis() % 1000);
+ p.id = "path_" + java.util.UUID.randomUUID().toString().substring(0, 8);
```

#### Esfuerzo
5 minutos.

#### Dependencias
Ninguna.

#### Cómo verificar
1. Abrir el editor de maestrías (`/xam editor` o similar)
2. Crear 2 paths nuevas en rápida sucesión
3. Guardar y recargar
4. Ambas paths deben existir (BUG actual: solo la última sobrevive)

---

## TIER B — TAREAS MEDIANAS (1-2 días cada uno)

Estos 8 issues requieren algo de diseño o refactor estructural pero no rompen la arquitectura existente.

---