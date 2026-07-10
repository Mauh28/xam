package org.xam.progression;

import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.client.ClientPacketHandler;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerData;
import org.xam.data.PlayerDataProvider;
import org.xam.network.SyncPlayerDataPacket;
import org.xam.network.XamNetwork;

public class MasteryService {

    public static void checkAndRefreshPlayerData(Player player, PlayerData data) {
        if (data.getLastConfigVersion() < ConfigManager.getConfigVersion()) {
            String currentPath = data.getCurrentPath();
            boolean found = false;
            if (currentPath != null) {
                PathInfo path = ConfigManager.PATHS_MAP.get(currentPath);
                if (path != null) {
                    data.setActivePathModId(path.mod_id);
                    found = true;
                }
            }
            if (!found && currentPath != null) {
                data.setCurrentPath(null);
                data.setActivePathModId("");
            } else if (!found) {
                data.setActivePathModId("");
            }
            data.setLastConfigVersion(ConfigManager.getConfigVersion());
            if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                sync(serverPlayer);
            }
        }
    }

    public static boolean hasItem(Player player, ResourceLocation itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdvancementCompleted(Player player, String id) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResourceLocation resLoc = ResourceLocation.tryParse(id);
            if (resLoc == null) return false;
            Advancement adv = serverPlayer.server.getAdvancements().getAdvancement(resLoc);
            return adv != null && serverPlayer.getAdvancements().getOrStartProgress(adv).isDone();
        } else {
            return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.isClientAdvancementCompleted(id));
        }
    }

    public static boolean isRequirementCompleted(Player player, PlayerData data, String pathId, Requirement req) {
        if (req == null) return false;
        if (req.type.equals("advancement")) {
            return isAdvancementCompleted(player, req.id);
        } else {
            String reqKey = pathId + ":" + req.type + ":" + req.id;
            return data.getCompletedRequirements().contains(reqKey);
        }
    }

    public static void checkPathCompletion(ServerPlayer player, PlayerData data, PathInfo pathInfo) {
        boolean completedAll = true;
        for (Requirement req : pathInfo.requirements) {
            if (!isRequirementCompleted(player, data, pathInfo.id, req)) {
                completedAll = false;
                break;
            }
        }

        if (completedAll) {
            data.addMasteredPath(pathInfo.id);
            data.setCurrentPath(null);
            data.removeCompletedRequirementsIf(k -> k.startsWith(pathInfo.id + ":"));
            sync(player);
            updateArmorModifiers(player);
            player.sendSystemMessage(Component.translatable("xam.msg.mastered_announcement", pathInfo.name));
        }
    }

    public static void checkAndProgressRequirement(ServerPlayer player, String type, String targetId) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            String currentPath = data.getCurrentPath();
            if (currentPath == null) return;

            PathInfo pathInfo = ConfigManager.PATHS_MAP.get(currentPath);
            if (pathInfo == null) return;

            boolean changed = false;
            for (Requirement req : pathInfo.requirements) {
                if (req.type.equals(type) && req.id.equals(targetId)) {
                    // ponytail: skip if this requirement's own dependencies aren't satisfied yet
                    if (!areRequirementDependenciesMet(player, data, req)) continue;
                    String reqKey = currentPath + ":" + type + ":" + targetId;
                    if (!data.getCompletedRequirements().contains(reqKey)) {
                        data.addCompletedRequirement(reqKey);
                        changed = true;
                    }
                }
            }

            if (changed) {
                player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.2F);
                sync(player);
                checkPathCompletion(player, data, pathInfo);
            }
        });
    }

    public static int getCompletedRequirementsCount(Player player, PlayerData data, PathInfo path) {
        if (path == null) return 0;
        int count = 0;
        for (Requirement req : path.requirements) {
            if (isRequirementCompleted(player, data, path.id, req)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isDependencyMet(Player player, PlayerData data, String depStr) {
        return DependencyResolver.isDependencyMet(player, data, depStr);
    }

    public static boolean areDependenciesMastered(Player player, PlayerData data, PathInfo path) {
        return DependencyResolver.areDependenciesMastered(player, data, path);
    }

    public static boolean canSwitchFromCurrentPath(Player player, PlayerData data) {
        String current = data.getCurrentPath();
        if (current == null) return true;
        PathInfo path = ConfigManager.PATHS_MAP.get(current);
        if (path == null) return true;

        int threshold = path.min_to_switch;
        if (threshold < 0) {
            return data.getMasteredPaths().contains(current);
        }

        int completed = getCompletedRequirementsCount(player, data, path);
        return completed >= threshold;
    }

    public static boolean areRequirementDependenciesMet(Player player, PlayerData data, Requirement req) {
        return DependencyResolver.areRequirementDependenciesMet(player, data, req);
    }

    // ponytail: returns true if the player must select a path before doing anything
    public static boolean mustSelectPath(Player player, PlayerData data) {
        return ItemValidityService.mustSelectPath(player, data);
    }

    public static boolean isItemValid(ItemStack stack, PlayerData data) {
        return ItemValidityService.isItemValid(stack, data);
    }

    public static void updateArmorModifiers(Player player) {
        ArmorModifierService.updateArmorModifiers(player);
    }

    public static void sync(ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            CompoundTag nbt = new CompoundTag();
            data.saveNBTData(nbt);
            XamNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerDataPacket(nbt));
        });
    }
}
