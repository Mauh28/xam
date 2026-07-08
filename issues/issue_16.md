#### Resumen
`UpdateConfigPacket` valida que cada path tenga `id/name/mod_id` no vacíos, pero no valida que sean `ResourceLocation` válidos, ni que `req.type` sea uno de los tipos conocidos, ni que `perkEffect` sea un efecto registrado.

#### Problema
**Archivo:** `src/main/java/org/xam/network/UpdateConfigPacket.java:32-65`

Validación actual (shallow):
```java
JsonObject json = new Gson().fromJson(pkt.json, JsonObject.class);
if (json != null && json.has("paths")) {
    JsonArray paths = json.getAsJsonArray("paths");
    for (int i = 0; i < paths.size(); i++) {
        JsonObject p = paths.get(i).getAsJsonObject();
        String id = p.has("id") ? p.get("id").getAsString() : "";
        String name = p.has("name") ? p.get("name").getAsString() : "";
        String modId = p.has("mod_id") ? p.get("mod_id").getAsString() : "";
        if (id.trim().isEmpty() || name.trim().isEmpty() || modId.trim().isEmpty()) {
            player.sendSystemMessage(Component.literal("[XAM] Rama inválida: ..."));
            return;
        }
        // ... solo valida req.id/name presence
    }
}
```

**No valida:**
- `id` sea un `ResourceLocation` válido → `ResourceLocation.fromNamespaceAndPath("xam", id + "/armor")` en `ConfigManager.java:122` lanza `ResourceLocationException` con chars inválidos
- `req.type` ∈ {`craft`, `collect`, `kill`, `advancement`} → typo silenciosamente rompe el handler
- `path.perkEffect` sea un `MobEffect` registrado → perk inexistente silenciosamente no aplica
- `path.dependencies` matchee el formato `"pathId:count|pathId:%|pathId:mastered"` → dependencia malformada cae en `MasteryService.isDependencyMet` try/catch y retorna `data.getMasteredPaths().contains(depPathId)` silenciosamente (`MasteryService.java:185-187`)
- Mensajes de error son hardcoded Spanish (`"[XAM] Rama inválida: ..."`)

#### Fix propuesto

```java
// UpdateConfigPacket.java
private static final Set<String> KNOWN_REQ_TYPES = Set.of("craft", "collect", "kill", "advancement");

public static boolean validate(UpdateConfigPacket pkt, ServerPlayer player) {
    if (!player.hasPermissions(2)) return false;

    JsonObject json;
    try {
        json = new Gson().fromJson(pkt.json, JsonObject.class);
    } catch (Exception e) {
        player.sendSystemMessage(Component.translatable("xam.msg.config_invalid_json"));
        return false;
    }
    if (json == null || !json.has("paths")) {
        player.sendSystemMessage(Component.translatable("xam.msg.config_no_paths"));
        return false;
    }

    JsonArray paths = json.getAsJsonArray("paths");
    for (int i = 0; i < paths.size(); i++) {
        JsonObject p = paths.get(i).getAsJsonObject();

        // Validar id como ResourceLocation
        String id = p.has("id") ? p.get("id").getAsString() : "";
        if (id.trim().isEmpty() || ResourceLocation.tryParse("xam:" + id) == null) {
            player.sendSystemMessage(Component.translatable("xam.msg.config_invalid_id", id));
            return false;
        }

        String name = p.has("name") ? p.get("name").getAsString() : "";
        String modId = p.has("mod_id") ? p.get("mod_id").getAsString() : "";
        if (name.trim().isEmpty() || modId.trim().isEmpty()) {
            player.sendSystemMessage(Component.translatable("xam.msg.config_missing_field"));
            return false;
        }

        // Validar perkEffect
        if (p.has("perkEffect") && !p.get("perkEffect").isJsonNull()) {
            String perk = p.get("perkEffect").getAsString();
            if (!perk.isEmpty()) {
                ResourceLocation perkRl = ResourceLocation.tryParse(perk);
                if (perkRl == null || !ForgeRegistries.MOB_EFFECTS.containsKey(perkRl)) {
                    player.sendSystemMessage(Component.translatable("xam.msg.config_unknown_effect", perk));
                    return false;
                }
            }
        }

        // Validar requirements
        if (p.has("requirements")) {
            for (JsonElement reqEl : p.getAsJsonArray("requirements")) {
                JsonObject req = reqEl.getAsJsonObject();
                String reqType = req.has("type") ? req.get("type").getAsString() : "";
                if (!KNOWN_REQ_TYPES.contains(reqType)) {
                    player.sendSystemMessage(Component.translatable("xam.msg.config_unknown_req_type", reqType));
                    return false;
                }
            }
        }

        // Validar dependencies (formato pathId:count | pathId:% | pathId:mastered)
        if (p.has("dependencies")) {
            for (JsonElement depEl : p.getAsJsonArray("dependencies")) {
                String dep = depEl.getAsString();
                if (!dep.matches("^[a-z0-9_]+:(\\d+|%|mastered)$")) {
                    player.sendSystemMessage(Component.translatable("xam.msg.config_bad_dependency", dep));
                    return false;
                }
            }
        }
    }
    return true;
}
```

#### Esfuerzo
4 horas (incluye mover mensajes a lang keys, dependerá de si #15 ya está hecho).

#### Dependencias
- Issue #15 (i18n) para los mensajes de error.

---

## TIER C — REFACTORS GRANDES (> 1 semana)

Estos 6 issues son inversiones estructurales. ROI a mediano-largo plazo. Hacer después de completar Tier A y B.

---