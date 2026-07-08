package org.xam;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.util.UUID;

public final class XamConstants {
    public static final String MODID = "xam";
    public static final Logger LOGGER = LogUtils.getLogger();

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

    // Pre-computed universal TagKeys to avoid allocation in hot paths
    public static final TagKey<Item> UNIVERSAL_ARMOR_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "universal/armor"));
    public static final TagKey<Item> UNIVERSAL_WEAPONS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "universal/weapons"));
    public static final TagKey<Item> UNIVERSAL_TOOLS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "universal/tools"));

    private XamConstants() {}
}
