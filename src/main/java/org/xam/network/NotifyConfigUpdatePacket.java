package org.xam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.xam.client.ClientPacketHandler;

import java.util.function.Supplier;

public class NotifyConfigUpdatePacket {
    private final long version;

    public NotifyConfigUpdatePacket(long version) {
        this.version = version;
    }

    public static void encode(NotifyConfigUpdatePacket pkt, FriendlyByteBuf buf) {
        buf.writeLong(pkt.version);
    }

    public static NotifyConfigUpdatePacket decode(FriendlyByteBuf buf) {
        return new NotifyConfigUpdatePacket(buf.readLong());
    }

    public static void handle(NotifyConfigUpdatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleNotifyConfigUpdate(pkt.version));
        });
        ctx.get().setPacketHandled(true);
    }
}
