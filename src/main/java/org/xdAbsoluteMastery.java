package org;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

@Mod(xdAbsoluteMastery.MODID)
public class xdAbsoluteMastery {
    public static final String MODID = "xam";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Network Channel Setup
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // Armor Reduction UUIDs
    public static final UUID[] ARMOR_MODIFIER_UUIDS = new UUID[]{
            UUID.fromString("845224CC-73B1-4C53-96C5-DFD25A0B2C9E"), // FEET
            UUID.fromString("D2DE1849-B7C0-4E5F-B73F-1349910D40CA"), // LEGS
            UUID.fromString("9F3D476D-C118-4544-8A0A-CE3B7DBF4B6D"), // CHEST
            UUID.fromString("2AD3E313-2415-4D72-B582-7EF31557B6C8")  // HEAD
    };

    public static final UUID[] TOUGHNESS_MODIFIER_UUIDS = new UUID[]{
            UUID.fromString("21A2DFCE-90F6-427E-9781-D9F57D2C04B3"), // FEET
            UUID.fromString("B2680E51-EA9A-4E2F-A08D-CB968DE20F3A"), // LEGS
            UUID.fromString("75A88B33-9118-40F1-B73F-941E66CF1433"), // CHEST
            UUID.fromString("35BF491C-032E-4D2A-936A-E393F421C2B0")  // HEAD
    };

    // Message Cooldown Manager (3 seconds)
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, String> LAST_MAINHAND = new HashMap<>();
    private static final Map<UUID, String> LAST_OFFHAND = new HashMap<>();

    public xdAbsoluteMastery() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing xd Absolute Mastery (XAM)");
        ConfigManager.loadConfig();

        // Register Network Packets
        int packetId = 0;
        CHANNEL.registerMessage(packetId++, SyncPlayerDataPacket.class,
                SyncPlayerDataPacket::encode, SyncPlayerDataPacket::decode, SyncPlayerDataPacket::handle);
        CHANNEL.registerMessage(packetId++, SelectPathPacket.class,
                SelectPathPacket::encode, SelectPathPacket::decode, SelectPathPacket::handle);
    }

    // --- Helper Logic ---

    public static void sendWarning(Player player, String message) {
        long now = System.currentTimeMillis();
        String key = player.getUUID().toString() + "_" + message;
        Long last = COOLDOWNS.get(key);
        if (last == null || (now - last) >= 5000) {
            COOLDOWNS.put(key, now);
            player.sendSystemMessage(Component.literal(message).withStyle(net.minecraft.ChatFormatting.RED));
        }
    }

    public static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.ProjectileWeaponItem
                || item instanceof net.minecraft.world.item.TridentItem
                || ForgeRegistries.ITEMS.getKey(item).getPath().contains("sword")
                || ForgeRegistries.ITEMS.getKey(item).getPath().contains("bow");
    }

    public static void sendItemWarning(Player player, ItemStack stack) {
        if (isWeapon(stack)) {
            sendWarning(player, "Esta arma no tiene efecto bajo tu maestría");
        } else {
            sendWarning(player, "Esta herramienta no tiene efecto bajo tu maestría");
        }
    }

    public static boolean isUniversal(ItemStack stack) {
        if (stack.isEmpty()) return true;
        Item item = stack.getItem();
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        if (rl == null) return false;

        // Minecraft vanilla is always universal (ponytail: native platforms and simple checks)
        // ponytail: temporary simulation overrides for testing
        if (rl.getNamespace().equals("minecraft")) {
            if (rl.getPath().equals("wooden_hoe") || rl.getPath().equals("leather_chestplate") || rl.getPath().equals("wooden_sword")) {
                return false;
            }
            return true;
        }

        // Tinkers' Construct check (tconstruct namespace or ModifiableItem class)
        if (rl.getNamespace().equals("tconstruct") || isTinkersItem(item)) {
            return true;
        }

        // xam:universal/armor, weapons, tools
        if (stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(MODID, "universal/armor")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(MODID, "universal/weapons")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(MODID, "universal/tools")))) {
            return true;
        }

        return false;
    }

    private static boolean isTinkersItem(Item item) {
        // ponytail: using reflection to check ModifiableItem without adding tconstruct compile dependency
        try {
            Class<?> clazz = Class.forName("slimeknights.tconstruct.library.tools.item.ModifiableItem");
            if (clazz.isInstance(item)) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {}
        return false;
    }

    public static String getPathFromItemTags(ItemStack stack) {
        if (stack.isEmpty()) return null;
        // ponytail: temporary simulation overrides for testing
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl != null && rl.getNamespace().equals("minecraft")) {
            if (rl.getPath().equals("wooden_hoe") || rl.getPath().equals("leather_chestplate") || rl.getPath().equals("wooden_sword")) {
                return "mekanism"; // Path 2 in default config
            }
        }

        for (ConfigManager.PathInfo path : ConfigManager.PATHS) {
            if (stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(MODID, path.id + "/armor")))
                    || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(MODID, path.id + "/weapons")))
                    || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation(MODID, path.id + "/tools")))) {
                return path.id;
            }
        }
        return null;
    }

    public static boolean isItemValid(ItemStack stack, PlayerData data) {
        if (stack.isEmpty()) return true;
        if (isUniversal(stack)) return true;
        String itemPath = getPathFromItemTags(stack);
        if (itemPath == null) return false;
        if (itemPath.equals(data.getCurrentPath())) return true;
        return data.getMasteredPaths().contains(itemPath);
    }

    public static void updateArmorModifiers(Player player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                    int index = slot.getIndex();
                    UUID armorUuid = ARMOR_MODIFIER_UUIDS[index];
                    UUID toughnessUuid = TOUGHNESS_MODIFIER_UUIDS[index];

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
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerDataPacket(nbt));
        });
    }

    // --- Forge Events Handler Class ---

    public static class ForgeEvents {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                if (!event.getObject().getCapability(PlayerDataProvider.PLAYER_DATA).isPresent()) {
                    event.addCapability(new ResourceLocation(MODID, "properties"), new PlayerDataProvider());
                }
            }
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
                sync(player);
                updateArmorModifiers(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                sync(player);
                updateArmorModifiers(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                sync(player);
                updateArmorModifiers(player);
            }
        }

        @SubscribeEvent
        public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
            if (event.getEntity() instanceof Player player) {
                updateArmorModifiers(player);
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof Player player) {
                ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty()) {
                    player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                        if (!isItemValid(mainHand, data)) {
                            event.setAmount(1.0f);
                            sendItemWarning(player, mainHand);
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    if (!isItemValid(mainHand, data)) {
                        event.setNewSpeed(0.0f);
                        sendItemWarning(player, mainHand);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            Player player = event.getPlayer();
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    if (!isItemValid(mainHand, data)) {
                        event.setCanceled(true);
                        sendItemWarning(player, mainHand);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
            Player player = event.getEntity();
            ItemStack stack = event.getItemStack();
            if (!stack.isEmpty()) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    if (!isItemValid(stack, data)) {
                        event.setCanceled(true);
                        sendItemWarning(player, stack);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
            Player player = event.getEntity();
            ItemStack stack = event.getItemStack();
            if (!stack.isEmpty()) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    if (!isItemValid(stack, data)) {
                        event.setCanceled(true);
                        sendItemWarning(player, stack);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onEntityInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
            Player player = event.getEntity();
            ItemStack stack = event.getItemStack();
            if (!stack.isEmpty()) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    if (!isItemValid(stack, data)) {
                        event.setCanceled(true);
                        sendItemWarning(player, stack);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
                Player player = event.player;
                UUID uuid = player.getUUID();

                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    // Check armor warning continuously (repeats autonomously every 5s)
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                            ItemStack armorStack = player.getItemBySlot(slot);
                            if (!armorStack.isEmpty() && !isItemValid(armorStack, data)) {
                                sendWarning(player, "Tu maestría rechaza esta armadura, no te protegerá");
                                break; // Only send one warning per tick
                            }
                        }
                    }
                    ItemStack mainHand = player.getMainHandItem();
                    ItemStack offHand = player.getOffhandItem();

                    String currentMain = ForgeRegistries.ITEMS.getKey(mainHand.getItem()).toString();
                    if (mainHand.isEmpty()) currentMain = "minecraft:air";

                    String currentOff = ForgeRegistries.ITEMS.getKey(offHand.getItem()).toString();
                    if (offHand.isEmpty()) currentOff = "minecraft:air";

                    String lastMain = LAST_MAINHAND.getOrDefault(uuid, "minecraft:air");
                    String lastOff = LAST_OFFHAND.getOrDefault(uuid, "minecraft:air");

                    if (!currentMain.equals(lastMain)) {
                        LAST_MAINHAND.put(uuid, currentMain);
                        if (!mainHand.isEmpty() && !isItemValid(mainHand, data)) {
                            sendItemWarning(player, mainHand);
                        }
                    }

                    if (!currentOff.equals(lastOff)) {
                        LAST_OFFHAND.put(uuid, currentOff);
                        if (!offHand.isEmpty() && !isItemValid(offHand, data)) {
                            sendItemWarning(player, offHand);
                        }
                    }

                    for (InteractionHand hand : InteractionHand.values()) {
                        ItemStack stack = player.getItemInHand(hand);
                        if (!stack.isEmpty() && stack.isDamageableItem()) {
                            if (!isItemValid(stack, data)) {
                                CompoundTag tag = stack.getOrCreateTag();
                                if (tag.contains("XamPrevDamage")) {
                                    int prevDamage = tag.getInt("XamPrevDamage");
                                    if (stack.getDamageValue() > prevDamage) {
                                        stack.setDamageValue(prevDamage);
                                    }
                                }
                                stack.getOrCreateTag().putInt("XamPrevDamage", stack.getDamageValue());
                            } else {
                                if (stack.hasTag() && stack.getTag().contains("XamPrevDamage")) {
                                    stack.getTag().remove("XamPrevDamage");
                                }
                            }
                        }
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onAdvancement(AdvancementEvent event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;

            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                String currentPath = data.getCurrentPath();
                if (currentPath == null) return;

                ConfigManager.PathInfo pathInfo = null;
                for (ConfigManager.PathInfo path : ConfigManager.PATHS) {
                    if (path.id.equals(currentPath)) {
                        pathInfo = path;
                        break;
                    }
                }

                if (pathInfo == null) return;

                if (player instanceof ServerPlayer serverPlayer) {
                    boolean completedAll = true;
                    net.minecraft.server.PlayerAdvancements playerAdvancements = serverPlayer.getAdvancements();

                    for (String advIdStr : pathInfo.mastery_advancements) {
                        ResourceLocation resLoc = new ResourceLocation(advIdStr);
                        net.minecraft.advancements.Advancement adv = serverPlayer.server.getAdvancements().getAdvancement(resLoc);
                        if (adv == null) {
                            completedAll = false;
                            break;
                        }
                        if (!playerAdvancements.getOrStartProgress(adv).isDone()) {
                            completedAll = false;
                            break;
                        }
                    }

                    if (completedAll) {
                        data.addMasteredPath(currentPath);
                        data.setCurrentPath(null);
                        sync(serverPlayer);
                        updateArmorModifiers(serverPlayer);
                        serverPlayer.sendSystemMessage(Component.literal("¡Has dominado " + pathInfo.name + "! Ahora puedes elegir un nuevo camino."));
                    }
                }
            });
        }
    }

    // --- Network Packets ---

    public static class SyncPlayerDataPacket {
        private final CompoundTag nbt;

        public SyncPlayerDataPacket(CompoundTag nbt) {
            this.nbt = nbt;
        }

        public static void encode(SyncPlayerDataPacket pkt, FriendlyByteBuf buf) {
            buf.writeNbt(pkt.nbt);
        }

        public static SyncPlayerDataPacket decode(FriendlyByteBuf buf) {
            return new SyncPlayerDataPacket(buf.readNbt());
        }

        public static void handle(SyncPlayerDataPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSync(pkt.nbt));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SelectPathPacket {
        private final String pathId;

        public SelectPathPacket(String pathId) {
            this.pathId = pathId;
        }

        public static void encode(SelectPathPacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.pathId);
        }

        public static SelectPathPacket decode(FriendlyByteBuf buf) {
            return new SelectPathPacket(buf.readUtf());
        }

        public static void handle(SelectPathPacket pkt, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                        data.setCurrentPath(pkt.pathId);
                        sync(player);
                        updateArmorModifiers(player);
                    });
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // --- Config Manager ---

    public static class ConfigManager {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        public static final List<PathInfo> PATHS = new ArrayList<>();

        public static class PathInfo {
            public String id;
            public String name;
            public List<String> mastery_advancements = new ArrayList<>();
        }

        public static void loadConfig() {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve("xam_paths.json");
            File file = configPath.toFile();
            if (!file.exists()) {
                createDefaultConfig(file);
            }
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                PATHS.clear();
                if (json != null && json.has("paths")) {
                    JsonArray pathsArray = json.getAsJsonArray("paths");
                    for (int i = 0; i < pathsArray.size(); i++) {
                        JsonObject pObj = pathsArray.get(i).getAsJsonObject();
                        PathInfo info = new PathInfo();
                        info.id = pObj.get("id").getAsString();
                        info.name = pObj.get("name").getAsString();
                        info.mastery_advancements = new ArrayList<>();
                        if (pObj.has("mastery_advancements")) {
                            JsonArray advs = pObj.getAsJsonArray("mastery_advancements");
                            for (int j = 0; j < advs.size(); j++) {
                                info.mastery_advancements.add(advs.get(j).getAsString());
                            }
                        }
                        PATHS.add(info);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load xam_paths.json config", e);
            }
        }

        private static void createDefaultConfig(File file) {
            try {
                file.getParentFile().mkdirs();
                JsonObject defaultJson = new JsonObject();
                JsonArray pathsArray = new JsonArray();

                JsonObject botania = new JsonObject();
                botania.addProperty("id", "botania");
                botania.addProperty("name", "El Camino de la Naturaleza");
                JsonArray botaniaAdvs = new JsonArray();
                botaniaAdvs.add("botania:main/rune_pickup");
                botaniaAdvs.add("botania:main/elf_portal_open");
                botania.add("mastery_advancements", botaniaAdvs);

                JsonObject mekanism = new JsonObject();
                mekanism.addProperty("id", "mekanism");
                mekanism.addProperty("name", "El Camino Tecnológico");
                JsonArray mekanismAdvs = new JsonArray();
                mekanismAdvs.add("mekanism:achievement/elite");
                mekanismAdvs.add("mekanism:achievement/master");
                mekanism.add("mastery_advancements", mekanismAdvs);

                pathsArray.add(botania);
                pathsArray.add(mekanism);
                defaultJson.add("paths", pathsArray);

                try (FileWriter writer = new FileWriter(file)) {
                    GSON.toJson(defaultJson, writer);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create default config xam_paths.json", e);
            }
        }
    }

    // --- Client Only Packet Handler to avoid Dedicated Server crashes ---

    public static class ClientPacketHandler {
        public static void handleSync(CompoundTag nbt) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    boolean isFirstSync = !data.isInitialized();
                    List<String> oldMastered = new ArrayList<>(data.getMasteredPaths());
                    data.loadNBTData(nbt);
                    data.setInitialized(true);

                    if (!isFirstSync) {
                        List<String> newMastered = data.getMasteredPaths();
                        for (String pathId : newMastered) {
                            if (!oldMastered.contains(pathId)) {
                                // Find the name of the mastered path
                                String pathName = pathId;
                                for (ConfigManager.PathInfo path : ConfigManager.PATHS) {
                                    if (path.id.equals(pathId)) {
                                        pathName = path.name;
                                        break;
                                    }
                                }
                                // Show premium client-side toast notification
                                net.minecraft.client.gui.components.toasts.SystemToast.add(
                                        mc.getToasts(),
                                        net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds.TUTORIAL_HINT,
                                        Component.literal("¡Maestría Completada!"),
                                        Component.literal("Has dominado: " + pathName)
                                );
                                break;
                            }
                        }
                    }
                });
            }
        }
    }

    // --- Client Only Keybinds & Suppressions ---

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        public static final net.minecraft.client.KeyMapping MASTERY_KEY = new net.minecraft.client.KeyMapping(
                "key.xam.mastery",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_M, // Key: 'M'
                "key.categories.xam"
        );

        @SubscribeEvent
        public static void registerKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(MASTERY_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    // Check if they pressed the key
                    if (ClientModEvents.MASTERY_KEY.consumeClick()) {
                        mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                            mc.setScreen(new MasteryInfoScreen(data));
                        });
                    }

                    // Key suppression check for invalid items
                    mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                        ItemStack mainHand = mc.player.getMainHandItem();
                        ItemStack offHand = mc.player.getOffhandItem();
                        boolean mainInvalid = !mainHand.isEmpty() && !isItemValid(mainHand, data);
                        boolean offInvalid = !offHand.isEmpty() && !isItemValid(offHand, data);

                        if (mainInvalid || offInvalid) {
                            for (net.minecraft.client.KeyMapping keyMapping : mc.options.keyMappings) {
                                if (isInteractionKey(keyMapping, mc.options)) {
                                    while (keyMapping.consumeClick()) {
                                        // Drain all clicks
                                    }
                                    keyMapping.setDown(false);
                                }
                            }
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onScreenInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inventoryScreen) {
                int leftPos = inventoryScreen.getGuiLeft();
                int topPos = inventoryScreen.getGuiTop();

                int btnX = leftPos - 22;
                int btnY = topPos + 8;

                boolean collided;
                do {
                    collided = false;
                    for (net.minecraft.client.gui.components.events.GuiEventListener listener : event.getListenersList()) {
                        if (listener instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                            if (widget.getX() == btnX && widget.getY() == btnY) {
                                btnY += 22;
                                collided = true;
                                break;
                            }
                        }
                    }
                } while (collided);

                net.minecraft.client.gui.components.Button btn = new net.minecraft.client.gui.components.Button(
                        btnX, btnY, 20, 20, Component.empty(),
                        b -> {
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                            if (mc.player != null) {
                                mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                                    mc.setScreen(new MasteryInfoScreen(data));
                                });
                            }
                        },
                        supplier -> supplier.get()
                ) {
                    @Override
                    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                        guiGraphics.renderFakeItem(new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK), this.getX() + 2, this.getY() + 2);
                    }
                };

                event.addListener(btn);
            }
        }

        private static boolean isInteractionKey(net.minecraft.client.KeyMapping key, net.minecraft.client.Options options) {
            return key != options.keyUp
                    && key != options.keyDown
                    && key != options.keyLeft
                    && key != options.keyRight
                    && key != options.keyJump
                    && key != options.keyShift
                    && key != options.keySprint
                    && key != options.keyInventory
                    && key != options.keyChat
                    && key != options.keyCommand
                    && key != options.keyPlayerList
                    && key != options.keyScreenshot
                    && key != options.keySmoothCamera
                    && key != options.keyFullscreen
                    && key != options.keySpectatorOutlines;
        }
    }
}
