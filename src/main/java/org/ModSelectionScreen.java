package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.function.Consumer;

public class ModSelectionScreen extends AbstractPickerScreen<String> {
    public ModSelectionScreen(Screen parent, Consumer<String> onSelect) {
        super(parent, Component.literal("Seleccionar Mod"), onSelect);
    }

    @Override
    protected boolean shouldShowNamespaceFilter() {
        return false;
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

    @Override
    protected void renderEntry(GuiGraphics guiGraphics, String entry, int x, int y, int index, boolean hovered) {
        guiGraphics.drawString(this.font, entry, x + 6, y + 6, hovered ? COLOR_BRASS : 0xFFFFFF, false);
    }

    @Override
    protected void onClickEntry(String entry) {
        this.onSelect.accept(entry);
    }
}
