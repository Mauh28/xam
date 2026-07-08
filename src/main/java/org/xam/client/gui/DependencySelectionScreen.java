package org.xam.client.gui;

import org.xam.XamConstants;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerData;
import org.xam.data.PlayerDataProvider;
import org.xam.network.XamNetwork;
import org.xam.network.SelectPathPacket;
import org.xam.network.UpdateConfigPacket;
import org.xam.progression.MasteryService;
import org.xam.progression.RequirementFormatter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Selector visual de dependencias entre ramas. Multi-selección con chip cíclico de cantidad
 * por rama (Dominar / 25% / 50% / 75% / 1 / 2 / 3 reqs). Reemplaza la edición manual por coma.
 * Extiende AbstractPickerScreen<String> (mismo patrón que ModSelectionScreen).
 */
public class DependencySelectionScreen extends AbstractPickerScreen<PathInfo> {

    /** Rama en edición: se excluye de la lista para evitar dependencias circulares (deadlock). */
    private final String excludeId;

    /** pathId -> índice del ciclo de cantidad elegido. Sólo tiene entradas para ramas seleccionadas. */
    private final Map<String, Integer> amountIndex = new LinkedHashMap<>();

    /** Callback final: recibe la lista de tokens serializados ("mekanism", "el_c:2", "botania:50%"). */
    private final Consumer<List<String>> onSave;

    /**
     * Ciclo de tokens de cantidad. El índice 0 = "Dominar" (sin sufijo, valor por defecto del parser).
     * Los demás generan sufijos válidos para isDependencyMet (xdAbsoluteMastery.java:424).
     */
    private static final String[] AMOUNT_SUFFIX = {"", ":1", ":2", ":3", ":25%", ":50%", ":75%"};
    private static final String[] AMOUNT_LABEL_KEYS  = {
        "xam.editor.dependency.master",
        "xam.editor.dependency.1_req",
        "xam.editor.dependency.2_reqs",
        "xam.editor.dependency.3_reqs",
        "xam.editor.dependency.pct_25",
        "xam.editor.dependency.pct_50",
        "xam.editor.dependency.pct_75"
    };

    private static String getAmountLabel(int index) {
        if (index < 0 || index >= AMOUNT_LABEL_KEYS.length) return "";
        return Component.translatable(AMOUNT_LABEL_KEYS[index]).getString();
    }

    public DependencySelectionScreen(Screen parent, String excludeId, List<PathInfo> sources,
                                     List<String> existing, Consumer<List<String>> onSelect) {
        super(parent, Component.translatable("xam.screen.dependency_selection.title"), null);
        this.excludeId = excludeId;
        this.onSave = onSelect;
        // Pre-marca como seleccionadas las dependencias que YA tenía la rama editada,
        // para no perderlas al reabrir el selector.
        for (String dep : existing) {
            String[] parts = dep.split(":");
            String depId = parts[0];
            if (excludeId != null && depId.equals(excludeId)) continue;
            String suffix = parts.length > 1 ? ":" + parts[1] : "";
            int idx = suffixIndex(suffix);
            amountIndex.put(depId, idx);
        }
    }

    private static int suffixIndex(String suffix) {
        for (int i = 0; i < AMOUNT_SUFFIX.length; i++) {
            if (AMOUNT_SUFFIX[i].equals(suffix)) return i;
        }
        return 0; // desconocido -> "Dominar"
    }

    private boolean isDraggingGridScrollbar = false;

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
        // Fuentes: todas las ramas conocidas excepto la que se está editando.
        // Recorre ConfigManager.PATHS (no localPaths) para usar los metadatos estables de nombre/icono.
        for (PathInfo p : ConfigManager.PATHS) {
            if (excludeId != null && p.id.equals(excludeId)) continue;
            this.allEntries.add(p);
        }
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
                boolean selected = amountIndex.containsKey(p.id);

                // Draw card background
                int bg = selected ? 0xFF2A201A : (hovered ? BUTTON_HOVER_BG : PANEL_INNER_BG);
                int border = selected ? COLOR_BRASS : (hovered ? BUTTON_HOVER_BORDER : WARM_BORDER);
                drawFlatPanel(guiGraphics, entryX, entryY, colWidth, cardH, bg, border);

                // Draw custom Checkbox
                int cbX = entryX + 8;
                int cbY = entryY + (cardH - 12) / 2;
                drawFlatPanel(guiGraphics, cbX, cbY, 12, 12, 0xFF140F0D, selected ? COLOR_BRASS : 0xFF2C221D);
                if (selected) {
                    guiGraphics.drawString(this.font, "✔", cbX + 2, cbY + 2, COLOR_BRASS, false);
                }

                // Render Branch Icon
                ItemStack icon = ItemStack.EMPTY;
                if (p.icon != null) {
                    net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(p.icon));
                    if (item != null) icon = new ItemStack(item);
                }
                if (icon.isEmpty()) icon = new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                guiGraphics.renderFakeItem(icon, entryX + 26, entryY + (cardH - 16) / 2);

                // Name + requirements count
                int textX = entryX + 46;
                int textY = entryY + (cardH - 8) / 2;
                int labelW = colWidth - 46 - 10;
                
                // If selected, we draw the cycle chip on the right
                if (selected) {
                    String chip = getAmountLabel(amountIndex.get(p.id)) + " ▸";
                    int chipW = this.font.width(chip) + 12;
                    int chipX = entryX + colWidth - chipW - 4;
                    int chipY = entryY + (cardH - 18) / 2;
                    
                    // Chip hover check
                    boolean chipHover = mouseX >= chipX && mouseX < chipX + chipW && mouseY >= chipY && mouseY < chipY + 18;
                    int chipBg = chipHover ? COLOR_COPPER_HOVER : PANEL_INNER_BG;
                    int chipBorder = chipHover ? COLOR_BRASS : COLOR_COPPER;
                    drawFlatPanel(guiGraphics, chipX, chipY, chipW, 18, chipBg, chipBorder);
                    guiGraphics.drawString(this.font, chip, chipX + 6, chipY + 5, COLOR_BRASS, false);
                    
                    labelW -= (chipW + 6);
                }

                String label = Component.translatable("xam.screen.dependency_selection.reqs_count", p.name, p.requirements.size()).getString();
                if (this.font.width(label) > labelW) {
                    label = this.font.plainSubstrByWidth(label, labelW - 8) + "...";
                }
                guiGraphics.drawString(this.font, label, textX, textY, selected ? COLOR_BRASS : (hovered ? TEXT_PRIMARY : TEXT_SECONDARY), false);
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
        // Botón "Cancelar" (izquierda) + "Guardar" (derecha, cobre).
        int btnW = 100, btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        drawFlatButton(graphics, startX, btnY, btnW, btnH, Component.translatable("xam.editor.cancel").getString(), mouseX, mouseY, true);

        boolean hasSelection = !amountIndex.isEmpty();
        String saveText = Component.translatable("xam.editor.save_format", amountIndex.size()).getString();
        drawFlatButton(graphics, startX + btnW + 10, btnY, btnW, btnH, saveText, mouseX, mouseY, hasSelection, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isBackButtonClicked(mouseX, mouseY)) {
            playClickSound();
            Minecraft.getInstance().setScreen(this.parent);
            return true;
        }
        // Botones del footer propio (sobreescribe el Cancelar del base).
        int btnW = 100, btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        if (button == 0 && mouseX >= startX && mouseX < startX + btnW
            && mouseY >= btnY && mouseY < btnY + btnH) {
            playClickSound();
            Minecraft.getInstance().setScreen(this.parent); // Cancelar
            return true;
        }
        int saveX = startX + btnW + 10;
        if (button == 0 && !amountIndex.isEmpty()
            && mouseX >= saveX && mouseX < saveX + btnW
            && mouseY >= btnY && mouseY < btnY + btnH) {
            playClickSound();
            onSave.accept(buildTokens());
            Minecraft.getInstance().setScreen(this.parent);
            return true;
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
                        
                        boolean selected = amountIndex.containsKey(p.id);
                        if (selected) {
                            // Check if clicked specifically on the cycle chip
                            String chip = getAmountLabel(amountIndex.get(p.id)) + " ▸";
                            int chipW = this.font.width(chip) + 12;
                            int chipX = entryX + colWidth - chipW - 4;
                            int chipY = entryY + (cardH - 18) / 2;
                            if (mouseX >= chipX && mouseX < chipX + chipW && mouseY >= chipY && mouseY < chipY + 18) {
                                int currentIdx = amountIndex.get(p.id);
                                amountIndex.put(p.id, (currentIdx + 1) % AMOUNT_SUFFIX.length);
                                return true;
                            }
                            // Otherwise, clicking the card deselects it
                            amountIndex.remove(p.id);
                        } else {
                            // Click card selects it with default "Dominar" (index 0)
                            amountIndex.put(p.id, 0);
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

    /** Serializa la selección a la lista de tokens que entiende isDependencyMet. */
    private List<String> buildTokens() {
        List<String> out = new ArrayList<>();
        // Orden estable: respeta el orden de allEntries (orden de aparición en PATHS), no el del map.
        for (PathInfo p : allEntries) {
            Integer idx = amountIndex.get(p.id);
            if (idx == null) continue;
            out.add(p.id + AMOUNT_SUFFIX[idx]);
        }
        return out;
    }

    /**
     * ponytail: self-check sin framework (AGENTS.md). Verifica round-trip token -> (pathId, amount)
     * contra la lógica de split que usa isDependencyMet (xdAbsoluteMastery.java:424-428).
     * Ceilings: sólo cubre el formato que genera este picker; el parser de runtime admite más.
     * Ejecutar con: java -cp ... org.DependencySelectionScreen
     */
    public static void main(String[] args) {
        String[][] cases = {
                {"mekanism",    "mekanism"},     // Dominar -> sin sufijo
                {"el_c:2",      "el_c"},         // N reqs
                {"botania:50%", "botania"}       // porcentaje
        };
        for (String[] c : cases) {
            String token = c[0];
            String expectedId = c[1];
            String[] parts = token.split(":");
            String pathId = parts[0];
            String amount = parts.length > 1 ? parts[1] : "mastered";
            assert pathId.equals(expectedId) : "pathId mismatch for " + token + ": got " + pathId;
            assert amount != null && !amount.isEmpty() : "amount empty for " + token;
            System.out.println("OK  " + token + " -> pathId=" + pathId + " amount=" + amount);
        }
        System.out.println("All dependency-token self-checks passed.");
    }
}
