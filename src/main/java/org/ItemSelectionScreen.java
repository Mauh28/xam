package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemSelectionScreen extends AbstractMasteryScreen {
    private static class ItemSearchEntry {
        final Item item;
        final String idLower;
        final String nameLower;
        final String namespaceLower;

        ItemSearchEntry(Item item) {
            this.item = item;
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            this.idLower = rl != null ? rl.toString().toLowerCase() : "";
            this.nameLower = item.getDescription().getString().toLowerCase();
            this.namespaceLower = rl != null ? rl.getNamespace().toLowerCase() : "";
        }
    }

    private final Screen parent;
    private final Consumer<Item> onSelect;
    private final List<ItemSearchEntry> allEntries = new ArrayList<>();
    private final List<Item> filteredEntries = new ArrayList<>();
    
    private EditBox searchBox;
    private String selectedModFilter = "Todos";
    private int scrollRowOffset = 0;
    private final int slotSize = 20;
    private final int gap = 2;
    private int cols = 1;
    private int visibleRows = 1;
    
    private boolean isDraggingScrollbar = false;

    public ItemSelectionScreen(Screen parent, Consumer<Item> onSelect) {
        super(Component.translatable("xam.screen.item_selection.title"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();
        
        int panelX = containerX;
        int gridW = containerW - 40;
        this.cols = Math.max(1, gridW / (slotSize + gap));
        
        int startY = bodyY + 62;
        int gridH = (bodyY + bodyH - 10) - startY;
        this.visibleRows = Math.max(1, gridH / (slotSize + gap));

        int searchY = bodyY + 37;
        this.searchBox = new EditBox(this.font, panelX + 24, searchY + 4, containerW - 48, 12, Component.translatable("xam.editor.search_placeholder"));
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(TEXT_PRIMARY);
        this.searchBox.setResponder(text -> {
            this.filterEntries(text);
            this.scrollRowOffset = 0;
        });
        this.addRenderableWidget(this.searchBox);

        if (this.allEntries.isEmpty()) {
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    this.allEntries.add(new ItemSearchEntry(item));
                }
            }
        }
        this.filterEntries(this.searchBox.getValue());
    }

    private void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        String nsFilter = (this.selectedModFilter == null || this.selectedModFilter.equals("Todos")) ? "" : this.selectedModFilter.toLowerCase();
        for (ItemSearchEntry entry : this.allEntries) {
            if (!nsFilter.isEmpty() && !entry.namespaceLower.contains(nsFilter)) {
                continue;
            }
            if (entry.idLower.contains(q) || entry.nameLower.contains(q)) {
                this.filteredEntries.add(entry.item);
            }
        }
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, Component.translatable("xam.screen.item_selection.title").getString(), containerX + 15, titleY, COLOR_BRASS, false);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnW = 100;
        int btnH = 20;
        int btnX = containerX + containerW - 15 - btnW;
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;
        drawFlatButton(graphics, btnX, btnY, btnW, btnH, Component.translatable("xam.editor.cancel").getString(), mouseX, mouseY, true);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int panelX = containerX;
        
        // Render Mod Filter Button
        int btnX = panelX + 20;
        int btnY = bodyY + 12;
        int btnW = containerW - 40;
        int btnH = 20;
        boolean filterHovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int filterBg = filterHovered ? BUTTON_HOVER_BG : BUTTON_BACKGROUND;
        int filterBorder = filterHovered ? BUTTON_HOVER_BORDER : BUTTON_BORDER;
        drawFlatPanel(guiGraphics, btnX, btnY, btnW, btnH, filterBg, filterBorder);
        
        String displayFilter = selectedModFilter.equals("Todos") ? Component.translatable("xam.editor.all").getString() : selectedModFilter;
        String btnText = Component.translatable("xam.editor.mod_filter_label", displayFilter).getString();
        int textX = btnX + (btnW - this.font.width(btnText)) / 2;
        int textY = btnY + (btnH - 8) / 2;
        guiGraphics.drawString(this.font, btnText, textX, textY, filterHovered ? TEXT_PRIMARY : TEXT_SECONDARY, false);

        // Search background
        int searchY = bodyY + 37;
        drawFlatPanel(guiGraphics, panelX + 20, searchY, containerW - 40, 20, INPUT_BACKGROUND, COLOR_COPPER);

        // Grid Area
        int startY = bodyY + 62;
        int startX = panelX + 20;
        int startIndex = scrollRowOffset * cols;
        int totalRows = (filteredEntries.size() + cols - 1) / cols;

        Item hoveredItem = null;
        ItemStack hoveredItemStack = ItemStack.EMPTY;

        for (int r = 0; r < visibleRows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = startIndex + r * cols + c;
                if (index >= filteredEntries.size()) break;
                
                Item item = filteredEntries.get(index);
                int slotX = startX + c * (slotSize + gap);
                int slotY = startY + r * (slotSize + gap);
                
                boolean slotHovered = mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize;
                int slotBg = slotHovered ? 0xFF2C221D : PANEL_INNER_BG;
                int slotBorder = slotHovered ? COLOR_BRASS : 0xFF2A201C;
                
                drawFlatPanel(guiGraphics, slotX, slotY, slotSize, slotSize, slotBg, slotBorder);
                ItemStack stack = new ItemStack(item);
                guiGraphics.renderFakeItem(stack, slotX + 2, slotY + 2);
                
                if (slotHovered) {
                    hoveredItem = item;
                    hoveredItemStack = stack;
                }
            }
        }

        // Render Scrollbar
        if (totalRows > visibleRows) {
            int scrollbarX = panelX + containerW - 15;
            int scrollbarY = startY;
            int scrollbarH = visibleRows * (slotSize + gap);
            
            guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarH, 0xFF2A201C);
            
            float fraction = (float) scrollRowOffset / (totalRows - visibleRows);
            int thumbHeight = Math.max(15, (int) (((float) visibleRows / totalRows) * scrollbarH));
            int thumbY = scrollbarY + (int) (fraction * (scrollbarH - thumbHeight));
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, COLOR_COPPER);
        }

        // Tooltip (rendered last to overlay slots)
        if (hoveredItem != null && !hoveredItemStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredItemStack, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalRows = (filteredEntries.size() + cols - 1) / cols;
        int maxScrollRows = Math.max(0, totalRows - visibleRows);
        if (maxScrollRows > 0) {
            scrollRowOffset = Math.max(0, Math.min(maxScrollRows, scrollRowOffset - (int) delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelX = containerX;
            
            // Mod Filter Button
            int btnX = panelX + 20;
            int btnY = bodyY + 12;
            int btnW = containerW - 40;
            int btnH = 20;
            if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                Minecraft.getInstance().setScreen(new ModSelectionScreen(this, modId -> {
                    this.selectedModFilter = modId;
                    this.filterEntries(this.searchBox.getValue());
                    this.scrollRowOffset = 0;
                    Minecraft.getInstance().setScreen(this);
                }));
                return true;
            }

            // Cancel Button
            int cancelBtnW = 100;
            int cancelBtnH = 20;
            int cancelBtnX = containerX + containerW - 15 - cancelBtnW;
            int cancelBtnY = containerY + containerH - footerH + (footerH - cancelBtnH) / 2;
            if (mouseX >= cancelBtnX && mouseX < cancelBtnX + cancelBtnW && mouseY >= cancelBtnY && mouseY < cancelBtnY + cancelBtnH) {
                playClickSound();
                Minecraft.getInstance().setScreen(this.parent);
                return true;
            }

            // Grid Items
            int startY = bodyY + 62;
            int startX = panelX + 20;
            int startIndex = scrollRowOffset * cols;
            for (int r = 0; r < visibleRows; r++) {
                for (int c = 0; c < cols; c++) {
                    int index = startIndex + r * cols + c;
                    if (index >= filteredEntries.size()) break;
                    
                    Item item = filteredEntries.get(index);
                    int slotX = startX + c * (slotSize + gap);
                    int slotY = startY + r * (slotSize + gap);
                    if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                        playClickSound();
                        this.onSelect.accept(item);
                        return true;
                    }
                }
            }

            // Scrollbar dragging
            int totalRows = (filteredEntries.size() + cols - 1) / cols;
            if (totalRows > visibleRows) {
                int scrollbarX = panelX + containerW - 15;
                int scrollbarY = startY;
                int scrollbarH = visibleRows * (slotSize + gap);
                if (mouseX >= scrollbarX && mouseX < scrollbarX + 4 && mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarH) {
                    this.isDraggingScrollbar = true;
                    this.updateScrollFromMouse(mouseY);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.isDraggingScrollbar) {
            this.updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateScrollFromMouse(double mouseY) {
        int startY = bodyY + 62;
        int scrollbarH = visibleRows * (slotSize + gap);
        float fraction = (float) (mouseY - startY) / scrollbarH;
        fraction = Math.max(0.0f, Math.min(1.0f, fraction));
        int totalRows = (filteredEntries.size() + cols - 1) / cols;
        int maxScrollRows = Math.max(0, totalRows - visibleRows);
        this.scrollRowOffset = Math.round(fraction * maxScrollRows);
    }
}
