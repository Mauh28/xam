package org.xam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xam.client.gui.MasteryCompletionToast;
import org.xam.client.gui.MasteryHubScreen;
import org.xam.client.gui.PathSelectionScreen;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.data.PlayerDataProvider;
import org.xam.network.RequestConfigPacket;
import org.xam.network.XamNetwork;
import org.xam.util.PathIcons;

import java.util.ArrayList;
import java.util.List;

public class ClientPacketHandler {
    private static final Logger LOGGER = LogManager.getLogger(ClientPacketHandler.class);
    // shouldOpenPathSelection removed
    private static java.lang.reflect.Field progressField;

    static {
        try {
            progressField = net.minecraft.client.multiplayer.ClientAdvancements.class.getDeclaredField("f_104378_");
            progressField.setAccessible(true);
        } catch (Exception e) {
            try {
                progressField = net.minecraft.client.multiplayer.ClientAdvancements.class.getDeclaredField("progress");
                progressField.setAccessible(true);
            } catch (Exception e2) {
                LOGGER.error("Failed to cache ClientAdvancements progress field", e2);
            }
        }
    }

    public static boolean isClientAdvancementCompleted(String id) {
        ResourceLocation resLoc = ResourceLocation.tryParse(id);
        if (resLoc == null || progressField == null) return false;
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            var clientAdvs = connection.getAdvancements();
            net.minecraft.advancements.Advancement adv = clientAdvs.getAdvancements().get(resLoc);
            if (adv != null) {
                try {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) progressField.get(clientAdvs);
                    if (map != null) {
                        Object val = map.get(adv);
                        if (val instanceof net.minecraft.advancements.AdvancementProgress progress) {
                            return progress.isDone();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    public static void handleSync(CompoundTag nbt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                boolean isFirstSync = !data.isInitialized();
                List<String> oldMastered = new ArrayList<>(data.getMasteredPaths());
                boolean oldAllMastered = data.isCompletedAllMasteries();
                data.loadNBTData(nbt);
                boolean newAllMastered = data.isCompletedAllMasteries();
                data.setInitialized(true);

                if (!isFirstSync) {
                    if (newAllMastered && !oldAllMastered) {
                        mc.getToasts().addToast(new MasteryCompletionToast(
                                Component.translatable("xam.toast.all_masteries_completed_title"),
                                Component.translatable("xam.toast.all_masteries_completed_desc"),
                                ItemStack.EMPTY
                        ));
                    } else {
                        List<String> newMastered = data.getMasteredPaths();
                        for (String pathId : newMastered) {
                            if (!oldMastered.contains(pathId)) {
                                // Find the name and icon of the mastered path
                                String pathName = pathId;
                                PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
                                ItemStack iconStack;
                                if (path != null) {
                                    pathName = path.getName();
                                    iconStack = PathIcons.getIcon(path);
                                } else {
                                    iconStack = PathIcons.getDefaultIcon(pathId);
                                }
                                // Show custom premium toast notification
                                mc.getToasts().addToast(new MasteryCompletionToast(
                                        Component.translatable("xam.toast.mastery_completed"),
                                        Component.translatable("xam.toast.mastered_format", Component.translatable(pathName)),
                                        iconStack
                                 ));
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    public static void handleSyncConfig(String json) {
        ConfigManager.loadConfigFromJson(json);
    }

    public static void handleNotifyConfigUpdate(long version) {
        if (ConfigManager.getConfigVersion() < version) {
            XamNetwork.CHANNEL.sendToServer(new RequestConfigPacket());
        }
    }
}
