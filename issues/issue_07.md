#### Resumen
`xam.mixins.json` declara `compatibilityLevel: JAVA_8` pero el toolchain del proyecto es Java 17 (`build.gradle:28`). Esto confunde a la toolchain de MixinGradle.

#### Problema
**Archivo:** `src/main/resources/xam.mixins.json`

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "org.mixin",
  "compatibilityLevel": "JAVA_8",   // ← proyecto es Java 17
  "refmap": "xam.refmap.json",
  "mixins": [],
  "client": []
}
```

Adicionalmente, el `"package": "org.mixin"` no existe en el proyecto (debería ser `org.xam.mixin` si se llegara a usar).

#### Contexto
No existe ninguna clase `@Mixin` en el codebase (grep confirma). Toda la infraestructura de Mixins (plugin en `build.gradle:18`, refmap config en `build.gradle:124-128`, annotation processor en `build.gradle:189`) es **boilerplate sin uso**.

#### Fix propuesto (2 opciones)

**Opción A — Eliminar todo (recomendada si no hay plans de mixins a corto plazo):**
- Borrar `src/main/resources/xam.mixins.json`
- En `build.gradle`: quitar `apply plugin: 'org.spongepowered.mixin'`
- En `build.gradle`: borrar el bloque `mixin { ... }`
- En `build.gradle`: borrar la línea `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'`
- En `build.gradle`: quitar el classpath `'org.spongepowered:mixingradle:0.7-SNAPSHOT'` del buildscript

**Opción B — Arreglar (si hay plans de usar mixins):**
```json
{
  "required": true,
  "minVersion": "0.8",
- "package": "org.mixin",
+ "package": "org.xam.mixin",
- "compatibilityLevel": "JAVA_8",
+ "compatibilityLevel": "JAVA_17",
  "refmap": "xam.refmap.json",
  "mixins": [],
  "client": []
}
```

#### Esfuerzo
- Opción A: 2 horas (verificar que el build sigue funcionando sin el plugin)
- Opción B: 1 minuto

#### Dependencias
Ninguna. Decidir A vs B es una decisión de diseño: ¿vas a usar mixins en el futuro cercano?

---