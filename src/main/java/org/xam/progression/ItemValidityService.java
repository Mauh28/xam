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
        if (data != null && data.isDevMode()) return false;
        if (data.getCurrentPath() != null) return false;
        for (PathInfo path : ConfigManager.PATHS) {
            if (!data.getMasteredPaths().contains(path.id) && DependencyResolver.areDependenciesMastered(player, data, path)) {
                return true;
            }
        }
        return false; // all paths mastered or none unlocked, no restriction
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
            if (activePath != null && activePath.armorTag != null) {
                if (stack.is(activePath.armorTag)
                        || stack.is(activePath.weaponsTag)
                        || stack.is(activePath.toolsTag)) {
                    return true;
                }
            }
        }

        // Check mastered paths (small loop, highly efficient)
        for (String pathId : data.getMasteredPaths()) {
            PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
            if (path != null) {
                if (namespace.equals(path.mod_id)) {
                    return true;
                }
                if (path.armorTag != null) {
                    if (stack.is(path.armorTag)
                            || stack.is(path.weaponsTag)
                            || stack.is(path.toolsTag)) {
                        return true;
                    }
                }
            }
        }

        // Check started/paused paths (small loop, highly efficient)
        for (String pathId : data.getStartedPaths()) {
            PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
            if (path != null) {
                if (namespace.equals(path.mod_id)) {
                    return true;
                }
                if (path.armorTag != null) {
                    if (stack.is(path.armorTag)
                            || stack.is(path.weaponsTag)
                            || stack.is(path.toolsTag)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
