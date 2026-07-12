package org.xam.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.config.ConfigManager;

import java.util.Set;
import java.util.function.Supplier;

public class UpdateConfigPacket {
    private final String json;

    public UpdateConfigPacket(String json) {
        this.json = json;
    }

    public static void encode(UpdateConfigPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.json);
    }

    public static UpdateConfigPacket decode(FriendlyByteBuf buf) {
        return new UpdateConfigPacket(buf.readUtf(32768));
    }

    private static final Set<String> KNOWN_REQ_TYPES = Set.of("craft", "collect", "kill", "advancement");

    public static boolean validate(UpdateConfigPacket pkt, ServerPlayer player) {
        if (!player.hasPermissions(2)) return false;

        JsonObject json;
        try {
            json = new Gson().fromJson(pkt.json, JsonObject.class);
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("xam.msg.config_invalid_json").withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }
        if (json == null || !json.has("paths")) {
            player.sendSystemMessage(Component.translatable("xam.msg.config_no_paths").withStyle(net.minecraft.ChatFormatting.RED));
            return false;
        }

        JsonArray paths = json.getAsJsonArray("paths");
        for (int i = 0; i < paths.size(); i++) {
            JsonObject p = paths.get(i).getAsJsonObject();

            // Validar id como ResourceLocation
            String id = p.has("id") ? p.get("id").getAsString() : "";
            if (id.trim().isEmpty() || ResourceLocation.tryParse("xam:" + id) == null) {
                player.sendSystemMessage(Component.translatable("xam.msg.config_invalid_id", id).withStyle(net.minecraft.ChatFormatting.RED));
                return false;
            }

            String name = p.has("name") ? p.get("name").getAsString() : "";
            String modId = p.has("mod_id") ? p.get("mod_id").getAsString() : "";
            if (name.trim().isEmpty() || modId.trim().isEmpty()) {
                player.sendSystemMessage(Component.translatable("xam.msg.config_missing_field").withStyle(net.minecraft.ChatFormatting.RED));
                return false;
            }

            // Validar perk_effect
            if (p.has("perk_effect") && !p.get("perk_effect").isJsonNull()) {
                String perk = p.get("perk_effect").getAsString();
                if (!perk.isEmpty()) {
                    ResourceLocation perkRl = ResourceLocation.tryParse(perk);
                    if (perkRl == null || !ForgeRegistries.MOB_EFFECTS.containsKey(perkRl)) {
                        player.sendSystemMessage(Component.translatable("xam.msg.config_unknown_effect", perk).withStyle(net.minecraft.ChatFormatting.RED));
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
                        player.sendSystemMessage(Component.translatable("xam.msg.config_unknown_req_type", reqType).withStyle(net.minecraft.ChatFormatting.RED));
                        return false;
                    }
                }
            }

            // Validar dependencies (formato pathId:count | pathId:% | pathId:mastered)
            if (p.has("dependencies")) {
                for (JsonElement depEl : p.getAsJsonArray("dependencies")) {
                    String dep = depEl.getAsString();
                    if (!dep.matches("^[a-z0-9_]+(:(\\d+%?|mastered))?$")) {
                        player.sendSystemMessage(Component.translatable("xam.msg.config_bad_dependency", dep).withStyle(net.minecraft.ChatFormatting.RED));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void handle(UpdateConfigPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                if (validate(pkt, player)) {
                    ConfigManager.saveConfigFromServer(player.server, pkt.json);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
