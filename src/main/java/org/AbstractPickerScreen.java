package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractPickerScreen<T> extends AbstractMasteryScreen {
    protected final Screen parent;
    protected final Consumer<T> onSelect;
    protected final List<T> allEntries = new ArrayList<>();
    protected final List<T> filteredEntries = new ArrayList<>();
    
    protected EditBox searchBox;
    protected String selectedModFilter = "Todos";
    protected int scrollOffset = 0;
    protected final int entryHeight = 20;
    protected int maxVisible = 7;
    
    private boolean isDraggingScrollbar = false;

    public AbstractPickerScreen(Screen parent, Component title, Consumer<T> onSelect) {
        super(title);
        this.parent = parent;
        this.onSelect = onSelect;
    }

    protected boolean shouldShowNamespaceFilter() {
        return true;
    }

    protected String getNamespaceFilter() {
        return (this.selectedModFilter == null || this.selectedModFilter.equals("Todos")) ? "" : this.selectedModFilter;
    }

    @Override
    protected void init() {
        super.init();
        
        // Calculate maxVisible dynamically based on bodyHeight
        int listHeight = bodyH - 70;
        this.maxVisible = Math.max(3, listHeight / entryHeight);

        int panelX = containerX;
        int searchY = bodyY + (shouldShowNamespaceFilter() ? 37 : 15);

        // searchBox is designed borderless within drawFlatPanel
        this.searchBox = new EditBox(this.font, panelX + 24, searchY + 4, containerW - 48, 12, Component.literal("Buscar..."));
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(TEXT_PRIMARY);
        this.searchBox.setResponder(text -> {
            this.filterEntries(text);
            this.scrollOffset = 0;
        });
        this.addRenderableWidget(this.searchBox);

        // Populate lists
        if (this.allEntries.isEmpty()) {
            this.populateEntries();
        }
        this.filterEntries(this.searchBox.getValue());
    }

    protected abstract void populateEntries();
    protected abstract void filterEntries(String query);
    protected abstract void renderEntry(GuiGraphics guiGraphics, T entry, int x, int y, int index, boolean hovered);
    protected abstract void onClickEntry(T entry);

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, this.title, containerX + 15, titleY, TEXT_PRIMARY, false);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnW = 100;
        int btnH = 20;
        int btnX = containerX + containerW - 15 - btnW;
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        drawFlatButton(graphics, btnX, btnY, btnW, btnH, "Cancelar", mouseX, mouseY, true);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int panelX = containerX;

        // Render Mod Filter Button if visible
        if (shouldShowNamespaceFilter()) {
            int btnX = panelX + 20;
            int btnY = bodyY + 12;
            int btnW = containerW - 40;
            int btnH = 20;
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
            
            int bg = hovered ? BUTTON_HOVER_BG : BUTTON_BACKGROUND;
            int border = hovered ? BUTTON_HOVER_BORDER : BUTTON_BORDER;
            
            drawFlatPanel(guiGraphics, btnX, btnY, btnW, btnH, bg, border);
            
            String btnText = "Mod: " + selectedModFilter;
            int textX = btnX + (btnW - this.font.width(btnText)) / 2;
            int textY = btnY + (btnH - 8) / 2;
            guiGraphics.drawString(this.font, btnText, textX, textY, hovered ? TEXT_PRIMARY : TEXT_SECONDARY, false);
        }

        // Draw search box background panel
        int searchY = bodyY + (shouldShowNamespaceFilter() ? 37 : 15);
        drawFlatPanel(guiGraphics, panelX + 20, searchY, containerW - 40, 20, INPUT_BACKGROUND, BORDER_STANDARD);

        // Render virtual list entries
        int startY = bodyY + (shouldShowNamespaceFilter() ? 62 : 40);
        int listWidth = containerW - 40;
        
        for (int i = 0; i < maxVisible; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= filteredEntries.size()) break;
            
            T entry = filteredEntries.get(entryIndex);
            int entryX = panelX + 20;
            int entryY = startY + i * entryHeight;
            
            boolean hovered = mouseX >= entryX && mouseX < entryX + listWidth && mouseY >= entryY && mouseY < entryY + entryHeight;
            
            // Hover background
            if (hovered) {
                guiGraphics.fill(entryX, entryY, entryX + listWidth, entryY + entryHeight, 0x33FFFFFF);
            }
            
            renderEntry(guiGraphics, entry, entryX, entryY, entryIndex, hovered);
        }

        // Render scrollbar if needed
        if (filteredEntries.size() > maxVisible) {
            int scrollbarX = panelX + containerW - 15;
            int scrollbarY = startY;
            int scrollbarHeight = maxVisible * entryHeight;
            
            // Track
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x33FFFFFF);
            
            // Scroll thumb
            float fraction = (float) scrollOffset / (filteredEntries.size() - maxVisible);
            int thumbHeight = Math.max(15, (int) (((float) maxVisible / filteredEntries.size()) * scrollbarHeight));
            int thumbY = scrollbarY + (int) (fraction * (scrollbarHeight - thumbHeight));
            
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, BORDER_STANDARD);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = containerX;

        // Mod Filter Button click
        if (shouldShowNamespaceFilter() && button == 0) {
            int btnX = panelX + 20;
            int btnY = bodyY + 12;
            int btnW = containerW - 40;
            int btnH = 20;
            if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                Minecraft.getInstance().setScreen(new ModSelectionScreen(this, modId -> {
                    this.selectedModFilter = modId;
                    this.filterEntries(this.searchBox.getValue());
                    this.scrollOffset = 0;
                    Minecraft.getInstance().setScreen(this);
                }));
                return true;
            }
        }

        // Cancelar button click
        int btnW = 100;
        int btnH = 20;
        int btnX = containerX + containerW - 15 - btnW;
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;
        if (button == 0 && mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
            playClickSound();
            Minecraft.getInstance().setScreen(this.parent);
            return true;
        }

        // Scrollbar click/drag detection
        int startY = bodyY + (shouldShowNamespaceFilter() ? 62 : 40);
        if (filteredEntries.size() > maxVisible) {
            int scrollbarX = panelX + containerW - 15;
            int scrollbarY = startY;
            int scrollbarHeight = maxVisible * entryHeight;
            int thumbHeight = Math.max(15, (int) (((float) maxVisible / filteredEntries.size()) * scrollbarHeight));

            if (button == 0 && mouseX >= scrollbarX && mouseX < scrollbarX + 6 && mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight) {
                this.isDraggingScrollbar = true;
                updateScrollFromMouse(mouseY, scrollbarY, scrollbarHeight, thumbHeight);
                return true;
            }
        }

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Entries click
        int listWidth = containerW - 40;
        for (int i = 0; i < maxVisible; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= filteredEntries.size()) break;
            
            T entry = filteredEntries.get(entryIndex);
            int entryX = panelX + 20;
            int entryY = startY + i * entryHeight;
            
            if (mouseX >= entryX && mouseX < entryX + listWidth && mouseY >= entryY && mouseY < entryY + entryHeight) {
                this.onClickEntry(entry);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingScrollbar && button == 0 && filteredEntries.size() > maxVisible) {
            int startY = bodyY + (shouldShowNamespaceFilter() ? 62 : 40);
            int scrollbarY = startY;
            int scrollbarHeight = maxVisible * entryHeight;
            int thumbHeight = Math.max(15, (int) (((float) maxVisible / filteredEntries.size()) * scrollbarHeight));
            updateScrollFromMouse(mouseY, scrollbarY, scrollbarHeight, thumbHeight);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateScrollFromMouse(double mouseY, int scrollbarY, int scrollbarHeight, int thumbHeight) {
        float relativeY = (float) (mouseY - scrollbarY - thumbHeight / 2.0);
        float range = scrollbarHeight - thumbHeight;
        float pct = range > 0 ? Math.max(0f, Math.min(1f, relativeY / range)) : 0f;
        this.scrollOffset = Math.max(0, Math.min(filteredEntries.size() - maxVisible, Math.round(pct * (filteredEntries.size() - maxVisible))));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (filteredEntries.size() > maxVisible) {
            if (delta > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (delta < 0) {
                scrollOffset = Math.min(filteredEntries.size() - maxVisible, scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
