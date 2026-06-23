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

public abstract class AbstractPickerScreen<T> extends Screen {
    protected final Screen parent;
    protected final Consumer<T> onSelect;
    protected final List<T> allEntries = new ArrayList<>();
    protected final List<T> filteredEntries = new ArrayList<>();
    
    protected EditBox searchBox;
    protected int scrollOffset = 0;
    protected final int entryHeight = 20;
    protected final int maxVisible = 7;
    
    public AbstractPickerScreen(Screen parent, Component title, Consumer<T> onSelect) {
        super(title);
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();
        
        int panelWidth = 250;
        int panelHeight = 210;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelY = this.height / 2 - panelHeight / 2;

        // Search text box
        this.searchBox = new EditBox(this.font, panelX + 15, panelY + 25, panelWidth - 30, 15, Component.literal("Buscar..."));
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

        // Cancel / Back Button
        Button backBtn = Button.builder(Component.literal("Cancelar"), b -> {
            Minecraft.getInstance().setScreen(this.parent);
        }).bounds(panelX + panelWidth / 2 - 40, panelY + panelHeight - 25, 80, 20).build();
        this.addRenderableWidget(backBtn);
    }

    protected abstract void populateEntries();
    protected abstract void filterEntries(String query);
    protected abstract void renderEntry(GuiGraphics guiGraphics, T entry, int x, int y, int index, boolean hovered);
    protected abstract void onClickEntry(T entry);

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int panelWidth = 250;
        int panelHeight = 210;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelY = this.height / 2 - panelHeight / 2;

        // Background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight - 30, 0xDD0F0F12);
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight - 30, 0xFFFFD700);

        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 10, 0xFFFFD700);

        // Render virtual list entries
        int startY = panelY + 45;
        int listWidth = panelWidth - 30;
        
        for (int i = 0; i < maxVisible; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= filteredEntries.size()) break;
            
            T entry = filteredEntries.get(entryIndex);
            int entryX = panelX + 15;
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
            int scrollbarX = panelX + panelWidth - 12;
            int scrollbarY = startY;
            int scrollbarHeight = maxVisible * entryHeight;
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0x33FFFFFF);
            
            // Scroll thumb
            float fraction = (float) scrollOffset / (filteredEntries.size() - maxVisible);
            int thumbHeight = Math.max(10, (int) ((float) maxVisible / filteredEntries.size() * scrollbarHeight));
            int thumbY = scrollbarY + (int) (fraction * (scrollbarHeight - thumbHeight));
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xFFFFD700);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int panelWidth = 250;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelHeight = 210;
        int panelY = this.height / 2 - panelHeight / 2;

        int startY = panelY + 45;
        int listWidth = panelWidth - 30;

        for (int i = 0; i < maxVisible; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= filteredEntries.size()) break;
            
            T entry = filteredEntries.get(entryIndex);
            int entryX = panelX + 15;
            int entryY = startY + i * entryHeight;
            
            if (mouseX >= entryX && mouseX < entryX + listWidth && mouseY >= entryY && mouseY < entryY + entryHeight) {
                this.onClickEntry(entry);
                return true;
            }
        }
        return false;
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
