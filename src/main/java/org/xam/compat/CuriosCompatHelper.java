package org.xam.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.xam.data.PlayerData;
import org.xam.progression.MasteryService;
import org.xam.util.MessageUtils;

import java.util.Map;

public class CuriosCompatHelper {
    public static void checkAndEjectCurios(Player player, PlayerData data) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            Map<String, ICurioStacksHandler> curios = handler.getCurios();
            for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                ICurioStacksHandler stacksHandler = entry.getValue();
                IItemHandlerModifiable stacks = stacksHandler.getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    ItemStack stack = stacks.getStackInSlot(slot);
                    if (!stack.isEmpty() && !MasteryService.isItemValid(stack, data)) {
                        ItemStack copy = stack.copy();
                        stacks.setStackInSlot(slot, ItemStack.EMPTY);
                        
                        MessageUtils.sendWarning(player, net.minecraft.network.chat.Component.translatable("xam.msg.curio_rejected"));
                        
                        if (!player.getInventory().add(copy)) {
                            player.drop(copy, false);
                        }
                    }
                }
            }
        });
    }
}
