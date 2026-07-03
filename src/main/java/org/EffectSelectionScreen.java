package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.Consumer;

public class EffectSelectionScreen extends AbstractPickerScreen<MobEffect> {
    private boolean isDraggingGridScrollbar = false;

    public EffectSelectionScreen(Screen parent, Consumer<MobEffect> onSelect) {
        super(parent, Component.translatable("xam.screen.effect_selection.title"), onSelect);
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
    protected boolean shouldShowNamespaceFilter() { return true; }

    @Override
    protected void populateEntries() {
        this.allEntries.addAll(ForgeRegistries.MOB_EFFECTS.getValues());
    }

    @Override
    protected void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        String namespace = getNamespaceFilter();

        for (MobEffect effect : this.allEntries) {
            ResourceLocation rl = ForgeRegistries.MOB_EFFECTS.getKey(effect);
            if (rl == null) continue;
            
            String modId = rl.getNamespace();
            if (!namespace.isEmpty() && !modId.equals(namespace)) continue;

            String name = effect.getDisplayName().getString();
            String id = rl.toString();

            if (name.toLowerCase().contains(q) || id.toLowerCase().contains(q)) {
                this.filteredEntries.add(effect);
            }
        }
    }

    @Override
    protected void renderEntry(GuiGraphics g, MobEffect p, int x, int y, int index, boolean hovered) {
        // No-op because we completely override render
    }

    @Override
    protected void onClickEntry(MobEffect p) {
        // No-op because we completely override mouseClicked
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Temporarily clear filteredEntries to prevent super.render from drawing the default single column list
        java.util.List<MobEffect> temp = new java.util.ArrayList<>(this.filteredEntries);
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

                MobEffect p = filteredEntries.get(entryIndex);
                int entryX = panelX + 20 + col * (colWidth + gap);
                int entryY = startY + i * entryHeight;

                boolean hovered = mouseX >= entryX && mouseX < entryX + colWidth && mouseY >= entryY && mouseY < entryY + cardH;

                // Draw card background
                int bg = hovered ? BUTTON_HOVER_BG : PANEL_INNER_BG;
                int border = hovered ? BUTTON_HOVER_BORDER : WARM_BORDER;
                drawFlatPanel(guiGraphics, entryX, entryY, colWidth, cardH, bg, border);

                // Render Effect Icon
                net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(p);
                if (sprite != null) {
                    com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
                    guiGraphics.blit(entryX + 12, entryY + (cardH - 18) / 2, 0, 18, 18, sprite);
                } else {
                    // Fallback to writable book
                    ItemStack icon = new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                    guiGraphics.renderFakeItem(icon, entryX + 13, entryY + (cardH - 16) / 2);
                }

                // Name + registry ID below it or parenthesized
                int textX = entryX + 38;
                int textY = entryY + (cardH - 8) / 2;
                int labelW = colWidth - 38 - 10;
                
                ResourceLocation rl = ForgeRegistries.MOB_EFFECTS.getKey(p);
                String idStr = rl != null ? rl.toString() : "";
                String label = p.getDisplayName().getString();
                if (this.font.width(label) > labelW) {
                    label = this.font.plainSubstrByWidth(label, labelW - 8) + "...";
                }
                guiGraphics.drawString(this.font, label, textX, textY - 4, hovered ? TEXT_PRIMARY : TEXT_SECONDARY, false);
                
                String subLabel = idStr;
                if (this.font.width(subLabel) > labelW) {
                    subLabel = this.font.plainSubstrByWidth(subLabel, labelW - 8) + "...";
                }
                guiGraphics.drawString(this.font, subLabel, textX, textY + 6, TEXT_MUTED, false);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

                    MobEffect p = filteredEntries.get(entryIndex);
                    int entryX = panelX + 20 + col * (colWidth + gap);
                    int entryY = startY + i * entryHeight;

                    if (mouseX >= entryX && mouseX < entryX + colWidth && mouseY >= entryY && mouseY < entryY + cardH) {
                        playClickSound();
                        onSelect.accept(p);
                        Minecraft.getInstance().setScreen(this.parent);
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
        java.util.List<MobEffect> temp = new java.util.ArrayList<>(this.filteredEntries);
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
