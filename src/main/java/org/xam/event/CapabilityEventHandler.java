package org.xam.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xam.XamConstants;
import org.xam.data.PlayerDataProvider;

@Mod.EventBusSubscriber(modid = XamConstants.MODID)
public class CapabilityEventHandler {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerDataProvider.PLAYER_DATA).isPresent()) {
                event.addCapability(ResourceLocation.fromNamespaceAndPath(XamConstants.MODID, "properties"), new PlayerDataProvider());
            }
        }
    }
}
