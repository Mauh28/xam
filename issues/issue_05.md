#### Resumen
`ClientForgeEvents.addWidgetToScreen` usa reflexión para invocar `Screen.addRenderableWidget`, pero en Forge 1.20.1 ese método es público. Las ~30 líneas de reflexión + el `printStackTrace` del issue #4 son innecesarias.

#### Problema
**Archivo:** `src/main/java/org/xam/client/event/ClientForgeEvents.java:73-89`

```java
public static void addWidgetToScreen(net.minecraft.client.gui.screens.Screen screen, net.minecraft.client.gui.components.AbstractWidget widget) {
    try {
        java.lang.reflect.Method method = null;
        for (java.lang.reflect.Method m : net.minecraft.client.gui.screens.Screen.class.getDeclaredMethods()) {
            if (m.getName().equals("addRenderableWidget") || m.getName().equals("m_142416_")) {
                method = m;
                break;
            }
        }
        if (method != null) {
            method.setAccessible(true);
            method.invoke(screen, widget);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

Problemas concretos:
1. `Screen.addRenderableWidget(RenderableElement)` es público en 1.20.1
2. `getDeclaredMethods()` se llama en cada invocación
3. Los errores se swallow con `printStackTrace`
4. ~30 líneas de código para una sola llamada

#### Fix propuesto
Borrar el helper y llamar directo:

```java
// Antes (callers):
ClientForgeEvents.addWidgetToScreen(screen, widget);

// Después:
screen.addRenderableWidget(widget);
```

Eliminar el método `addWidgetToScreen` completo de `ClientForgeEvents`.

#### Esfuerzo
15 minutos (editar callers + borrar helper + compilar).

#### Dependencias
Ninguna.

#### Cómo verificar
Compilar (`./gradlew build`), abrir el inventario en juego y verificar que los widgets del mod (botón de maestría en esquina superior izquierda) siguen apareciendo.

---