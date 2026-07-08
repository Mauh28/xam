#### Resumen
La lógica de fallback de íconos para paths (botania → poppy, mekanism → redstone, default → writable_book) está duplicada en 4 lugares.

#### Problema
**Lugares con la misma lógica de fallback:**

1. `ConfigManager.java:103-114` (durante `parseJson`)
2. `ClientPacketHandler.java:93-99` (en `handleSync` para toast)
3. `MasteryHubScreen.java:116-118` (icono de path activo)
4. `MasteryHubScreen.java:563-571` (método `getPathIcon`)

```java
// Mismo código, 4 copias:
if (pathId.equals("botania")) {
    stack = new ItemStack(Items.POPPY);
} else if (pathId.equals("mekanism")) {
    stack = new ItemStack(Items.REDSTONE);
} else {
    stack = new ItemStack(Items.WRITABLE_BOOK);
}
```

#### Problemas
1. Si se agrega una nueva path default, hay que tocar 4 lugares
2. `botania` y `mekanism` son nombres de mods hardcoded — knowledge específico de mods leakando en un mod genérico
3. Las 4 copias pueden divergir silenciosamente

#### Fix propuesto
Crear clase centralizada:

```java
// Nuevo archivo: src/main/java/org/xam/util/PathIcons.java
package org.xam.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PathIcons {
    private PathIcons() {}

    public static ItemStack getDefaultIcon(String pathId) {
        if (pathId == null) return new ItemStack(Items.WRITABLE_BOOK);
        return switch (pathId) {
            case "botania"   -> new ItemStack(Items.POPPY);
            case "mekanism"  -> new ItemStack(Items.REDSTONE);
            default          -> new ItemStack(Items.WRITABLE_BOOK);
        };
    }
}
```

Reemplazar las 4 copias con `PathIcons.getDefaultIcon(pathId)`.

#### Recomendación adicional
Considerar **eliminar los casos especiales** de botania/mekanism y siempre usar `WRITABLE_BOOK`. El mod no debería tener conocimiento específico de otros mods. Si el usuario quiere íconos custom, los setea en `xam_paths.json`.

#### Esfuerzo
2 horas.

#### Dependencias
Ninguna.

---