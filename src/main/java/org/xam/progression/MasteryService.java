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
                    data.setActivePathModId(path.getModId());
                    found = true;
                }
            }
            if (!found && currentPath != null) {
                data.setCurrentPath(null);
                data.setActivePathModId("");
            } else if (!found) {
                data.setActivePathModId("");
            }

            // Self-healing clean-up for started paths that no longer exist in config
            java.util.List<String> started = new java.util.ArrayList<>(data.getStartedPaths());
            for (String pId : started) {
                if (!ConfigManager.PATHS_MAP.containsKey(pId)) {
                    data.removeStartedPath(pId);
                }
            }

            // Self-healing clean-up for mastered paths that no longer exist in config
            java.util.List<String> mastered = new java.util.ArrayList<>(data.getMasteredPaths());
            for (String pId : mastered) {
                if (!ConfigManager.PATHS_MAP.containsKey(pId)) {
                    data.removeMasteredPath(pId);
                }
            }

            // Self-healing clean-up for completed requirements of paths that no longer exist in config
            data.removeCompletedRequirementsIf(k -> {
                String[] parts = k.split(":");
                if (parts.length > 0) {
                    return !ConfigManager.PATHS_MAP.containsKey(parts[0]);
                }
                return false;
            });

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

    public static String getRequirementShortKey(Requirement req) {
        if (req == null) return "";
        String base = req.getType() + ":" + req.getId();
        if (req.getEffect() != null && !req.getEffect().isEmpty()) {
            return base + ":" + req.getEffect();
        }
        return base;
    }

    public static String getRequirementKey(String pathId, Requirement req) {
        if (req == null) return "";
        return pathId + ":" + getRequirementShortKey(req);
    }

    public static boolean isRequirementCompleted(Player player, PlayerData data, String pathId, Requirement req) {
        if (req == null) return false;
        if (req.getType().equals("advancement")) {
            return isAdvancementCompleted(player, req.getId());
        } else {
            return data.getCompletedRequirements().contains(getRequirementKey(pathId, req));
        }
    }

    public static PathInfo findMostProgressedAvailablePath(Player player, PlayerData data) {
        // First try to pick from started paths prioritizing most progress.
        // A path is started if it is in startedPaths list OR has completed requirements count > 0.
        PathInfo bestStarted = null;
        int bestStartedCount = -1;
        for (PathInfo path : ConfigManager.PATHS) {
            if (path.getRequirements().isEmpty()) continue;
            if (data.getMasteredPaths().contains(path.getId())) continue;
            if (!DependencyResolver.areDependenciesMastered(player, data, path)) continue;

            boolean isStarted = data.getStartedPaths().contains(path.getId()) 
                    || getCompletedRequirementsCount(player, data, path) > 0;

            if (isStarted) {
                int count = getCompletedRequirementsCount(player, data, path);
                if (count > bestStartedCount) {
                    bestStartedCount = count;
                    bestStarted = path;
                }
            }
        }
        if (bestStarted != null) {
            return bestStarted;
        }

        // Fallback to any available path if no started paths are valid
        PathInfo best = null;
        int bestCount = -1;
        for (PathInfo path : ConfigManager.PATHS) {
            if (path.getRequirements().isEmpty()) continue;
            if (data.getMasteredPaths().contains(path.getId())) continue;
            if (!DependencyResolver.areDependenciesMastered(player, data, path)) continue;

            int count = getCompletedRequirementsCount(player, data, path);
            if (count > bestCount) {
                bestCount = count;
                best = path;
            }
        }
        return best;
    }

    public static void checkPathCompletion(ServerPlayer player, PlayerData data, PathInfo pathInfo) {
        boolean completedAll = true;
        for (Requirement req : pathInfo.getRequirements()) {
            if (!isRequirementCompleted(player, data, pathInfo.getId(), req)) {
                completedAll = false;
                break;
            }
        }

        if (completedAll) {
            data.addMasteredPath(pathInfo.getId());

            PathInfo bestPath = findMostProgressedAvailablePath(player, data);
            if (bestPath != null) {
                data.setCurrentPath(bestPath.getId());
                data.setActivePathModId(bestPath.getModId());
            } else {
                data.setCurrentPath(null);
                data.setActivePathModId("");
            }

            sync(player);
            updateArmorModifiers(player);
            player.sendSystemMessage(Component.translatable("xam.msg.mastered_announcement", Component.translatable(pathInfo.getName())));

            if (bestPath != null) {
                player.sendSystemMessage(Component.translatable("xam.msg.auto_assigned_path", Component.translatable(bestPath.getName())).withStyle(net.minecraft.ChatFormatting.GREEN));
            }
        }
    }

    public static void revalidateMasteredPaths(ServerPlayer player, PlayerData data) {
        java.util.List<String> toUnmaster = new java.util.ArrayList<>();
        for (String pathId : data.getMasteredPaths()) {
            PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
            if (path == null) continue;

            boolean allMet = true;
            for (Requirement req : path.getRequirements()) {
                if (!isRequirementCompleted(player, data, pathId, req)) {
                    String expectedKey = getRequirementKey(pathId, req);
                    org.xam.XamConstants.LOGGER.warn("XAM Revalidation failed for player {} on path {}: requirement {} not completed. Key expected: '{}'. Player completed requirements: {}",
                            player.getGameProfile().getName(), pathId, req.getId(), expectedKey, data.getCompletedRequirements());
                    allMet = false;
                    break;
                }
            }
            if (!allMet) {
                toUnmaster.add(pathId);
            }
        }

        if (toUnmaster.isEmpty()) return;

        for (String pathId : toUnmaster) {
            data.removeMasteredPath(pathId);
            PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
            String pathName = path != null ? path.getName() : pathId;

            if (data.getCurrentPath() == null && path != null) {
                data.setCurrentPath(pathId);
                data.setActivePathModId(path.getModId());
            }

            player.sendSystemMessage(Component.translatable("xam.msg.mastery_revoked", Component.translatable(pathName)).withStyle(net.minecraft.ChatFormatting.YELLOW));
        }

        sync(player);
        updateArmorModifiers(player);
    }

    public static void checkAndProgressRequirement(ServerPlayer player, String type, String targetId) {
        checkAndProgressRequirement(player, type, targetId, null);
    }

    public static void checkAndProgressRequirement(ServerPlayer player, String type, String targetId, net.minecraft.world.entity.LivingEntity killedEntity) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            String currentPath = data.getCurrentPath();
            if (currentPath == null) return;

            PathInfo pathInfo = ConfigManager.PATHS_MAP.get(currentPath);
            if (pathInfo == null) return;

            boolean changed = false;
            for (Requirement req : pathInfo.getRequirements()) {
                if (req.getType().equals(type) && req.getId().equals(targetId)) {
                    if (type.equals("kill") && killedEntity != null && req.getEffect() != null && !req.getEffect().isEmpty()) {
                        if (!hasRequiredEffect(killedEntity, req.getEffect())) {
                            continue;
                        }
                    }
                    // ponytail: skip if this requirement's own dependencies aren't satisfied yet
                    if (!areRequirementDependenciesMet(player, data, req)) continue;
                    String reqKey = getRequirementKey(currentPath, req);
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

    private static boolean hasRequiredEffect(net.minecraft.world.entity.LivingEntity entity, String effectStr) {
        String effectId = "";
        int requiredLevel = 1;
        if (effectStr.contains(" ")) {
            int lastSpace = effectStr.lastIndexOf(' ');
            effectId = effectStr.substring(0, lastSpace).trim();
            try {
                requiredLevel = Integer.parseInt(effectStr.substring(lastSpace + 1).trim());
            } catch (NumberFormatException ignored) {}
        } else {
            effectId = effectStr.trim();
        }

        if (effectId.equals("minecraft:strenght")) {
            effectId = "minecraft:strength";
        }

        ResourceLocation rl = ResourceLocation.tryParse(effectId);
        if (rl == null) return false;

        net.minecraft.world.effect.MobEffect effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(rl);
        if (effect == null) return false;

        net.minecraft.world.effect.MobEffectInstance instance = entity.getEffect(effect);
        if (instance == null) return false;

        return instance.getAmplifier() >= (requiredLevel - 1);
    }

    public static int getCompletedRequirementsCount(Player player, PlayerData data, PathInfo path) {
        if (path == null) return 0;
        int count = 0;
        for (Requirement req : path.getRequirements()) {
            if (isRequirementCompleted(player, data, path.getId(), req)) {
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

        int threshold = path.getMinToSwitch();
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
