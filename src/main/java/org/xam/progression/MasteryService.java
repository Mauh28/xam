package org.xam.progression;

import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.XamConstants;
import org.xam.client.ClientPacketHandler;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerData;
import org.xam.data.PlayerDataProvider;
import org.xam.network.SyncPlayerDataPacket;
import org.xam.network.XamNetwork;
import org.xam.util.ItemUtils;
import org.xam.util.MessageUtils;

import java.util.UUID;

public class MasteryService {

    public static void checkAndRefreshPlayerData(Player player, PlayerData data) {
        if (data.getLastConfigVersion() < ConfigManager.getConfigVersion()) {
            String currentPath = data.getCurrentPath();
            boolean found = false;
            if (currentPath != null) {
                for (PathInfo path : ConfigManager.PATHS) {
                    if (path.id.equals(currentPath)) {
                        data.setActivePathModId(path.mod_id);
                        found = true;
                        break;
                    }
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
            data.getCompletedRequirements().removeIf(k -> k.startsWith(pathInfo.id + ":"));
            sync(player);
            updateArmorModifiers(player);
            player.sendSystemMessage(Component.translatable("xam.msg.mastered_announcement", pathInfo.name));
        }
    }

    public static void checkAndProgressRequirement(ServerPlayer player, String type, String targetId) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            String currentPath = data.getCurrentPath();
            if (currentPath == null) return;

            PathInfo pathInfo = null;
            for (PathInfo path : ConfigManager.PATHS) {
                if (path.id.equals(currentPath)) {
                    pathInfo = path;
                    break;
                }
            }
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
        if (depStr == null || depStr.isEmpty()) return true;
        String[] parts = depStr.split(":");
        String depPathId = parts[0];
        if (!ConfigManager.PATHS_MAP.containsKey(depPathId)) {
            return true; // ponytail: ignore deleted dependencies to prevent deadlocks
        }
        String amountStr = parts.length > 1 ? parts[1] : "mastered";

        if (amountStr.equalsIgnoreCase("mastered") || amountStr.equalsIgnoreCase("all")) {
            return data.getMasteredPaths().contains(depPathId);
        }

        int requiredCount = 0;
        PathInfo depPath = ConfigManager.PATHS_MAP.get(depPathId);
        if (depPath == null) return true;

        try {
            if (amountStr.endsWith("%")) {
                int pct = Integer.parseInt(amountStr.replace("%", ""));
                if (!depPath.requirements.isEmpty()) {
                    requiredCount = (int) Math.ceil((pct / 100.0) * depPath.requirements.size());
                }
            } else {
                requiredCount = Integer.parseInt(amountStr);
            }
        } catch (NumberFormatException e) {
            return data.getMasteredPaths().contains(depPathId);
        }

        if (data.getMasteredPaths().contains(depPathId)) {
            return true;
        }

        int completed = getCompletedRequirementsCount(player, data, depPath);
        return completed >= requiredCount;
    }

    public static boolean areDependenciesMastered(Player player, PlayerData data, PathInfo path) {
        if (path.dependencies == null || path.dependencies.isEmpty()) return true;
        for (String dep : path.dependencies) {
            if (!isDependencyMet(player, data, dep)) {
                return false;
            }
        }
        return true;
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
        if (req.dependencies == null || req.dependencies.isEmpty()) return true;
        String currentPath = data.getCurrentPath();
        if (currentPath == null) return false;
        PathInfo path = ConfigManager.PATHS_MAP.get(currentPath);
        if (path == null) return false;

        for (String depId : req.dependencies) {
            Requirement depReq = null;
            for (Requirement r : path.requirements) {
                if (r.id.equals(depId)) {
                    depReq = r;
                    break;
                }
            }
            if (depReq != null) {
                if (!isRequirementCompleted(player, data, path.id, depReq)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ponytail: returns true if the player must select a path before doing anything
    public static boolean mustSelectPath(Player player, PlayerData data) {
        if (data != null && data.isDevMode()) return false;
        if (data.getCurrentPath() != null) return false;
        for (PathInfo path : ConfigManager.PATHS) {
            if (!data.getMasteredPaths().contains(path.id) && areDependenciesMastered(player, data, path)) {
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

    public static void updateArmorModifiers(Player player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            checkAndRefreshPlayerData(player, data);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                    int index = slot.getIndex();
                    UUID armorUuid = XamConstants.ARMOR_MODIFIER_UUIDS[index];
                    UUID toughnessUuid = XamConstants.TOUGHNESS_MODIFIER_UUIDS[index];

                    AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
                    AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);

                    if (armorAttr != null) armorAttr.removeModifier(armorUuid);
                    if (toughnessAttr != null) toughnessAttr.removeModifier(toughnessUuid);

                    ItemStack stack = player.getItemBySlot(slot);
                    if (!stack.isEmpty() && !isItemValid(stack, data)) {
                        double armorVal = 0;
                        double toughnessVal = 0;

                        var modifiers = stack.getAttributeModifiers(slot);
                        if (modifiers.containsKey(Attributes.ARMOR)) {
                            for (AttributeModifier modifier : modifiers.get(Attributes.ARMOR)) {
                                armorVal += modifier.getAmount();
                            }
                        }
                        if (modifiers.containsKey(Attributes.ARMOR_TOUGHNESS)) {
                            for (AttributeModifier modifier : modifiers.get(Attributes.ARMOR_TOUGHNESS)) {
                                toughnessVal += modifier.getAmount();
                            }
                        }

                        if (armorVal > 0 && armorAttr != null) {
                            armorAttr.addTransientModifier(new AttributeModifier(armorUuid, "XAM Armor Reduction", -armorVal, AttributeModifier.Operation.ADDITION));
                        }
                        if (toughnessVal > 0 && toughnessAttr != null) {
                            toughnessAttr.addTransientModifier(new AttributeModifier(toughnessUuid, "XAM Toughness Reduction", -toughnessVal, AttributeModifier.Operation.ADDITION));
                        }
                    }
                }
            }
        });
    }

    public static void sync(ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            CompoundTag nbt = new CompoundTag();
            data.saveNBTData(nbt);
            XamNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerDataPacket(nbt));
        });
    }
}
