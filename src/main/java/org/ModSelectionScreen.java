package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class ModSelectionScreen extends AbstractPickerScreen<String> {
    private boolean isDraggingGridScrollbar = false;

    public ModSelectionScreen(Screen parent, Consumer<String> onSelect) {
        super(parent, Component.translatable("xam.screen.mod_selection.title"), onSelect);
    }

    @Override
    protected boolean shouldShowNamespaceFilter() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.entryHeight = 44; // 40px card + 4px space
        int listHeight = bodyH - 38; // Maximize vertical usage
        this.maxVisible = Math.max(2, listHeight / entryHeight);
    }

    @Override
    protected void populateEntries() {
        if (this.parent instanceof AbstractPickerScreen) {
            this.allEntries.add("Todos");
        }
        ModList.get().getMods().forEach(mod -> {
            this.allEntries.add(mod.getModId());
        });
    }

    @Override
    protected void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        for (String modId : this.allEntries) {
            if (modId.toLowerCase().contains(q)) {
                this.filteredEntries.add(modId);
            }
        }
    }

    private static class TextureInfo {
        final ResourceLocation resourceLocation;
        final int width;
        final int height;

        TextureInfo(ResourceLocation resourceLocation, int width, int height) {
            this.resourceLocation = resourceLocation;
            this.width = width;
            this.height = height;
        }
    }

    private static final java.util.Map<String, TextureInfo> MOD_LOGOS = new java.util.HashMap<>();
    private static final ResourceLocation FALLBACK_ICON = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/writable_book.png");
    private static final TextureInfo FALLBACK_INFO = new TextureInfo(FALLBACK_ICON, 16, 16);

    public static TextureInfo getOrCreateModLogo(String modId) {
        if (MOD_LOGOS.containsKey(modId)) {
            return MOD_LOGOS.get(modId);
        }

        TextureInfo info = null;

        var containerOpt = net.minecraftforge.fml.ModList.get().getModContainerById(modId);
        if (containerOpt.isPresent()) {
            var container = containerOpt.get();
            var modInfo = container.getModInfo();
            String logoFile = modInfo.getLogoFile().orElse(null);
            if (logoFile != null && !logoFile.isEmpty()) {
                try {
                    var modFile = modInfo.getOwningFile().getFile();
                    var logoPath = modFile.findResource(logoFile);
                    if (java.nio.file.Files.exists(logoPath)) {
                        try (var is = java.nio.file.Files.newInputStream(logoPath)) {
                            var nativeImage = com.mojang.blaze3d.platform.NativeImage.read(is);
                            int w = nativeImage.getWidth();
                            int h = nativeImage.getHeight();
                            var dynamicTexture = new net.minecraft.client.renderer.texture.DynamicTexture(nativeImage);
                            ResourceLocation resLoc = ResourceLocation.fromNamespaceAndPath("xam", "textures/mod_logos/" + modId.toLowerCase());
                            net.minecraft.client.Minecraft.getInstance().getTextureManager().register(resLoc, dynamicTexture);
                            info = new TextureInfo(resLoc, w, h);
                        }
                    }
                } catch (Exception e) {
                    // Fallback to null
                }
            }
        }

        MOD_LOGOS.put(modId, info);
        return info;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Temporarily clear filteredEntries to prevent super.render from drawing the default single column list
        java.util.List<String> temp = new java.util.ArrayList<>(this.filteredEntries);
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

                String entry = filteredEntries.get(entryIndex);
                int entryX = panelX + 20 + col * (colWidth + gap);
                int entryY = startY + i * entryHeight;

                boolean hovered = mouseX >= entryX && mouseX < entryX + colWidth && mouseY >= entryY && mouseY < entryY + cardH;

                // Draw card background
                int bg = hovered ? BUTTON_HOVER_BG : PANEL_INNER_BG;
                int border = hovered ? BUTTON_HOVER_BORDER : WARM_BORDER;
                drawFlatPanel(guiGraphics, entryX, entryY, colWidth, cardH, bg, border);

                // Fetch Logo
                TextureInfo logo = null;
                if (!entry.equals("Todos")) {
                    logo = getOrCreateModLogo(entry);
                }
                if (logo == null) {
                    logo = FALLBACK_INFO;
                }

                // Draw logo scaled to 32x32 centered vertically inside the card
                int logoSize = 32;
                int logoY = entryY + (cardH - logoSize) / 2;
                guiGraphics.blit(logo.resourceLocation, entryX + 4, logoY, logoSize, logoSize, 0.0F, 0.0F, logo.width, logo.height, logo.width, logo.height);

                // Draw mod ID/Name next to logo
                String displayName = entry;
                if (entry.equals("Todos")) {
                    displayName = Component.translatable("xam.screen.mod_selection.all_mods").getString();
                } else {
                    int textMaxW = colWidth - 46;
                    if (this.font.width(displayName) > textMaxW) {
                        displayName = this.font.plainSubstrByWidth(displayName, textMaxW - 10) + "...";
                    }
                }
                int textY = entryY + (cardH - 8) / 2;
                guiGraphics.drawString(this.font, displayName, entryX + 42, textY, hovered ? COLOR_BRASS : TEXT_PRIMARY, false);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startY = getListStartY();
        int panelX = containerX;
        int listWidth = containerW - 40;
        int colWidth = (listWidth - 6) / 2;
        int gap = 6;
        int totalRows = (filteredEntries.size() + 1) / 2;

        if (button == 0) {
            // Check grid click
            for (int i = 0; i < maxVisible; i++) {
                for (int col = 0; col < 2; col++) {
                    int entryIndex = scrollOffset * 2 + i * 2 + col;
                    if (entryIndex >= filteredEntries.size()) break;

                    int entryX = panelX + 20 + col * (colWidth + gap);
                    int entryY = startY + i * entryHeight;

                    if (mouseX >= entryX && mouseX < entryX + colWidth && mouseY >= entryY && mouseY < entryY + (entryHeight - 4)) {
                        playClickSound();
                        this.onClickEntry(filteredEntries.get(entryIndex));
                        return true;
                    }
                }
            }

            // Check scrollbar click
            if (totalRows > maxVisible) {
                int scrollbarX = panelX + containerW - 15;
                int scrollbarHeight = maxVisible * entryHeight;
                int thumbHeight = Math.max(15, (int) (((float) maxVisible / totalRows) * scrollbarHeight));

                if (mouseX >= scrollbarX && mouseX < scrollbarX + 6 && mouseY >= startY && mouseY < startY + scrollbarHeight) {
                    this.isDraggingGridScrollbar = true;
                    updateGridScrollFromMouse(mouseY, startY, scrollbarHeight, thumbHeight);
                    return true;
                }
            }
        }

        // Delegate search box, back button and cancel button clicks to super
        java.util.List<String> temp = new java.util.ArrayList<>(this.filteredEntries);
        this.filteredEntries.clear();
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        this.filteredEntries.addAll(temp);
        return handled;
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

    private void updateGridScrollFromMouse(double mouseY, int scrollbarY, int scrollbarHeight, int thumbHeight) {
        int totalRows = (filteredEntries.size() + 1) / 2;
        float relativeY = (float) (mouseY - scrollbarY - thumbHeight / 2.0);
        float range = scrollbarHeight - thumbHeight;
        float pct = range > 0 ? Math.max(0f, Math.min(1f, relativeY / range)) : 0f;
        this.scrollOffset = Math.max(0, Math.min(totalRows - maxVisible, Math.round(pct * (totalRows - maxVisible))));
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

    @Override
    protected void renderEntry(GuiGraphics guiGraphics, String entry, int x, int y, int index, boolean hovered) {
        // Unused since we override the render method to draw a 2-column grid
    }

    @Override
    protected void onClickEntry(String entry) {
        this.onSelect.accept(entry);
    }
}
