package org.xam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.xam.data.PlayerDataProvider;
import org.xam.progression.MasteryService;

import java.util.function.Supplier;

public class TrackRequirementPacket {
    private final String reqKey;

    public TrackRequirementPacket(String reqKey) {
        this.reqKey = reqKey != null ? reqKey : "";
    }

    public static void encode(TrackRequirementPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.reqKey);
    }

    public static TrackRequirementPacket decode(FriendlyByteBuf buf) {
        return new TrackRequirementPacket(buf.readUtf(256));
    }

    public static void handle(TrackRequirementPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    data.setTrackedRequirementKey(pkt.reqKey);
                    MasteryService.sync(player);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}