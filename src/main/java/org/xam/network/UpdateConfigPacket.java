package org.xam.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.xam.config.ConfigManager;

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

    public static void handle(UpdateConfigPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                // ponytail: server-side validation — don't trust client data
                try {
                    JsonObject json = new Gson().fromJson(pkt.json, JsonObject.class);
                    if (json != null && json.has("paths")) {
                        JsonArray paths = json.getAsJsonArray("paths");
                        for (int i = 0; i < paths.size(); i++) {
                            JsonObject p = paths.get(i).getAsJsonObject();
                            String id = p.has("id") ? p.get("id").getAsString() : "";
                            String name = p.has("name") ? p.get("name").getAsString() : "";
                            String modId = p.has("mod_id") ? p.get("mod_id").getAsString() : "";
                            if (id.trim().isEmpty() || name.trim().isEmpty() || modId.trim().isEmpty()) {
                                player.sendSystemMessage(Component.literal("[XAM] Rama inválida: campos vacíos en '" + name + "'").withStyle(net.minecraft.ChatFormatting.RED));
                                return;
                            }
                            if (p.has("requirements")) {
                                JsonArray reqs = p.getAsJsonArray("requirements");
                                for (int j = 0; j < reqs.size(); j++) {
                                    JsonObject r = reqs.get(j).getAsJsonObject();
                                    String rId = r.has("id") ? r.get("id").getAsString() : "";
                                    String rName = r.has("name") ? r.get("name").getAsString() : "";
                                    if (rId.trim().isEmpty() || rName.trim().isEmpty()) {
                                        player.sendSystemMessage(Component.literal("[XAM] Tarea inválida en rama '" + name + "': campos vacíos").withStyle(net.minecraft.ChatFormatting.RED));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    player.sendSystemMessage(Component.literal("[XAM] JSON de configuración inválido").withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }
                ConfigManager.saveConfigFromServer(player.server, pkt.json);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
