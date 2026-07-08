package org.xam.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.XamConstants;
import org.xam.progression.MasteryGuard;
import org.xam.progression.MasteryService;

@Mod.EventBusSubscriber(modid = XamConstants.MODID)
public class ItemEventHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack crafted = event.getCrafting();
            if (!crafted.isEmpty()) {
                ResourceLocation rl = ForgeRegistries.ITEMS.getKey(crafted.getItem());
                if (rl != null) {
                    MasteryService.checkAndProgressRequirement(serverPlayer, "craft", rl.toString());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        MasteryGuard.Result result = MasteryGuard.check(player, stack);
        if (result != MasteryGuard.Result.ALLOW) {
            event.setCanceled(true);
            MasteryGuard.warn(player, result, stack);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        MasteryGuard.Result result = MasteryGuard.check(player, stack);
        if (result != MasteryGuard.Result.ALLOW) {
            event.setCanceled(true);
            MasteryGuard.warn(player, result, stack);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        MasteryGuard.Result result = MasteryGuard.check(player, stack);
        if (result != MasteryGuard.Result.ALLOW) {
            event.setCanceled(true);
            MasteryGuard.warn(player, result, stack);
        }
    }
}
