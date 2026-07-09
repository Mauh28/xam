package org.xam.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.xam.data.PlayerDataProvider;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.progression.MasteryService;

import java.util.function.Supplier;

public class SelectPathPacket {
    private final String pathId;

    public SelectPathPacket(String pathId) {
        this.pathId = pathId;
    }

    public static void encode(SelectPathPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.pathId);
    }

    public static SelectPathPacket decode(FriendlyByteBuf buf) {
        return new SelectPathPacket(buf.readUtf(256));
    }

    public static void handle(SelectPathPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                if (!PacketRateLimiter.canSelectPath(player.getUUID())) {
                    org.xam.XamConstants.LOGGER.warn("Player {} rate-limited on SelectPathPacket", player.getName().getString());
                    return;
                }
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    if (pkt.pathId != null) {
                        PathInfo targetPath = ConfigManager.PATHS_MAP.get(pkt.pathId);
                        if (targetPath == null || targetPath.requirements.isEmpty() || data.getMasteredPaths().contains(pkt.pathId) || !MasteryService.areDependenciesMastered(player, data, targetPath)) {
                            MasteryService.sync(player);
                            return;
                        }
                    }
                    if (!MasteryService.canSwitchFromCurrentPath(player, data)) {
                        MasteryService.sync(player);
                        return;
                    }
                    data.setCurrentPath(pkt.pathId);
                    MasteryService.sync(player);
                    MasteryService.updateArmorModifiers(player);
                    if (pkt.pathId != null) {
                        PathInfo targetPath = ConfigManager.PATHS_MAP.get(pkt.pathId);
                        if (targetPath != null) {
                            MasteryService.checkPathCompletion(player, data, targetPath);
                        }
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
