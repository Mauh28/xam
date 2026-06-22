name: xd-absolute-mastery
description: >
  Genera e implementa la lógica del mod xd Absolute Mastery (XAM) para Minecraft Forge 1.20.1. 
  Incluye el sistema de caminos/ramas, restricciones de equipamiento por elección, 
  maestría acumulativa (las ramas completadas no se penalizan) e integración con Tinkers' Construct. 
  Activa esta skill cuando se pida generar código para el mod XAM, sistemas de clases, 
  restricciones de items o lógica de progresión.
---

# Instrucciones para el Agente: xd Absolute Mastery (XAM)

Eres un desarrollador experto de mods para Minecraft Forge 1.20.1. Tu objetivo es escribir el código para el mod **xd Absolute Mastery (XAM)**. 

Debes seguir estrictamente las siguientes reglas de diseño y arquitectura. Todo el código generado debe usar el Mod ID: `xam`.

## 1. Reglas Fundamentales del Mod

- El jugador debe elegir un "Camino" (Rama) al empezar (ej. `botania`, `mekanism`).
- Mientras una rama está activa, el jugador NO puede usar con efecto armaduras/armas de otras ramas no dominadas.
- **Excepciones (Universales):** Los items de Minecraft Vanilla y TODOS los items de Tinkers' Construct son de uso libre siempre.
- **Maestría Acumulativa:** Si el jugador completa los logros de maestría de su rama actual, esta se marca como "Dominada". El jugador puede elegir una nueva rama y conservar los permisos de las ramas que ya dominó. Las ramas dominadas nunca son penalizadas.

## 2. Penalizaciones por Incompatibilidad

Si un jugador usa un item que no pertenece a su rama actual, ni a una rama dominada, ni es universal:

- **Armaduras:** Se pueden equipar, pero OTORGAN 0 de defensa y 0 de dureza (usar Attribute Modifiers). Enviar mensaje de advertencia: *"Tu maestría rechaza esta armadura, no te protegerá"*.
- **Armas/Herramientas:** Se pueden sostener, pero:
  - Hacen exactamente **1.0 de daño** (golpe de mano vacía).
  - Mantienen su **Cooldown original** (no se puede hacer spam de clics).
  - Tienen **velocidad de minería 0** (no pican, talan ni excavan).
  - **NO consumen durabilidad** al usarse de forma ineficaz.
  - Enviar mensaje de advertencia: *"Esta arma no tiene efecto bajo tu maestría"*.
- Los mensajes de advertencia en el chat deben tener un cooldown interno de 3 segundos para no spamear.

## 3. Arquitectura de Datos y Configuración

### 3.1. Datos del Jugador (Capabilities / Attachments NBT)
Guarda estos datos ligados al jugador:
- `currentPath` (String, nullable): La rama activa actual (ej. `"cataclysm"`). Si es null, se debe abrir la GUI de selección.
- `masteredPaths` (List<String>): Lista de ramas que el jugador ya ha completado.

### 3.2. Validación Central (Pseudocódigo a implementar en Java)
```java
boolean isItemValid(ItemStack stack, PlayerData data) {
    if (isUniversal(stack)) return true;
    String itemPath = getPathFromItemTags(stack); 
    if (itemPath == null) return false;
    if (itemPath.equals(data.currentPath)) return true;
    if (data.masteredPaths.contains(itemPath)) return true;
    return false;
}

boolean isUniversal(ItemStack stack) {
    // Comprobar si es de Vanilla (namespace "minecraft")
    // Comprobar si es instancia de Tinkers' Construct (ej: item instanceof ModifiableItem)
    // Comprobar si está en los tags xam:universal/*
}
```

### 3.3. Sistema de Tags de Forge
Usa el namespace `xam` para todos los tags. Los packmakers añadirán items aquí mediante datapacks/KubeJS:
- `xam:<rama_id>/armor`
- `xam:<rama_id>/weapons`
- `xam:<rama_id>/tools`
- `xam:universal/armor`, `xam:universal/weapons`, `xam:universal/tools`

### 3.4. Archivo de Configuración (`config/xam_paths.json`)
El mod debe leer este JSON para definir las ramas y los logros (advancements) necesarios para dominarlas:
```json
{
  "paths": [
    {
      "id": "botania",
      "name": "El Camino de la Naturaleza",
      "mastery_advancements": ["botania:main/rune_pickup", "botania:main/elf_portal_open"]
    },
    {
      "id": "mekanism",
      "name": "El Camino Tecnológico",
      "mastery_advancements": ["mekanism:achievement/elite", "mekanism:achievement/master"]
    }
  ]
}
```

## 4. Eventos de Forge a Implementar

1. **Progresión (Maestría Acumulativa):** Escuchar `AdvancementEvent`. Si el jugador completa los `mastery_advancements` de su `currentPath`, mover esta rama a `masteredPaths`, poner `currentPath = null` y abrir la GUI para elegir una nueva rama.
2. **Restricción de Armaduras:** Usar `LivingEquipmentChangeEvent`. Si el item equipado no es válido según `isItemValid`, aplicar modificadores de atributo que anulen la armadura (GENERIC_ARMOR y GENERIC_ARMOR_TOUGHNESS a 0) y enviar advertencia.
3. **Restricción de Daño (Armas):** Usar `LivingHurtEvent`. Si el arma en la mano principal no es válida, sobrescribir el daño con `event.setAmount(1.0f)`.
4. **Restricción de Minería (Herramientas):** Usar `PlayerEvent.BreakSpeed` (setNewSpeed a 0.0f) y `BlockEvent.BreakEvent` (cancelar evento si la herramienta no es válida). Asegurarse de no desgastar la durabilidad.

## 5. Flujo de Trabajo al Generar Código

Cuando se te pida generar código para este mod, sigue este orden lógico si no se especifica lo contrario:
1. Generar la clase principal `XdAbsoluteMastery` y el registro de eventos del mod (Mod ID: xam).
2. Generar la Capability/Attachment para guardar `currentPath` y `masteredPaths` en el jugador.
3. Generar el parser de `config/xam_paths.json`.
4. Generar la lógica central de validación `isItemValid` y `isUniversal` (con integración de Tinkers' Construct mediante instancia de clase).
5. Generar los EventHandlers para penalizaciones (Armadura 0 defensa, Armas 1 daño, Minado 0 velocidad).
6. Generar el EventHandler de `AdvancementEvent` para la lógica de Maestría Acumulativa.
7. Generar las interfaces gráficas (GUI) de selección de camino.
```