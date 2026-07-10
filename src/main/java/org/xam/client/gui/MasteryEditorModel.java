package org.xam.client.gui;

import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable state holder for MasteryEditorScreen.
 * All state mutations go through explicit mutator methods.
 * Read access via getters returning views or copies where appropriate.
 */
public final class MasteryEditorModel {

    public static final class MenuOption {
        public final String label;
        public final Runnable action;
        public final boolean isDanger;

        public MenuOption(String label, Runnable action) {
            this(label, action, false);
        }

        public MenuOption(String label, Runnable action, boolean isDanger) {
            this.label = label;
            this.action = action;
            this.isDanger = isDanger;
        }
    }

    private final List<PathInfo> localPaths = new ArrayList<>();
    private int selectedPathIndex = -1;
    private double scrollY = 0;

    // Context menu state
    private int contextMenuIndex = -1;
    private boolean contextMenuIsBranch = false;
    private int contextMenuX = 0;
    private int contextMenuY = 0;
    private final List<MenuOption> activeMenuOptions = new ArrayList<>();

    // Notification state
    private long saveNotificationTime = 0;
    private String saveNotificationMsg = "";

    public MasteryEditorModel() {
        for (PathInfo path : ConfigManager.PATHS) {
            PathInfo p = new PathInfo();
            p.setId(path.getId());
            p.setName(path.getName());
            p.setModId(path.getModId());
            p.setIcon(path.getIcon());
            p.setMinToSwitch(path.getMinToSwitch());
            p.setPerkEffect(path.getPerkEffect() != null ? path.getPerkEffect() : "");
            p.setPerkAmplifier(path.getPerkAmplifier());
            p.setDependencies(new ArrayList<>(path.getDependencies()));
            p.setRequirements(new ArrayList<>());
            for (Requirement req : path.getRequirements()) {
                Requirement r = new Requirement();
                r.setType(req.getType());
                r.setId(req.getId());
                r.setName(req.getName());
                r.setDescription(req.getDescription());
                r.setDependencies(new ArrayList<>(req.getDependencies()));
                p.addRequirement(r);
            }
            this.localPaths.add(p);
        }

        if (!this.localPaths.isEmpty()) {
            this.selectedPathIndex = 0;
        }
    }

    // === Paths ===
    public List<PathInfo> getPaths() {
        return localPaths;
    }

    public PathInfo getSelectedPath() {
        return (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size())
            ? localPaths.get(selectedPathIndex) : null;
    }

    public int getSelectedPathIndex() {
        return selectedPathIndex;
    }

    public void setSelectedPathIndex(int idx) {
        this.selectedPathIndex = idx;
    }

    public void addPath(PathInfo p) {
        localPaths.add(p);
        selectedPathIndex = localPaths.size() - 1;
    }

    // === Scroll ===
    public double getScrollY() {
        return scrollY;
    }

    public void setScrollY(double v) {
        this.scrollY = v;
    }

    public void addScrollY(double delta) {
        this.scrollY += delta;
    }

    // === Context menu ===
    public int getContextMenuIndex() {
        return contextMenuIndex;
    }

    public boolean isContextMenuOpen() {
        return contextMenuIndex >= 0;
    }

    public boolean isContextMenuBranch() {
        return contextMenuIsBranch;
    }

    public int getContextMenuX() {
        return contextMenuX;
    }

    public int getContextMenuY() {
        return contextMenuY;
    }

    public List<MenuOption> getActiveMenuOptions() {
        return activeMenuOptions;
    }

    public void openContextMenu(int index, boolean isBranch, int x, int y, List<MenuOption> options) {
        this.contextMenuIndex = index;
        this.contextMenuIsBranch = isBranch;
        this.contextMenuX = x;
        this.contextMenuY = y;
        this.activeMenuOptions.clear();
        this.activeMenuOptions.addAll(options);
    }

    public void closeContextMenu() {
        this.contextMenuIndex = -1;
        this.activeMenuOptions.clear();
    }

    // === Notification ===
    public long getSaveNotificationTime() {
        return saveNotificationTime;
    }

    public String getSaveNotificationMsg() {
        return saveNotificationMsg;
    }

    public void showNotification(String msg, boolean isError) {
        this.saveNotificationMsg = (isError ? "✕ " : "✔ ") + msg;
        this.saveNotificationTime = System.currentTimeMillis();
    }

    public void clearNotification() {
        this.saveNotificationTime = 0;
        this.saveNotificationMsg = "";
    }
}
