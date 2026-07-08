package org.xam.data;

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
    private final List<String> startedPaths = new ArrayList<>();
    private final List<String> completedRequirements = new ArrayList<>();
    // ponytail: initialized tracks if selection was opened this session; intentionally not persisted or copied in copyFrom
    private boolean initialized = false;

    // cache fields (ponytail: server cache optimization)
    private String activePathModId = "";
    private long lastConfigVersion = -1;
    private boolean devMode = false;

    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

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
        if (currentPath != null) {
            addStartedPath(currentPath);
        }
    }

    public List<String> getStartedPaths() {
        return startedPaths;
    }

    public void addStartedPath(String path) {
        if (!startedPaths.contains(path)) {
            startedPaths.add(path);
        }
    }

    public List<String> getMasteredPaths() {
        return masteredPaths;
    }

    public void addMasteredPath(String path) {
        if (!masteredPaths.contains(path)) {
            masteredPaths.add(path);
        }
    }

    public List<String> getCompletedRequirements() {
        return completedRequirements;
    }

    public void addCompletedRequirement(String req) {
        if (!completedRequirements.contains(req)) {
            completedRequirements.add(req);
        }
    }

    public void clearCompletedRequirements() {
        completedRequirements.clear();
    }

    public String getActivePathModId() {
        return activePathModId;
    }

    public void setActivePathModId(String activePathModId) {
        this.activePathModId = activePathModId != null ? activePathModId : "";
    }

    public long getLastConfigVersion() {
        return lastConfigVersion;
    }

    public void setLastConfigVersion(long lastConfigVersion) {
        this.lastConfigVersion = lastConfigVersion;
    }

    public void copyFrom(PlayerData source) {
        this.currentPath = source.currentPath;
        this.masteredPaths.clear();
        this.masteredPaths.addAll(source.masteredPaths);
        this.startedPaths.clear();
        this.startedPaths.addAll(source.startedPaths);
        this.completedRequirements.clear();
        this.completedRequirements.addAll(source.completedRequirements);
        this.activePathModId = source.activePathModId;
        this.lastConfigVersion = source.lastConfigVersion;
        this.devMode = source.devMode;
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

        ListTag startedTag = new ListTag();
        for (String path : startedPaths) {
            startedTag.add(StringTag.valueOf(path));
        }
        nbt.put("startedPaths", startedTag);

        ListTag completedTag = new ListTag();
        for (String req : completedRequirements) {
            completedTag.add(StringTag.valueOf(req));
        }
        nbt.put("completedRequirements", completedTag);

        nbt.putString("activePathModId", activePathModId);
        nbt.putLong("lastConfigVersion", lastConfigVersion);
        nbt.putBoolean("devMode", devMode);
    }

    public void loadNBTData(CompoundTag nbt) {
        currentPath = nbt.contains("currentPath", Tag.TAG_STRING) ? nbt.getString("currentPath") : null;
        if (currentPath != null) {
            addStartedPath(currentPath);
        }
        masteredPaths.clear();
        ListTag masteredTag = nbt.getList("masteredPaths", Tag.TAG_STRING);
        for (int i = 0; i < masteredTag.size(); i++) {
            masteredPaths.add(masteredTag.getString(i));
        }

        startedPaths.clear();
        if (nbt.contains("startedPaths", Tag.TAG_LIST)) {
            ListTag startedTag = nbt.getList("startedPaths", Tag.TAG_STRING);
            for (int i = 0; i < startedTag.size(); i++) {
                startedPaths.add(startedTag.getString(i));
            }
        }

        completedRequirements.clear();
        if (nbt.contains("completedRequirements", Tag.TAG_LIST)) {
            ListTag completedTag = nbt.getList("completedRequirements", Tag.TAG_STRING);
            for (int i = 0; i < completedTag.size(); i++) {
                completedRequirements.add(completedTag.getString(i));
            }
        }

        activePathModId = nbt.contains("activePathModId", Tag.TAG_STRING) ? nbt.getString("activePathModId") : "";
        lastConfigVersion = nbt.contains("lastConfigVersion", Tag.TAG_LONG) ? nbt.getLong("lastConfigVersion") : -1;
        devMode = nbt.contains("devMode", Tag.TAG_BYTE) && nbt.getBoolean("devMode");
    }
}
