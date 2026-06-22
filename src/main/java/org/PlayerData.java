package org;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.ArrayList;
import java.util.List;

@AutoRegisterCapability
public class PlayerData {
    private String currentPath = null;
    private final List<String> masteredPaths = new ArrayList<>();
    private boolean initialized = false;

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public List<String> getMasteredPaths() {
        return masteredPaths;
    }

    public void addMasteredPath(String path) {
        if (!masteredPaths.contains(path)) {
            masteredPaths.add(path);
        }
    }

    public void copyFrom(PlayerData source) {
        this.currentPath = source.currentPath;
        this.masteredPaths.clear();
        this.masteredPaths.addAll(source.masteredPaths);
    }

    public void saveNBTData(CompoundTag nbt) {
        if (currentPath != null) {
            nbt.putString("currentPath", currentPath);
        }
        ListTag masteredTag = new ListTag();
        for (String path : masteredPaths) {
            masteredTag.add(StringTag.valueOf(path));
        }
        nbt.put("masteredPaths", masteredTag);
    }

    public void loadNBTData(CompoundTag nbt) {
        currentPath = nbt.contains("currentPath", Tag.TAG_STRING) ? nbt.getString("currentPath") : null;
        masteredPaths.clear();
        ListTag masteredTag = nbt.getList("masteredPaths", Tag.TAG_STRING);
        for (int i = 0; i < masteredTag.size(); i++) {
            masteredPaths.add(masteredTag.getString(i));
        }
    }
}
