package org.xam.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.xam.client.ClientPacketHandler;

import java.util.function.Supplier;

public class SyncPlayerDataPacket {
    private final CompoundTag nbt;

    public SyncPlayerDataPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public static void encode(SyncPlayerDataPacket pkt, FriendlyByteBuf buf) {
        buf.writeNbt(pkt.nbt);
    }

    public static SyncPlayerDataPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerDataPacket(buf.readNbt());
    }

    public static void handle(SyncPlayerDataPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSync(pkt.nbt));
        });
        ctx.get().setPacketHandled(true);
    }
}
