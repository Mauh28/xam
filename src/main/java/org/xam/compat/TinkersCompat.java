package org.xam.compat;

import net.minecraft.world.item.Item;

public class TinkersCompat {
    private static Class<?> modifiableItemClass = null;
    private static boolean checkedTconstruct = false;

    public static boolean isTinkersItem(Item item) {
        // ponytail: using reflection to check ModifiableItem without adding tconstruct compile dependency
        if (!checkedTconstruct) {
            try {
                modifiableItemClass = Class.forName("slimeknights.tconstruct.library.tools.item.ModifiableItem");
            } catch (ClassNotFoundException ignored) {}
            checkedTconstruct = true;
        }
        return modifiableItemClass != null && modifiableItemClass.isInstance(item);
    }
}
