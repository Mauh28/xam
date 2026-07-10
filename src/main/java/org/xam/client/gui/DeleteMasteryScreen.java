package org.xam.client.gui;

import org.xam.config.PathInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

// --- Extracted from MasteryEditorScreen nested class (issue #17 step 2/5) ---

public class DeleteMasteryScreen extends AbstractPickerScreen<PathInfo> {
    private final MasteryEditorScreen editor;
    private final List<String> selectedIds = new ArrayList<>();
    private boolean isDraggingGridScrollbar = false;

    public DeleteMasteryScreen(MasteryEditorScreen editor) {
        super(editor, Component.translatable("xam.screen.mastery_editor.delete_mastery.title"), null);
        this.editor = editor;
    }

    @Override
    protected void init() {
        super.init();
        this.entryHeight = 44;
        boolean showFilter = shouldShowNamespaceFilter() && containerH >= 220;
        int listHeight = bodyH - (showFilter ? 70 : 45);
        this.maxVisible = Math.max(2, listHeight / entryHeight);
    }

    @Override
    protected boolean shouldShowNamespaceFilter() { return false; }

    @Override
    protected void populateEntries() {
        this.allEntries.addAll(editor.localPaths);
    }

    @Override
    protected void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        for (PathInfo p : this.allEntries) {
            if (p.name.toLowerCase().contains(q) || p.id.toLowerCase().contains(q)) {
                this.filteredEntries.add(p);
            }
        }
    }

    @Override
    protected void renderEntry(GuiGraphics g, PathInfo p, int x, int y, int index, boolean hovered) {
        // No-op because we completely override render
    }

    @Override
    protected void onClickEntry(PathInfo p) {
        // No-op because we completely override mouseClicked
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Temporarily clear filteredEntries to prevent super.render from drawing the default single column list
        java.util.List<PathInfo> temp = new java.util.ArrayList<>(this.filteredEntries);
        this.filteredEntries.clear();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.filteredEntries.addAll(temp);

        int panelX = containerX;
        int startY = getListStartY();
        int listWidth = containerW - 40;
        int colWidth = (listWidth - 6) / 2;
        int gap = 6;
        int cardH = entryHeight - 4; // 40px

        // Render double column list
        for (int i = 0; i < maxVisible; i++) {
            for (int col = 0; col < 2; col++) {
                int entryIndex = scrollOffset * 2 + i * 2 + col;
                if (entryIndex >= filteredEntries.size()) break;

                PathInfo p = filteredEntries.get(entryIndex);
                int entryX = panelX + 20 + col * (colWidth + gap);
                int entryY = startY + i * entryHeight;

                boolean hovered = mouseX >= entryX && mouseX < entryX + colWidth && mouseY >= entryY && mouseY < entryY + cardH;
                boolean selected = selectedIds.contains(p.id);

                // Draw card background
                int bg = selected ? 0xFF2A1515 : (hovered ? BUTTON_HOVER_BG : PANEL_INNER_BG);
                int border = selected ? 0xFFFF5555 : (hovered ? BUTTON_HOVER_BORDER : WARM_BORDER);
                drawFlatPanel(guiGraphics, entryX, entryY, colWidth, cardH, bg, border);

                // Draw custom Checkbox
                int cbX = entryX + 8;
                int cbY = entryY + (cardH - 12) / 2;
                drawFlatPanel(guiGraphics, cbX, cbY, 12, 12, 0xFF140F0D, selected ? 0xFFFF5555 : 0xFF2C221D);
                if (selected) {
                    guiGraphics.drawString(this.font, "✔", cbX + 2, cbY + 2, 0xFFFF5555, false);
                }

                // Render Icon
                net.minecraft.world.item.ItemStack iconStack = net.minecraft.world.item.ItemStack.EMPTY;
                if (p.icon != null) {
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(p.icon));
                    if (item != null) iconStack = new net.minecraft.world.item.ItemStack(item);
                }
                if (iconStack.isEmpty()) iconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                guiGraphics.renderFakeItem(iconStack, entryX + 26, entryY + (cardH - 16) / 2);

                // Name + mod/id parenthesized
                int textX = entryX + 46;
                int textY = entryY + (cardH - 8) / 2;
                int labelW = colWidth - 46 - 10;
                String label = p.name + " (" + p.id + ")";
                if (this.font.width(label) > labelW) {
                    label = this.font.plainSubstrByWidth(label, labelW - 8) + "...";
                }
                guiGraphics.drawString(this.font, label, textX, textY, selected ? 0xFFFF5555 : (hovered ? TEXT_PRIMARY : TEXT_SECONDARY), false);
            }
        }

        // Render custom scrollbar based on rows count
        int totalRows = (filteredEntries.size() + 1) / 2;
        if (totalRows > maxVisible) {
            int scrollbarX = panelX + containerW - 15;
            int scrollbarY = startY;
            int scrollbarHeight = maxVisible * entryHeight;

            // Track
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0xFF2A201C);

            // Thumb
            float fraction = (float) scrollOffset / (totalRows - maxVisible);
            int thumbHeight = Math.max(15, (int) (((float) maxVisible / totalRows) * scrollbarHeight));
            int thumbY = scrollbarY + (int) (fraction * (scrollbarHeight - thumbHeight));

            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, COLOR_COPPER);
        }
    }

    private void updateGridScrollFromMouse(double mouseY, int startY, int scrollbarHeight, int thumbHeight) {
        int totalRows = (filteredEntries.size() + 1) / 2;
        int maxScrollOffset = totalRows - maxVisible;
        if (maxScrollOffset <= 0) return;

        double relativeY = mouseY - startY - (thumbHeight / 2.0);
        double trackLength = scrollbarHeight - thumbHeight;
        double pct = Math.max(0.0, Math.min(1.0, relativeY / trackLength));
        this.scrollOffset = (int) Math.round(pct * maxScrollOffset);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingGridScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int totalRows = (filteredEntries.size() + 1) / 2;
        if (this.isDraggingGridScrollbar && button == 0 && totalRows > maxVisible) {
            int startY = getListStartY();
            int scrollbarHeight = maxVisible * entryHeight;
            int thumbHeight = Math.max(15, (int) (((float) maxVisible / totalRows) * scrollbarHeight));
            updateGridScrollFromMouse(mouseY, startY, scrollbarHeight, thumbHeight);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnW = 100, btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        drawFlatButton(graphics, startX, btnY, btnW, btnH, Component.translatable("xam.editor.cancel").getString(), mouseX, mouseY, true);

        boolean hasSelection = !selectedIds.isEmpty();
        String deleteText = Component.translatable("xam.screen.mastery_editor.delete_mastery.btn_delete", selectedIds.size()).getString();
        // Borrar button uses danger hover style
        boolean delHovered = hasSelection && mouseX >= startX + btnW + 10 && mouseX < startX + btnW + 10 + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int delBg = delHovered ? 0xFF3A1111 : (hasSelection ? COLOR_COPPER : 0xFF181818);
        int delBorder = delHovered ? 0xFFFF5555 : (hasSelection ? COLOR_BRASS : 0xFF282828);
        drawFlatPanel(graphics, startX + btnW + 10, btnY, btnW, btnH, delBg, delBorder);
        graphics.drawCenteredString(this.font, deleteText, startX + btnW + 10 + btnW / 2, btnY + 6, hasSelection ? TEXT_PRIMARY : TEXT_MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int btnW = 100, btnH = 20;
            int startX = containerX + containerW - 15 - (btnW * 2 + 10);
            int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

            // Cancelar
            if (mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                Minecraft.getInstance().setScreen(this.editor);
                return true;
            }

            // Borrar (N)
            if (!selectedIds.isEmpty() && mouseX >= startX + btnW + 10 && mouseX < startX + btnW + 10 + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                String targetsLabel = selectedIds.size() == 1 ? selectedIds.get(0) : selectedIds.size() + " maestrías";
                Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(this, () -> {
                    editor.localPaths.removeIf(p -> selectedIds.contains(p.id));
                    if (editor.selectedPathIndex >= editor.localPaths.size()) {
                        editor.selectedPathIndex = editor.localPaths.isEmpty() ? -1 : 0;
                    }
                    editor.updateEditors();
                }, targetsLabel, editor));
                return true;
            }
        }

        // Check grid click
        int startY = getListStartY();
        int panelX = containerX;
        int listWidth = containerW - 40;
        int colWidth = (listWidth - 6) / 2;
        int gap = 6;
        int totalRows = (filteredEntries.size() + 1) / 2;
        int cardH = entryHeight - 4;

        if (button == 0) {
            for (int i = 0; i < maxVisible; i++) {
                for (int col = 0; col < 2; col++) {
                    int entryIndex = scrollOffset * 2 + i * 2 + col;
                    if (entryIndex >= filteredEntries.size()) break;

                    PathInfo p = filteredEntries.get(entryIndex);
                    int entryX = panelX + 20 + col * (colWidth + gap);
                    int entryY = startY + i * entryHeight;

                    if (mouseX >= entryX && mouseX < entryX + colWidth && mouseY >= entryY && mouseY < entryY + cardH) {
                        playClickSound();
                        
                        if (selectedIds.contains(p.id)) {
                            selectedIds.remove(p.id);
                        } else {
                            selectedIds.add(p.id);
                        }
                        return true;
                    }
                }
            }

            // Check scrollbar click
            if (totalRows > maxVisible) {
                int scrollbarX = panelX + containerW - 15;
                int scrollbarHeight = maxVisible * entryHeight;
                int thumbHeight = Math.max(15, (int) (((float) maxVisible / totalRows) * scrollbarHeight));

                if (mouseX >= scrollbarX - 4 && mouseX < scrollbarX + 10 && mouseY >= startY && mouseY < startY + scrollbarHeight) {
                    this.isDraggingGridScrollbar = true;
                    updateGridScrollFromMouse(mouseY, startY, scrollbarHeight, thumbHeight);
                    return true;
                }
            }
        }

        // Delegate search box click to super
        java.util.List<PathInfo> temp = new java.util.ArrayList<>(this.filteredEntries);
        this.filteredEntries.clear();
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        this.filteredEntries.addAll(temp);
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalRows = (filteredEntries.size() + 1) / 2;
        if (totalRows > maxVisible) {
            if (delta > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (delta < 0) {
                scrollOffset = Math.min(totalRows - maxVisible, scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
