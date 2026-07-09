package org.xam.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.xam.XamConstants;
import org.xam.compat.CuriosCompat;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerDataProvider;
import org.xam.network.SyncConfigPacket;
import org.xam.network.XamNetwork;
import org.xam.progression.MasteryService;
import org.xam.util.MessageUtils;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = XamConstants.MODID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        MessageUtils.LAST_MAINHAND.remove(uuid);
        MessageUtils.LAST_OFFHAND.remove(uuid);
        MessageUtils.COOLDOWNS.entrySet().removeIf(e -> e.getKey().startsWith(uuid.toString()));
        org.xam.network.PacketRateLimiter.clear(uuid);
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
        }
        try {
            event.getOriginal().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(oldStore -> {
                event.getEntity().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(newStore -> {
                    newStore.copyFrom(oldStore);
                });
            });
        } finally {
            if (event.isWasDeath()) {
                event.getOriginal().invalidateCaps();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MasteryService.sync(player);
            MasteryService.updateArmorModifiers(player);
            String pathsJson = ConfigManager.getPathsJson();
            XamNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncConfigPacket(pathsJson));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MasteryService.sync(player);
            MasteryService.updateArmorModifiers(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MasteryService.sync(player);
            MasteryService.updateArmorModifiers(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            Player player = event.player;
            UUID uuid = player.getUUID();

            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                MasteryService.checkAndRefreshPlayerData(player, data);

                // ponytail: lazy inventory scanning and armor warning checks every 1 second
                if (player.tickCount % 20 == 0) {
                    if (data.getCurrentPath() != null) {
                        PathInfo path = ConfigManager.PATHS_MAP.get(data.getCurrentPath());
                        if (path != null) {
                            for (Requirement req : path.requirements) {
                                if (req.type.equals("collect")) {
                                    ResourceLocation reqRl = ResourceLocation.tryParse(req.id);
                                    if (reqRl != null && MasteryService.hasItem(player, reqRl)) {
                                        MasteryService.checkAndProgressRequirement((ServerPlayer) player, "collect", req.id);
                                    }
                                }
                            }
                        }
                    }

                    // Check armor warning every 1s instead of every tick
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                            ItemStack armorStack = player.getItemBySlot(slot);
                            if (!armorStack.isEmpty() && !MasteryService.isItemValid(armorStack, data)) {
                                MessageUtils.sendWarning(player, Component.translatable("xam.msg.armor_rejected"));
                                break; // Only send one warning per check
                            }
                        }
                    }

                    // Check and eject unallowed curios
                    CuriosCompat.checkAndEjectCurios(player, data);

                    // Apply active perks for mastered paths
                    for (String pathId : data.getMasteredPaths()) {
                        PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
                        if (path != null && path.perkEffectCached != null) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(path.perkEffectCached, 400, path.perkAmplifier, true, false, false));
                        }
                    }
                }

                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                String currentMain = "minecraft:air";
                if (!mainHand.isEmpty()) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(mainHand.getItem());
                    if (rl != null) currentMain = rl.toString();
                }

                String currentOff = "minecraft:air";
                if (!offHand.isEmpty()) {
                    ResourceLocation rl = ForgeRegistries.ITEMS.getKey(offHand.getItem());
                    if (rl != null) currentOff = rl.toString();
                }

                String lastMain = MessageUtils.LAST_MAINHAND.getOrDefault(uuid, "minecraft:air");
                String lastOff = MessageUtils.LAST_OFFHAND.getOrDefault(uuid, "minecraft:air");

                if (!currentMain.equals(lastMain)) {
                    MessageUtils.LAST_MAINHAND.put(uuid, currentMain);
                    if (!mainHand.isEmpty() && !MasteryService.isItemValid(mainHand, data)) {
                        MessageUtils.sendItemWarning(player, mainHand);
                    }
                }

                if (!currentOff.equals(lastOff)) {
                    MessageUtils.LAST_OFFHAND.put(uuid, currentOff);
                    if (!offHand.isEmpty() && !MasteryService.isItemValid(offHand, data)) {
                        MessageUtils.sendItemWarning(player, offHand);
                    }
                }
            });
        }
    }
}
