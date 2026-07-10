package org.xam.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import org.xam.XamConstants;
import org.xam.network.XamNetwork;
import org.xam.network.NotifyConfigUpdatePacket;
import org.xam.util.PathIcons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<PathInfo> PATHS = new ArrayList<>();
    public static final Map<String, PathInfo> PATHS_MAP = new HashMap<>();
    public static final Map<String, PathInfo> NAMESPACE_TO_PATH = new HashMap<>();
    public static final Set<String> UNIVERSAL_NAMESPACES = new HashSet<>(Arrays.asList("minecraft", "tconstruct"));
    private static long configVersion = 0;

    public static long getConfigVersion() {
        return configVersion;
    }

    public static void setConfigVersion(long version) {
        configVersion = version;
    }

    public static void loadConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("xam_paths.json");
        File file = configPath.toFile();
        if (!file.exists()) {
            createDefaultConfig(file);
        }
        byte[] bytes;
        try {
            bytes = java.nio.file.Files.readAllBytes(configPath);
        } catch (IOException e) {
            XamConstants.LOGGER.error("Failed to read bytes from xam_paths.json", e);
            return;
        }
        String content;
        try {
            java.nio.charset.CharsetDecoder decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
            content = decoder.decode(buf).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            content = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        }
        try {
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            parseJson(json);
        } catch (Exception e) {
            XamConstants.LOGGER.error("Failed to parse xam_paths.json config", e);
        }
    }

    public static void loadConfigFromJson(String jsonString) {
        try {
            JsonObject json = GSON.fromJson(jsonString, JsonObject.class);
            parseJson(json);
        } catch (Exception e) {
            XamConstants.LOGGER.error("Failed to load config from json string", e);
        }
    }

    private static void parseJson(JsonObject json) {
        configVersion++;

        UNIVERSAL_NAMESPACES.clear();
        if (json != null && json.has("universal_namespaces")) {
            JsonArray nsArray = json.getAsJsonArray("universal_namespaces");
            for (int i = 0; i < nsArray.size(); i++) {
                UNIVERSAL_NAMESPACES.add(nsArray.get(i).getAsString());
            }
        } else {
            UNIVERSAL_NAMESPACES.addAll(Arrays.asList("minecraft", "tconstruct"));
        }

        PATHS.clear();
        PATHS_MAP.clear();
        if (json != null && json.has("paths")) {
            JsonArray pathsArray = json.getAsJsonArray("paths");
            for (int i = 0; i < pathsArray.size(); i++) {
                JsonObject pObj = pathsArray.get(i).getAsJsonObject();
                PathInfo info = new PathInfo();
                info.id = pObj.get("id").getAsString();
                info.name = pObj.get("name").getAsString();
                info.mod_id = pObj.has("mod_id") ? pObj.get("mod_id").getAsString() : info.id;
                info.min_to_switch = pObj.has("min_to_switch") ? pObj.get("min_to_switch").getAsInt() : 0;
                info.perkEffect = pObj.has("perk_effect") ? pObj.get("perk_effect").getAsString() : "";
                info.perkAmplifier = pObj.has("perk_amplifier") ? pObj.get("perk_amplifier").getAsInt() : 0;
                if (pObj.has("icon")) {
                    info.icon = pObj.get("icon").getAsString();
                } else {
                    info.icon = PathIcons.getDefaultIconId(info.id);
                }
                info.dependencies = new ArrayList<>();
                if (pObj.has("dependencies")) {
                    JsonArray deps = pObj.getAsJsonArray("dependencies");
                    for (int j = 0; j < deps.size(); j++) {
                        info.dependencies.add(deps.get(j).getAsString());
                    }
                }
                info.armorTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(XamConstants.MODID, info.id + "/armor"));
                info.weaponsTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(XamConstants.MODID, info.id + "/weapons"));
                info.toolsTag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(XamConstants.MODID, info.id + "/tools"));
                info.requirements = new ArrayList<>();
                if (pObj.has("requirements")) {
                    JsonArray reqs = pObj.getAsJsonArray("requirements");
                    for (int j = 0; j < reqs.size(); j++) {
                        JsonObject rObj = reqs.get(j).getAsJsonObject();
                        Requirement req = new Requirement();
                        req.type = rObj.get("type").getAsString();
                        req.id = rObj.get("id").getAsString();
                        req.name = rObj.has("name") ? rObj.get("name").getAsString() : "";
                        req.description = rObj.has("description") ? rObj.get("description").getAsString() : "";
                        req.dependencies = new ArrayList<>();
                        if (rObj.has("dependencies")) {
                            JsonArray reqDeps = rObj.getAsJsonArray("dependencies");
                            for (int k = 0; k < reqDeps.size(); k++) {
                                req.dependencies.add(reqDeps.get(k).getAsString());
                            }
                        }
                        info.requirements.add(req);
                    }
                } else if (pObj.has("mastery_advancements")) {
                    JsonArray advs = pObj.getAsJsonArray("mastery_advancements");
                    for (int j = 0; j < advs.size(); j++) {
                        String advId = advs.get(j).getAsString();
                        String simpleName = advId;
                        if (simpleName.contains(":")) simpleName = simpleName.split(":")[1];
                        if (simpleName.contains("/")) {
                            String[] split = simpleName.split("/");
                            simpleName = split[split.length - 1];
                        }
                        simpleName = simpleName.replace("_", " ");
                        if (!simpleName.isEmpty()) {
                            simpleName = Character.toUpperCase(simpleName.charAt(0)) + simpleName.substring(1);
                        }
                        info.requirements.add(new Requirement("advancement", advId, simpleName, net.minecraft.network.chat.Component.translatable("xam.req_type.advancement", simpleName).getString()));
                    }
                }
                if (info.perkEffect != null && !info.perkEffect.isEmpty()) {
                    ResourceLocation rl = ResourceLocation.tryParse(info.perkEffect);
                    if (rl != null) {
                        info.perkEffectCached = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(rl);
                        if (info.perkEffectCached == null) {
                            XamConstants.LOGGER.warn("Path {} has unknown perkEffect: {}", info.id, info.perkEffect);
                        }
                    }
                }
                PATHS.add(info);
                PATHS_MAP.put(info.id, info);
            }
        }
        // ponytail: O(1) namespace→path lookup for ItemUtils.getPathFromItemTags hot path
        NAMESPACE_TO_PATH.clear();
        for (PathInfo info : PATHS) {
            if (info.mod_id != null && !info.mod_id.isEmpty()) {
                NAMESPACE_TO_PATH.put(info.mod_id, info);
            }
        }
    }

    public static String serializePaths(List<PathInfo> pathsList) {
        JsonObject json = new JsonObject();

        JsonArray nsArray = new JsonArray();
        for (String ns : UNIVERSAL_NAMESPACES) {
            nsArray.add(ns);
        }
        json.add("universal_namespaces", nsArray);

        JsonArray pathsArray = new JsonArray();
        for (PathInfo path : pathsList) {
            JsonObject pObj = new JsonObject();
            pObj.addProperty("id", path.id);
            pObj.addProperty("name", path.name);
            pObj.addProperty("mod_id", path.mod_id);
            pObj.addProperty("icon", path.icon != null ? path.icon : "minecraft:writable_book");
            pObj.addProperty("min_to_switch", path.min_to_switch);
            pObj.addProperty("perk_effect", path.perkEffect != null ? path.perkEffect : "");
            pObj.addProperty("perk_amplifier", path.perkAmplifier);
            JsonArray depsArray = new JsonArray();
            for (String dep : path.dependencies) {
                depsArray.add(dep);
            }
            pObj.add("dependencies", depsArray);
            JsonArray reqsArray = new JsonArray();
            for (Requirement req : path.requirements) {
                JsonObject rObj = new JsonObject();
                rObj.addProperty("type", req.type);
                rObj.addProperty("id", req.id);
                rObj.addProperty("name", req.name);
                rObj.addProperty("description", req.description);
                JsonArray reqDeps = new JsonArray();
                if (req.dependencies != null) {
                    for (String dep : req.dependencies) {
                        reqDeps.add(dep);
                    }
                }
                rObj.add("dependencies", reqDeps);
                reqsArray.add(rObj);
            }
            pObj.add("requirements", reqsArray);
            pathsArray.add(pObj);
        }
        json.add("paths", pathsArray);
        return GSON.toJson(json);
    }

    public static String getPathsJson() {
        return serializePaths(PATHS);
    }

    // ponytail: serialize concurrent config saves
    private static final Object CONFIG_WRITE_LOCK = new Object();

    public static void saveConfigFromServer(net.minecraft.server.MinecraftServer server, String jsonString) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            synchronized (CONFIG_WRITE_LOCK) {
                Path configPath = FMLPaths.CONFIGDIR.get().resolve("xam_paths.json");
                try {
                    java.nio.file.Files.createDirectories(configPath.getParent());
                    try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(configPath, java.nio.charset.StandardCharsets.UTF_8)) {
                        writer.write(jsonString);
                    }
                    if (server != null) {
                        server.execute(() -> {
                            loadConfigFromJson(jsonString);
                            XamNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new NotifyConfigUpdatePacket(getConfigVersion()));
                        });
                    }
                } catch (IOException e) {
                    XamConstants.LOGGER.error("Failed to save paths config on server asynchronously", e);
                }
            }
        });
    }

    private static void createDefaultConfig(File file) {
        try {
            file.getParentFile().mkdirs();
            JsonObject defaultJson = new JsonObject();

            JsonArray nsArray = new JsonArray();
            nsArray.add("minecraft");
            nsArray.add("tconstruct");
            defaultJson.add("universal_namespaces", nsArray);

            JsonArray pathsArray = new JsonArray();

            JsonObject botania = new JsonObject();
            botania.addProperty("id", "botania");
            botania.addProperty("name", "xam.default.botania.name");
            botania.addProperty("mod_id", "botania");
            JsonArray botaniaReqs = new JsonArray();

            JsonObject r1 = new JsonObject();
            r1.addProperty("type", "advancement");
            r1.addProperty("id", "botania:main/rune_pickup");
            r1.addProperty("name", "xam.default.botania.req1.name");
            r1.addProperty("description", "xam.default.botania.req1.description");
            botaniaReqs.add(r1);

            JsonObject r2 = new JsonObject();
            r2.addProperty("type", "advancement");
            r2.addProperty("id", "botania:main/elf_portal_open");
            r2.addProperty("name", "xam.default.botania.req2.name");
            r2.addProperty("description", "xam.default.botania.req2.description");
            botaniaReqs.add(r2);

            botania.add("requirements", botaniaReqs);

            JsonObject mekanism = new JsonObject();
            mekanism.addProperty("id", "mekanism");
            mekanism.addProperty("name", "xam.default.mekanism.name");
            mekanism.addProperty("mod_id", "mekanism");
            JsonArray mekanismReqs = new JsonArray();

            JsonObject r3 = new JsonObject();
            r3.addProperty("type", "advancement");
            r3.addProperty("id", "mekanism:achievement/elite");
            r3.addProperty("name", "xam.default.mekanism.req1.name");
            r3.addProperty("description", "xam.default.mekanism.req1.description");
            mekanismReqs.add(r3);

            JsonObject r4 = new JsonObject();
            r4.addProperty("type", "advancement");
            r4.addProperty("id", "mekanism:achievement/master");
            r4.addProperty("name", "xam.default.mekanism.req2.name");
            r4.addProperty("description", "xam.default.mekanism.req2.description");
            mekanismReqs.add(r4);

            mekanism.add("requirements", mekanismReqs);

            pathsArray.add(botania);
            pathsArray.add(mekanism);
            defaultJson.add("paths", pathsArray);

            try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(file.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                GSON.toJson(defaultJson, writer);
            }
        } catch (IOException e) {
            XamConstants.LOGGER.error("Failed to create default config xam_paths.json", e);
        }
    }
}
