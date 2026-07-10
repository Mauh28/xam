package org.xam.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.config.PathInfo;

/**
 * Centralizes path icon resolution: config icon → hardcoded fallback → default book.
 */
public final class PathIcons {
    private PathIcons() {}

    /** Resolve icon for a PathInfo: tries path.getIcon() from registry, then fallback by id. */
    public static ItemStack getIcon(PathInfo path) {
        if (path.getIcon() != null) {
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(path.getIcon()));
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) return stack;
            }
        }
        return getDefaultIcon(path.getId());
    }

    /** Hardcoded fallback icons for known path ids. */
    public static ItemStack getDefaultIcon(String pathId) {
        if (pathId == null) return new ItemStack(Items.WRITABLE_BOOK);
        // ponytail: compatibility fallbacks for configs without explicit icon field
        return switch (pathId) {
            case "botania"  -> new ItemStack(Items.POPPY);
            case "mekanism" -> new ItemStack(Items.REDSTONE);
            default         -> new ItemStack(Items.WRITABLE_BOOK);
        };
    }

    /** Returns the default icon string id (for ConfigManager parse-time assignment). */
    public static String getDefaultIconId(String pathId) {
        if (pathId == null) return "minecraft:writable_book";
        return switch (pathId) {
            case "botania"  -> "minecraft:poppy";
            case "mekanism" -> "minecraft:redstone";
            default         -> "minecraft:writable_book";
        };
    }
}
