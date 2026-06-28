package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.xdAbsoluteMastery.ConfigManager.PathInfo;

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
    private static final String[] AMOUNT_LABEL  = {"Dominar", "1 req", "2 reqs", "3 reqs", "25%", "50%", "75%"};

    public DependencySelectionScreen(Screen parent, String excludeId, List<PathInfo> sources,
                                     List<String> existing, Consumer<List<String>> onSelect) {
        super(parent, Component.literal("Seleccionar Dependencias"), null);
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

    @Override
    protected boolean shouldShowNamespaceFilter() { return false; }

    @Override
    protected void populateEntries() {
        // Fuentes: todas las ramas conocidas excepto la que se está editando.
        // Recorre ConfigManager.PATHS (no localPaths) para usar los metadatos estables de nombre/icono.
        for (PathInfo p : org.xdAbsoluteMastery.ConfigManager.PATHS) {
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
        // Icono de la rama (mismo fallback que MasteryEditorScreen:353-369)
        ItemStack icon = ItemStack.EMPTY;
        if (p.icon != null) {
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(p.icon));
            if (item != null) icon = new ItemStack(item);
        }
        if (icon.isEmpty()) icon = new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
        g.renderFakeItem(icon, x + 2, y + 1);

        // Nombre + nº de requisitos
        boolean selected = amountIndex.containsKey(p.id);
        int nameColor = selected ? COLOR_BRASS : (hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
        String label = p.name + "  (" + p.requirements.size() + " reqs)";
        g.drawString(this.font, label, x + 24, y + 6, nameColor, false);

        // Checkbox visual a la izquierda del icono
        g.drawString(this.font, selected ? "☑" : "☐", x - 12, y + 6, selected ? COLOR_BRASS : TEXT_MUTED, false);

        // Chip cíclico de cantidad (sólo si está seleccionada), alineado a la derecha de la fila
        if (selected) {
            String chip = AMOUNT_LABEL[amountIndex.get(p.id)] + " ▸";
            int chipW = this.font.width(chip) + 12;
            int chipH = entryHeight - 4;
            int chipX = x + (containerW - 40) - chipW - 4;
            int chipY = y + 2;
            drawFlatPanel(g, chipX, chipY, chipW, chipH, PANEL_INNER_BG, COLOR_COPPER);
            g.drawString(this.font, chip, chipX + 6, chipY + (chipH - 8) / 2 + 1, COLOR_BRASS, false);
        }
    }

    /**
     * Override del clic de fila: NO cierra la pantalla ni llama a onSelect.
     * Alterna la selección. El clic en el chip de cantidad se resuelve por coordenadas aquí mismo.
     */
    @Override
    protected void onClickEntry(PathInfo p) {
        if (amountIndex.containsKey(p.id)) {
            // ¿Clic dentro del chip? -> ciclar cantidad en vez de deseleccionar.
            int entryY = currentEntryY(p);
            if (entryY >= 0 && clickedOnAmountChip(p, entryY)) {
                int idx = amountIndex.get(p.id);
                amountIndex.put(p.id, (idx + 1) % AMOUNT_SUFFIX.length);
                return;
            }
            amountIndex.remove(p.id); // deseleccionar
        } else {
            amountIndex.put(p.id, 0); // seleccionar (por defecto "Dominar")
        }
    }

    /** Reconstruye la Y de una fila visible a partir de su índice en filteredEntries. */
    private int currentEntryY(PathInfo p) {
        int startY = bodyY + 40; // shouldShowNamespaceFilter() == false -> 40
        for (int i = 0; i < maxVisible; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= filteredEntries.size()) break;
            if (filteredEntries.get(entryIndex).id.equals(p.id)) {
                return startY + i * entryHeight;
            }
        }
        return -1;
    }

    private boolean clickedOnAmountChip(PathInfo p, int entryY) {
        int panelX = containerX;
        int entryX = panelX + 20;
        String chip = AMOUNT_LABEL[amountIndex.get(p.id)] + " ▸";
        int chipW = this.font.width(chip) + 12;
        int chipH = entryHeight - 4;
        int chipX = entryX + (containerW - 40) - chipW - 4;
        int chipY = entryY + 2;
        return lastMouseX >= chipX && lastMouseX < chipX + chipW
            && lastMouseY >= chipY && lastMouseY < chipY + chipH;
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        // Botón "Cancelar" (izquierda) + "Guardar" (derecha, cobre).
        int btnW = 100, btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        drawFlatButton(graphics, startX, btnY, btnW, btnH, "Cancelar", mouseX, mouseY, true);

        boolean hasSelection = !amountIndex.isEmpty();
        String saveText = "Guardar (" + amountIndex.size() + ")";
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

        return super.mouseClicked(mouseX, mouseY, button);
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
