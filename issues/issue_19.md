> **ESTADO:** ✅ Resuelto en commit `e943aae`.

#### Resumen
Si en el issue #7 se decidió **Opción A** (eliminar Mixins en vez de arreglar), este issue ejecuta la limpieza completa en `build.gradle`.

#### Problema
**Archivos afectados:**
- `src/main/resources/xam.mixins.json` (borrar)
- `build.gradle:7-9` — classpath `'org.spongepowered:mixingradle:0.7-SNAPSHOT'`
- `build.gradle:18` — `apply plugin: 'org.spongepowered.mixin'`
- `build.gradle:124-128` — bloque `mixin { ... }`
- `build.gradle:189` — `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'`

#### Fix propuesto
Aplicar cada una de las eliminaciones listadas arriba, luego correr `./gradlew clean build` para verificar que el build sigue funcionando.

Si en algún momento futuro se necesitan Mixins (por ejemplo para AT a campos privados de vanilla), se puede restaurar selectivamente siguiendo la documentación oficial de MixinGradle actualizada.

#### Esfuerzo
2 horas (verificar que el build sigue funcionando).

#### Dependencias
- Issue #7 (decisión A vs B).

---
