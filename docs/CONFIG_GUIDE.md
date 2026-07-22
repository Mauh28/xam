# 📘 Guía Completa de Configuración de XAM (xd Absolute Mastery)

Esta guía documenta la estructura técnica completa del archivo de configuración `xam_paths.json` generado en la carpeta `config/` del servidor/cliente.

---

## 📑 Estructura General del Archivo `xam_paths.json`

El archivo de configuración utiliza formato JSON estándar y se divide en dos secciones principales:

```json
{
  "universal_namespaces": [
    "minecraft",
    "tconstruct"
  ],
  "paths": [
    { ... }
  ]
}
```

---

## 🌐 1. Namespaces Universales (`universal_namespaces`)

Lista de identificadores de mod (*modid* / *namespaces*) cuyos objetos, armas y herramientas son **universales**. Cualquier jugador puede utilizarlos sin importar qué maestría tenga equipada.

```json
"universal_namespaces": [
  "minecraft",
  "tconstruct"
]
```

> 💡 **Nota:** `minecraft` y `tconstruct` se recomiendan como universales para permitir el inicio vanilla y herramientas básicas.

---

## 📜 2. Definición de Ramas de Maestría (`paths`)

Cada elemento en el arreglo `paths` define una rama de maestría completa.

### Campos de una Rama:

| Campo | Tipo | Descripción | Ejemplo |
| :--- | :--- | :--- | :--- |
| `id` | String | Identificador único de la rama. | `"botania"` |
| `name` | String | Título visible en el GUI o clave de traducción. | `"El Camino de la Naturaleza"` |
| `mod_id` | String | Namespace del mod asociado. Los objetos de este modid pertenecerán a esta rama. | `"botania"` |
| `icon` | String | ResourceLocation del objeto usado como icono de la rama. | `"botania:lexicon"` |
| `min_to_switch` | Integer | Número mínimo de misiones a completar antes de poder cambiar a otra rama. Usar `-1` para exigir 100% (Dominio completo). | `0` o `2` o `-1` |
| `perk_effect` | String | ResourceLocation del efecto de poción otorgado permanentemente al dominar la rama (100%). | `"minecraft:speed"` |
| `perk_amplifier` | Integer | Nivel/Intensidad del efecto (`0` = Nivel I, `1` = Nivel II). | `0` |
| `dependencies` | Array | Lista de dependencias requeridas para desbloquear esta rama. | `["magic_tier1:50%"]` |
| `requirements` | Array | Lista de tareas/misiones de la rama. | `[...]` |

---

## 🎯 3. Formato de Requisitos (`requirements`)

Cada tarea dentro de `requirements` define una misión individual.

```json
{
  "type": "craft",
  "id": "mekanism:basic_control_circuit",
  "name": "Circuito de Control Básico",
  "description": "Fabrica un Circuito de Control Básico de Mekanism",
  "effect": "",
  "dependencies": []
}
```

### Tipos de Requisitos Disponibles (`type`):

1. **`craft`**: Se completa automáticamente cuando el jugador craftea el objeto en una mesa de crafteo o inventario.
2. **`collect`**: Se completa automáticamente cuando el jugador obtiene y tiene el objeto en su inventario.
3. **`kill`**: Se completa cuando el jugador elimina a una entidad o jefe.
   - *Campo opcional `effect`*: Exige que la entidad eliminada o el jugador tengan un efecto activo. Ejemplo: `"minecraft:strength 1"` exige nivel de Fuerza I o superior.
4. **`advancement`**: Se completa cuando el jugador obtiene el logro especificado en `id` (ejemplo: `"botania:main/rune_pickup"`).

---

## 🔗 4. Sintaxis de Dependencias entre Ramas (`dependencies`)

Las dependencias determinan qué ramas deben progresarse antes de poder elegir una rama avanzada:

- **`"rama_id:count"`**: Exige haber completado un número exacto de requisitos de esa rama. Ejemplo: `"botania:2"`.
- **`"rama_id:porcentaje%"`**: Exige un porcentaje de avance. Ejemplo: `"magic_tier1:50%"`.
- **`"rama_id:mastered"`**: Exige haber dominado la rama al 100%. Ejemplo: `"tech_tier1:mastered"`.

---

## 🛠️ 5. Comandos Administrativos (`/xam`)

Los administradores pueden gestionar y probar la configuración en tiempo real (requiere permiso OP nivel 2):

| Comando | Descripción |
| :--- | :--- |
| `/xam info [player]` | Muestra la información de maestría activa, ramas dominadas y modo dev del jugador. |
| `/xam progress [player]` | Muestra el progreso de requisitos de la maestría activa del jugador. |
| `/xam started [player]` | Muestra las ramas iniciadas actualmente en progreso por el jugador. |
| `/xam help` | Muestra el menú de ayuda interactivo con la lista de comandos. |
| `/xam select <path_id> [player]` | Selecciona o asigna una rama activa a un jugador. |
| `/xam master <path_id> [true/false] [player]` | Domina o desmarca una rama de maestría para un jugador (activa efectos 3D). |
| `/xam complete_req <req_id> [player]` | Completa manualmente un requisito específico para un jugador. |
| `/xam revert_req <req_id> [player]` | Revierte el progreso de un requisito específico para un jugador. |
| `/xam check_item` | Analiza el objeto en mano mostrando sus etiquetas, namespace y estado de acceso. |
| `/xam master_all [player]` | Domina instantáneamente todas las ramas de maestría para un jugador. |
| `/xam deletepath <path_id>` | Elimina una rama de maestría completamente de la configuración del servidor. |
| `/xam reset <player>` | Restablece y borra todo el progreso de maestría de un jugador. |
| `/xam dev` | Activa o desactiva el Modo Desarrollador (ignora restricciones de objetos). |
| `/xam reload` | Recarga la configuración `xam_paths.json` desde disco sin reiniciar el servidor. |
