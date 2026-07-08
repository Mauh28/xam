package org.xam.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.xam.XamConstants;

public class XamNetwork {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(XamConstants.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int packetId = 0;
        CHANNEL.registerMessage(packetId++, SyncPlayerDataPacket.class,
                SyncPlayerDataPacket::encode, SyncPlayerDataPacket::decode, SyncPlayerDataPacket::handle);
        CHANNEL.registerMessage(packetId++, SelectPathPacket.class,
                SelectPathPacket::encode, SelectPathPacket::decode, SelectPathPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncConfigPacket.class,
                SyncConfigPacket::encode, SyncConfigPacket::decode, SyncConfigPacket::handle);
        CHANNEL.registerMessage(packetId++, UpdateConfigPacket.class,
                UpdateConfigPacket::encode, UpdateConfigPacket::decode, UpdateConfigPacket::handle);
        CHANNEL.registerMessage(packetId++, NotifyConfigUpdatePacket.class,
                NotifyConfigUpdatePacket::encode, NotifyConfigUpdatePacket::decode, NotifyConfigUpdatePacket::handle);
        CHANNEL.registerMessage(packetId++, RequestConfigPacket.class,
                RequestConfigPacket::encode, RequestConfigPacket::decode, RequestConfigPacket::handle);
    }
}
