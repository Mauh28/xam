package org.xam.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xam.XamConstants;
import org.xam.data.PlayerDataProvider;
import org.xam.progression.MasteryService;
import org.xam.util.MessageUtils;

@Mod.EventBusSubscriber(modid = XamConstants.MODID)
public class BlockEventHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            if (MasteryService.mustSelectPath(player, data)) {
                event.setNewSpeed(0.0f);
                MessageUtils.sendWarning(player, Component.translatable("xam.msg.must_select_path"));
                return;
            }
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                MasteryService.checkAndRefreshPlayerData(player, data);
                if (!MasteryService.isItemValid(mainHand, data)) {
                    event.setNewSpeed(0.0f);
                    MessageUtils.sendItemWarning(player, mainHand);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            if (MasteryService.mustSelectPath(player, data)) {
                event.setCanceled(true);
                MessageUtils.sendWarning(player, Component.translatable("xam.msg.must_select_path"));
                return;
            }
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                MasteryService.checkAndRefreshPlayerData(player, data);
                if (!MasteryService.isItemValid(mainHand, data)) {
                    event.setCanceled(true);
                    MessageUtils.sendItemWarning(player, mainHand);
                }
            }
        });
    }
}
