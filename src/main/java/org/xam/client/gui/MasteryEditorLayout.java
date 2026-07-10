package org.xam.client.gui;

/**
 * Precomputed pixel coordinates for MasteryEditorScreen.
 * Computed once in init() based on screen dimensions; immutable afterwards.
 */
public final class MasteryEditorLayout {
    public final boolean isNarrow;
    public final int iconX, iconY;
    public final int titleX, titleY, titleW;
    public final int modX, modY, modEditW, browseX;
    public final int depsX, secondY, depsW, depsBtnX;
    public final int minX, minY, minW;
    public final int metadataFrameH;
    public final int reqTitleY;

    // Sidebar dimensions (needed by render and mouseClicked)
    public final int sidebarW;
    public final int editorX;
    public final int editorW;

    public MasteryEditorLayout(int containerX, int containerY, int containerW, int bodyY, int bodyH) {
        this.sidebarW = containerW < 450 ? 95 : (int) (containerW * 0.25);
        this.editorX = containerX + sidebarW + 2;
        this.editorW = containerW - sidebarW - 4;
        this.isNarrow = editorW < 320;

        int iconW = 20;
        if (!isNarrow) {
            this.iconX = editorX + 20;
            this.iconY = bodyY + 22;

            this.titleX = iconX + iconW + 20;
            int availableW = editorW - 100;
            this.titleW = (int) (availableW * 0.60);
            this.titleY = bodyY + 22;

            int modW = (int) (availableW * 0.40);
            this.modX = titleX + titleW + 10;
            this.modEditW = modW - 25;
            this.modY = bodyY + 22;
            this.browseX = modX + modEditW + 5;

            // Increased vertical spacing to prevent vertical overlaps with upper fields
            this.secondY = bodyY + 58;
            int secondW = editorW - 40;
            this.minW = 120;
            int depsAreaW = secondW - minW - 10;
            this.depsW = depsAreaW - 25;
            this.depsX = editorX + 20;
            this.depsBtnX = depsX + depsW + 5;
            this.minX = depsBtnX + 20 + 10;
            this.minY = secondY;

            this.metadataFrameH = 88;
        } else {
            // Narrow stacked layout with comfortable row gaps
            this.iconX = editorX + 15;
            this.iconY = bodyY + 22;

            this.titleX = iconX + iconW + 20;
            this.titleW = editorW - 70;
            this.titleY = bodyY + 22;

            this.modX = editorX + 15;
            this.modEditW = editorW - 55;
            this.modY = bodyY + 58;
            this.browseX = modX + modEditW + 5;

            this.depsX = editorX + 15;
            this.depsW = editorW - 55;
            this.secondY = bodyY + 94;
            this.depsBtnX = depsX + depsW + 5;

            this.minX = editorX + 15;
            this.minW = editorW - 30;
            this.minY = bodyY + 130;

            this.metadataFrameH = 160;
        }

        this.reqTitleY = bodyY + 10 + metadataFrameH + 10;
    }
}
