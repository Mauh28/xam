# XAM (xd Absolute Mastery)


![XAM Logo](src/main/resources/xam.png)


**XAM** es un mod de progresión y maestría para Minecraft Forge 1.20.1 que revoluciona la forma en que los jugadores interactúan con los mods en un modpack. En lugar de tener acceso a todo desde el principio, XAM divide el contenido en "Rutas" o "Maestrías". El jugador debe elegir una ruta, completar requisitos específicos (logros, crafteos, matar jefes) y solo entonces desbloquear el uso de los items, herramientas y armaduras de esa categoría.

¿Quieres usar el poder de *Mekanism*? Primero deberás dominar "El Camino Tecnológico". ¿Prefieres *Botania*? Tendrás que avanzar en "El Camino de la Naturaleza". 

Este mod está diseñado para dar un sentido de progresión, desafío y rejugabilidad a los modpacks.

---

## ✨ Características Principales

* **Sistema de Rutas Dinámico:** Define rutas basadas en mods, avances de Minecraft, o categorías custom.
* **Bloqueo de Items Inteligente:** Los jugadores no pueden usar items, armas, herramientas o armaduras de una ruta que no hayan empezado o dominado.
* **Requisitos Variados:** Completa logros (advancements), craftea items específicos, recolecta recursos o derrota enemigos para progresar.
* **Integración con GUIs:** Pantallas intuitivas para seleccionar rutas, ver progreso y editar configuraciones en juego.
* **Soporte para Multiplayer:** Sincronización completa entre cliente y servidor, con protección anti-exploits.
* **Compatibilidad Extendida:** Integración nativa con JEI, Jade, Curios y Tinkers' Construct.

---

## 🛠️ Instalación

1. Asegúrate de tener instalado **Minecraft Forge 1.20.1** (recomendado versión 47.4.0 o superior).
2. Descarga el archivo `.jar` de XAM desde la página de [Releases](https://github.com/Mauh28/xam/releases).
3. Coloca el archivo `.jar` en la carpeta `mods` de tu directorio de Minecraft.
4. ¡Disfruta!

---

## 🎮 Cómo Jugar (Mecánicas)

Al entrar al mundo por primera vez, el jugador no podrá usar items de mods complejos. 

1. **Abrir el Hub de Maestría:** Presiona la tecla **`M`** (configurable) o usa el botón en la esquina superior izquierda del inventario.
2. **Elegir una Ruta:** Selecciona la maestría que quieres iniciar. No podrás cambiar de ruta hasta cumplir un mínimo de requisitos o dominarla por completo.
3. **Progresar:** Abre el Hub para ver qué tareas debes completar (ej. "Recoge una runa", "Craftea un circuito básico").
4. **Dominar:** Al completar todos los requisitos, la ruta se domina, otorgando permisos permanentes para usar sus items y posiblemente un *Perk* (efecto de poción) de recompensa.

---

## ⚙️ Configuración para Modpacks

XAM está pensado para ser altamente configurable mediante el archivo `xam_paths.json` que se genera en la carpeta `config/` al ejecutar el mod por primera vez.

Allí puedes definir:
* Nuevas rutas y sus respectivos IDs de mods.
* Requisitos personalizados (tipo: `advancement`, `craft`, `kill`, `collect`).
* Dependencias entre rutas (ej. debes dominar el camino mágico para desbloquear el camino tecnológico).
* Porcentajes mínimos para poder abandonar o cambiar de ruta.
* Namespaces universales (items que se pueden usar libremente sin restricciones).

### Comandos Administrativos

El mod incluye el comando `/xam` para gestionar a los jugadores (requiere permisos OP):

| Comando | Descripción |
| :--- | :--- |
| `/xam info [player]` | Muestra el estado actual de maestría del jugador. |
| `/xam progress [player]` | Muestra el progreso detallado de la ruta actual. |
| `/xam select <path_id> [player]` | Fuerza a un jugador a seleccionar una ruta. |
| `/xam master <path_id> [player]` | Marca una ruta como dominada para un jugador. |
| `/xam complete_req <req_id> [player]` | Completa automáticamente un requisito para un jugador. |
| `/xam reset <player>` | Borra todo el progreso del jugador. |
| `/xam dev` | Activa/desactiva el Modo Dev (ignora restricciones de items). |
| `/xam reload` | Recarga la configuración `xam_paths.json` sin reiniciar el servidor. |

---

## 🤝 Integraciones y Compatibilidades

XAM funciona de forma autónoma, pero brilla cuando se combina con otros mods:

* **Just Enough Items (JEI):** Muestra un candado visual en los items bloqueados para que el jugador sepa qué falta por desbloquear.
* **Jade:** Muestra información en el HUD sobre si un bloque o entidad está bloqueado.
* **Curios API:** Expulsa automáticamente los items de Curios si el jugador pierde acceso a una ruta.
* **Tinkers' Construct:** Reconoce herramientas modificables de Tinkers y aplica las restricciones correctamente.

---

## 👨‍💻 Para Desarrolladores (Compilar desde el código)

Si deseas contribuir o compilar el mod desde la fuente:

**Requisitos:**
* Java Development Kit (JDK) 17 o 21
* Git

**Compilación:**
```bash
git clone https://github.com/Mauh28/xam.git
cd xam
./gradlew build
```
El archivo `.jar` compilado se encontrará en `build/libs/`.

---

## 📝 Licencia y Créditos

* **Autores:** xd Team, Mauh28
* **Licencia:** Este proyecto está licenciado bajo la **Licencia Pública General Reducida de GNU (LGPL-3.0)**. 

Esto significa que:
* Puedes descargar, usar y modificar este mod.
* Otros desarrolladores pueden usar este mod como dependencia para sus propios mods (incluso mods de código cerrado).
* Si modificas el código fuente de XAM y lo redistribuyes, debes liberar esas modificaciones bajo la misma licencia LGPL-3.0.

Consulta el archivo `LICENSE` en la raíz de este repositorio para obtener el texto legal completo.
```