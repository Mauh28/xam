package org.xam.config;

import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.List;

public class PathInfo {
    private String id;
    private String name;
    private String mod_id;
    private String icon = "minecraft:writable_book";
    private int min_to_switch = 0;
    private List<String> dependencies = new ArrayList<>();
    private List<Requirement> requirements = new ArrayList<>();
    private String perkEffect = "";
    private int perkAmplifier = 0;

    // Cached TagKeys to avoid allocation in hot paths
    private TagKey<Item> armorTag;
    private TagKey<Item> weaponsTag;
    private TagKey<Item> toolsTag;

    // Cache MobEffect to avoid registry lookups in tick loops
    private transient MobEffect perkEffectCached;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModId() {
        return mod_id;
    }

    public void setModId(String mod_id) {
        this.mod_id = mod_id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getMinToSwitch() {
        return min_to_switch;
    }

    public void setMinToSwitch(int min_to_switch) {
        this.min_to_switch = min_to_switch;
    }

    public List<String> getDependencies() {
        return java.util.Collections.unmodifiableList(dependencies);
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
    }

    public void addDependency(String dependency) {
        if (dependency != null && !this.dependencies.contains(dependency)) {
            this.dependencies.add(dependency);
        }
    }

    public void removeDependency(String dependency) {
        this.dependencies.remove(dependency);
    }

    public void clearDependencies() {
        this.dependencies.clear();
    }

    public List<Requirement> getRequirements() {
        return java.util.Collections.unmodifiableList(requirements);
    }

    public void setRequirements(List<Requirement> requirements) {
        this.requirements = requirements != null ? new ArrayList<>(requirements) : new ArrayList<>();
    }

    public void addRequirement(Requirement requirement) {
        if (requirement != null && !this.requirements.contains(requirement)) {
            this.requirements.add(requirement);
        }
    }

    public void removeRequirement(Requirement requirement) {
        this.requirements.remove(requirement);
    }

    public void removeRequirementAtIndex(int index) {
        if (index >= 0 && index < this.requirements.size()) {
            this.requirements.remove(index);
        }
    }

    public void clearRequirements() {
        this.requirements.clear();
    }

    public String getPerkEffect() {
        return perkEffect;
    }

    public void setPerkEffect(String perkEffect) {
        this.perkEffect = perkEffect;
    }

    public int getPerkAmplifier() {
        return perkAmplifier;
    }

    public void setPerkAmplifier(int perkAmplifier) {
        this.perkAmplifier = perkAmplifier;
    }

    public TagKey<Item> getArmorTag() {
        return armorTag;
    }

    public void setArmorTag(TagKey<Item> armorTag) {
        this.armorTag = armorTag;
    }

    public TagKey<Item> getWeaponsTag() {
        return weaponsTag;
    }

    public void setWeaponsTag(TagKey<Item> weaponsTag) {
        this.weaponsTag = weaponsTag;
    }

    public TagKey<Item> getToolsTag() {
        return toolsTag;
    }

    public void setToolsTag(TagKey<Item> toolsTag) {
        this.toolsTag = toolsTag;
    }

    public MobEffect getPerkEffectCached() {
        return perkEffectCached;
    }

    public void setPerkEffectCached(MobEffect perkEffectCached) {
        this.perkEffectCached = perkEffectCached;
    }
}
