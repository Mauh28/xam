package org.xam.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.data.PlayerDataProvider;
import org.xam.util.MessageUtils;

import java.util.concurrent.atomic.AtomicReference;

public final class MasteryGuard {

    public enum Result { ALLOW, ITEM_INVALID }

    public static boolean isToolOrArmor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof net.minecraft.world.item.DiggerItem
                || item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.ShearsItem
                || item instanceof net.minecraft.world.item.ProjectileWeaponItem
                || item instanceof net.minecraft.world.item.TridentItem
                || item instanceof net.minecraft.world.item.ShieldItem
                || item instanceof net.minecraft.world.item.ArmorItem
                || item instanceof net.minecraft.world.item.TieredItem) {
            return true;
        }
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        if (rl != null) {
            String path = rl.getPath().toLowerCase();
            if (path.contains("pickaxe") || path.contains("axe") || path.contains("shovel")
                    || path.contains("hoe") || path.contains("sword") || path.contains("bow")
                    || path.contains("shield") || path.contains("helmet") || path.contains("chestplate")
                    || path.contains("leggings") || path.contains("boots") || path.contains("armor")
                    || path.contains("tool") || path.contains("weapon")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNonVanillaTinkersTool(ItemStack stack) {
        if (org.xam.util.ItemUtils.isUniversal(stack)) return false;
        if (!isToolOrArmor(stack)) return false;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;

        String namespace = rl.getNamespace();
        if (namespace.equals("minecraft") || namespace.equals("tconstruct") || org.xam.compat.TinkersCompat.isTinkersItem(stack.getItem())) {
            return false;
        }
        return true;
    }

    /**
     * Central check: capability → isItemValid.
     * Returns ALLOW if the action should proceed.
     */
    public static Result check(Player player, ItemStack stack) {
        if (stack.isEmpty()) return Result.ALLOW;
        AtomicReference<Result> out = new AtomicReference<>(Result.ALLOW);
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            if (data.isDevMode()) return;
            MasteryService.checkAndRefreshPlayerData(player, data);

            if (!MasteryService.isItemValid(stack, data)) {
                out.set(Result.ITEM_INVALID);
            }
        });
        return out.get();
    }

    /** Sends the appropriate warning for the given result. */
    public static void warn(Player player, Result result, ItemStack stack) {
        switch (result) {
            case ITEM_INVALID -> MessageUtils.sendItemWarning(player, stack);
            default -> {}
        }
    }
}
