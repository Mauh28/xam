package org.xam.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.XamConstants;
import org.xam.data.PlayerDataProvider;
import org.xam.progression.MasteryService;
import org.xam.util.MessageUtils;

@Mod.EventBusSubscriber(modid = XamConstants.MODID)
public class CombatEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                ItemStack mainHand = player.getMainHandItem();
                if (data.getCurrentPath() == null) {
                    if (!mainHand.isEmpty() && org.xam.progression.MasteryGuard.isNonVanillaTinkersTool(mainHand)) {
                        event.setAmount(1.0f);
                        MessageUtils.sendItemWarning(player, mainHand);
                    }
                    return;
                }

                if (!mainHand.isEmpty()) {
                    MasteryService.checkAndRefreshPlayerData(player, data);
                    if (!MasteryService.isItemValid(mainHand, data)) {
                        event.setAmount(1.0f);
                        MessageUtils.sendItemWarning(player, mainHand);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource() != null && event.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
            if (rl != null) {
                MasteryService.checkAndProgressRequirement(serverPlayer, "kill", rl.toString(), event.getEntity());
            }
        }
    }
}
