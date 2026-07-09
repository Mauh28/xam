package org.xam.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.XamConstants;
import org.xam.compat.TinkersCompat;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;

public class ItemUtils {
    public static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.ProjectileWeaponItem
                || item instanceof net.minecraft.world.item.TridentItem) {
            return true;
        }
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        if (rl != null) {
            String path = rl.getPath();
            return path.contains("sword") || path.contains("bow");
        }
        return false;
    }

    public static boolean isUniversal(ItemStack stack) {
        if (stack.isEmpty()) return true;
        Item item = stack.getItem();
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        if (rl == null) return false;

        String namespace = rl.getNamespace();

        // Check dynamic universal namespaces
        if (ConfigManager.UNIVERSAL_NAMESPACES.contains(namespace)) {
            return true;
        }

        // Tinkers' Construct check (tconstruct namespace or ModifiableItem class)
        if (namespace.equals("tconstruct") || TinkersCompat.isTinkersItem(item)) {
            return true;
        }

        // xam:universal/armor, weapons, tools
        if (stack.is(XamConstants.UNIVERSAL_ARMOR_TAG)
                || stack.is(XamConstants.UNIVERSAL_WEAPONS_TAG)
                || stack.is(XamConstants.UNIVERSAL_TOOLS_TAG)) {
            return true;
        }

        return false;
    }

    public static String getPathFromItemTags(ItemStack stack) {
        if (stack.isEmpty()) return null;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null) return null;

        // O(1) lookup by namespace
        PathInfo byNamespace = ConfigManager.NAMESPACE_TO_PATH.get(rl.getNamespace());
        if (byNamespace != null) return byNamespace.id;

        // Fallback check for legacy path tags xam:path_id/...
        for (PathInfo path : ConfigManager.PATHS) {
            if (path.armorTag != null) {
                if (stack.is(path.armorTag)
                        || stack.is(path.weaponsTag)
                        || stack.is(path.toolsTag)) {
                    return path.id;
                }
            }
        }
        return null;
    }
}
