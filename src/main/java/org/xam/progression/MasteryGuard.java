package org.xam.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.xam.data.PlayerDataProvider;
import org.xam.util.MessageUtils;

import java.util.concurrent.atomic.AtomicReference;

public final class MasteryGuard {

    public enum Result { ALLOW, MUST_SELECT_PATH, ITEM_INVALID }

    /**
     * Central check: capability → mustSelectPath → isItemValid.
     * Returns ALLOW if the action should proceed.
     */
    public static Result check(Player player, ItemStack stack) {
        AtomicReference<Result> out = new AtomicReference<>(Result.ALLOW);
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            if (MasteryService.mustSelectPath(player, data)) {
                out.set(Result.MUST_SELECT_PATH);
                return;
            }
            if (!stack.isEmpty()) {
                MasteryService.checkAndRefreshPlayerData(player, data);
                if (!MasteryService.isItemValid(stack, data)) {
                    out.set(Result.ITEM_INVALID);
                }
            }
        });
        return out.get();
    }

    /** Sends the appropriate warning for the given result. */
    public static void warn(Player player, Result result, ItemStack stack) {
        switch (result) {
            case MUST_SELECT_PATH -> MessageUtils.sendWarning(player, Component.translatable("xam.msg.must_select_path"));
            case ITEM_INVALID -> MessageUtils.sendItemWarning(player, stack);
            default -> {}
        }
    }
}
