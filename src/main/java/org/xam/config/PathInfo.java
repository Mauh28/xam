package org.xam.config;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.List;

public class PathInfo {
    public String id;
    public String name;
    public String mod_id;
    public String icon = "minecraft:writable_book";
    public int min_to_switch = 0;
    public List<String> dependencies = new ArrayList<>();
    public List<Requirement> requirements = new ArrayList<>();
    public String perkEffect = "";
    public int perkAmplifier = 0;

    // Cached TagKeys to avoid allocation in hot paths
    public TagKey<Item> armorTag;
    public TagKey<Item> weaponsTag;
    public TagKey<Item> toolsTag;
}
