#### Resumen
`ClientForgeEvents.isInteractionKey` usa una blacklist de 13 teclas, pero `keySwapOffhand` (tecla F) no está en la lista. Un jugador con item inválido en mano principal puede presionar F para swappearlo a la offhand, dejando la mano principal libre.

#### Problema
**Archivo:** `src/main/java/org/xam/client/event/ClientForgeEvents.java:176-191`

```java
private static boolean isInteractionKey(KeyMapping key) {
    return key != mc.options.keyUse
        && key != mc.options.keyAttack
        && key != mc.options.keyPickItem
        && key != mc.options.keyDrop
        && key != mc.options.keyInventory
        && key != mc.options.keyShift
        // ... 13 teclas total
        && key != mc.options.keySwapHands;  // ← FALTA esto
}
```

Cualquier tecla no listada se permite. Si Minecraft agrega una nueva key binding (o un mod la agrega), el bypass se introduce silenciosamente.

#### Impacto
- Bypass confirmado: `keySwapOffhand` no está en la lista → jugador puede swappear item inválido a offhand
- Futuras keys de Minecraft o mods también serán bypassed por diseño

#### Fix propuesto
Cambiar a **whitelist**: permitir solo movement + chat + inventory + escape. Cualquier otra tecla se suprime cuando el jugador sostiene un item inválido.

```java
// ClientForgeEvents.java
private static final Set<KeyMapping> ALLOWED_KEYS = Set.of(
    // Movement
    mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
    mc.options.keyJump, mc.options.keyShift, mc.options.keySprint,
    // UI
    mc.options.keyInventory, mc.options.keyChat, mc.options.keyCommand,
    // System
    mc.options.keyPause
);

private static boolean isInteractionKey(KeyMapping key) {
    return !ALLOWED_KEYS.contains(key);
}
```

Nota: el `Set.of(...)` no puede referenciar campos de instancia durante inicialización estática. Hay que construirlo en `onClientTick` la primera vez o usar un `HashSet<KeyMapping>` lazy-initialized.

#### Esfuerzo
2 horas (diseñar whitelist + inicialización lazy + test manual en juego).

#### Dependencias
Ninguna.

#### Cómo verificar
1. Equipar item inválido en mano principal
2. Intentar todas las teclas: F (swap), Q (drop), click izquierdo/derecho, etc.
3. Todas deben suprimirse excepto movimiento y chat

---