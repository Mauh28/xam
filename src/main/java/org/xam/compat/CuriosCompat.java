package org.xam.compat;

import net.minecraft.world.entity.player.Player;
import org.xam.data.PlayerData;

public class CuriosCompat {
    private static Boolean isCuriosLoaded = null;

    public static boolean isCuriosInstalled() {
        if (isCuriosLoaded == null) {
            isCuriosLoaded = net.minecraftforge.fml.ModList.get().isLoaded("curios");
        }
        return isCuriosLoaded;
    }

    public static void checkAndEjectCurios(Player player, PlayerData data) {
        if (isCuriosInstalled()) {
            CuriosImpl.checkAndEjectCurios(player, data);
        }
    }

    // Defer loading of CuriosCompatHelper class to prevent ClassNotFoundException if curios mod is not present
    private static class CuriosImpl {
        public static void checkAndEjectCurios(Player player, PlayerData data) {
            CuriosCompatHelper.checkAndEjectCurios(player, data);
        }
    }
}
