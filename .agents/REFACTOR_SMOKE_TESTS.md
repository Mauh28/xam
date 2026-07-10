# Smoke Tests — Refactor Issue #17

Marcar ✅/❌ después de cada paso del refactor.

---

### Test #1 — Diálogo de confirmación de borrado
1. Abrir Editor de Maestrías.
2. Click derecho en una rama en el sidebar.
3. Click en "Eliminar rama".
4. **Esperado:** Aparece `ConfirmDeleteScreen` con nombre de la rama.
5. Click "Cancelar".
6. **Esperado:** Vuelve al editor sin borrar nada.
7. Repetir 1-4, click "Confirmar".
8. **Esperado:** La rama desaparece del sidebar.

### Test #2 — Picker de eliminación
1. En el editor, click en el botón "Eliminar rama" (botón de toolbar).
2. **Esperado:** Aparece `DeleteMasteryScreen` con grid de ramas.
3. Click en una rama.
4. **Esperado:** Aparece `ConfirmDeleteScreen`.
5. Confirmar.
6. **Esperado:** Vuelve al editor y la rama no está.
7. Scroll del grid (rueda del mouse) — debe funcionar.
8. Drag del scrollbar — debe funcionar.

### Test #3 — Resize de pantalla
1. Abrir editor en ventana normal (≥ 450px container).
2. **Esperado:** Layout horizontal: icon | title | mod_id | browse | deps | min en dos filas.
3. Redimensionar la ventana de Minecraft a un ancho muy pequeño (< 320px editor width).
4. **Esperado:** Layout cambia a stacked (cada campo en su propia fila).
5. Volver a tamaño grande.
6. **Esperado:** Layout vuelve a horizontal.

### Test #4 — Modo narrow (regresión crítica)
1. Abrir editor en modo narrow (ventana pequeña).
2. Verificar que:
   - Icon visible.
   - EditBox de título ocupa todo el ancho disponible.
   - EditBox de mod_id en fila 2.
   - EditBox de deps en fila 3.
   - EditBox de min en fila 4.
3. Editar todos los campos — deben guardar valor al hacer blur.

### Test #5 — Scroll de lista de requisitos
1. Seleccionar una rama con 5+ requisitos.
2. Hacer scroll con rueda del mouse sobre la lista de requisitos.
3. **Esperado:** La lista scrollea, los items entran/salen de vista.
4. Si hay scrollbar visible, hacer drag.
5. **Esperado:** Scroll sigue al drag.

### Test #6 — Guardar config válida
1. Abrir editor con config existente (sin cambios).
2. Click "Guardar".
3. **Esperado:** Aparece notificación verde "✔ Configuración guardada" durante 3s.
4. Verificar que el archivo `config/xam_paths.json` se actualizó (timestamp).

### Test #7 — Guardar con errores (validación)
1. Seleccionar una rama.
2. Vaciar el campo "Nombre" (EditBox de título).
3. Click "Guardar".
4. **Esperado:** Aparece notificación roja con mensaje de error específico (ej: "La rama 1 no tiene nombre").
5. **Esperado:** No se envía packet al server. El archivo en disco no se modifica.

### Test #8 — Guardar rama con requisito inválido
1. Seleccionar una rama.
2. Añadir un requisito nuevo.
3. Vaciar el campo ID del requisito.
4. Click "Guardar".
5. **Esperado:** Notificación roja: "La tarea 1 de <rama> no tiene ID".
6. Repetir con nombre vacío, descripción vacía.
7. **Esperado:** Cada caso da su mensaje específico.

### Test #9 — Regresión total (ejecutar después de PASO 6 y PASO 7)
Ejecutar todos los tests #1–#8 en orden. Todos deben pasar.
