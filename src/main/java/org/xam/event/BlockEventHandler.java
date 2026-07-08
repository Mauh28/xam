package org.xam.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xam.XamConstants;
import org.xam.progression.MasteryGuard;

@Mod.EventBusSubscriber(modid = XamConstants.MODID)
public class BlockEventHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        MasteryGuard.Result result = MasteryGuard.check(player, mainHand);
        if (result != MasteryGuard.Result.ALLOW) {
            event.setNewSpeed(0.0f);
            MasteryGuard.warn(player, result, mainHand);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getMainHandItem();
        MasteryGuard.Result result = MasteryGuard.check(player, mainHand);
        if (result != MasteryGuard.Result.ALLOW) {
            event.setCanceled(true);
            MasteryGuard.warn(player, result, mainHand);
        }
    }
}
