package org.xam.progression;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.data.PlayerData;
import org.xam.util.ItemUtils;

public final class ItemValidityService {

    public static boolean mustSelectPath(Player player, PlayerData data) {
        return false;
    }

    public static boolean isItemValid(ItemStack stack, PlayerData data) {
        if (stack.isEmpty()) return true;
        if (data != null && data.isDevMode()) return true;
        if (ItemUtils.isUniversal(stack)) return true;
        if (ItemUtils.getPathFromItemTags(stack) == null) return true;

        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return false;

        String namespace = rl.getNamespace();

        // 1. O(1) compare namespace against active path mod id
        String activeModId = data.getActivePathModId();
        if (data.getCurrentPath() != null && !activeModId.isEmpty() && namespace.equals(activeModId)) {
            return true;
        }

        // 2. O(1) tags check (second layer of classification) for active path
        String activePathId = data.getCurrentPath();
        if (activePathId != null) {
            PathInfo activePath = ConfigManager.PATHS_MAP.get(activePathId);
            if (activePath != null && activePath.getArmorTag() != null) {
                if (stack.is(activePath.getArmorTag())
                        || stack.is(activePath.getWeaponsTag())
                        || stack.is(activePath.getToolsTag())) {
                    return true;
                }
            }
        }

        // Check mastered paths (small loop, highly efficient)
        for (String pathId : data.getMasteredPaths()) {
            PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
            if (path != null) {
                if (namespace.equals(path.getModId())) {
                    return true;
                }
                if (path.getArmorTag() != null) {
                    if (stack.is(path.getArmorTag())
                            || stack.is(path.getWeaponsTag())
                            || stack.is(path.getToolsTag())) {
                        return true;
                    }
                }
            }
        }

        // Check started/paused paths (small loop, highly efficient)
        for (String pathId : data.getStartedPaths()) {
            PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
            if (path != null) {
                if (namespace.equals(path.getModId())) {
                    return true;
                }
                if (path.getArmorTag() != null) {
                    if (stack.is(path.getArmorTag())
                            || stack.is(path.getWeaponsTag())
                            || stack.is(path.getToolsTag())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
