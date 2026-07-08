package org.xam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.xam.client.ClientPacketHandler;

import java.util.function.Supplier;

public class SyncConfigPacket {
    private final String json;

    public SyncConfigPacket(String json) {
        this.json = json;
    }

    public static void encode(SyncConfigPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.json);
    }

    public static SyncConfigPacket decode(FriendlyByteBuf buf) {
        return new SyncConfigPacket(buf.readUtf(262144));
    }

    public static void handle(SyncConfigPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncConfig(pkt.json));
        });
        ctx.get().setPacketHandled(true);
    }
}
