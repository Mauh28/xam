package org.xam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.xam.config.ConfigManager;

import java.util.function.Supplier;

public class RequestConfigPacket {
    public RequestConfigPacket() {}

    public static void encode(RequestConfigPacket pkt, FriendlyByteBuf buf) {}

    public static RequestConfigPacket decode(FriendlyByteBuf buf) {
        return new RequestConfigPacket();
    }

    public static void handle(RequestConfigPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                String pathsJson = ConfigManager.getPathsJson();
                XamNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncConfigPacket(pathsJson));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
