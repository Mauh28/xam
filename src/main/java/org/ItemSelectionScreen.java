package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

public class ItemSelectionScreen extends AbstractPickerScreen<Item> {
    public ItemSelectionScreen(Screen parent, Consumer<Item> onSelect) {
        super(parent, Component.literal("Seleccionar Ítem"), onSelect);
    }

    @Override
    protected void populateEntries() {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                this.allEntries.add(item);
            }
        }
    }

    @Override
    protected void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        for (Item item : this.allEntries) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl == null) continue;
            String idStr = rl.toString().toLowerCase();
            String nameStr = item.getDescription().getString().toLowerCase();
            if (idStr.contains(q) || nameStr.contains(q)) {
                this.filteredEntries.add(item);
            }
        }
    }

    @Override
    protected void renderEntry(GuiGraphics guiGraphics, Item entry, int x, int y, int index, boolean hovered) {
        // Render item icon
        guiGraphics.renderFakeItem(new ItemStack(entry), x + 2, y + 2);
        
        // Render item name
        String name = entry.getDescription().getString();
        if (name.length() > 20) {
            name = name.substring(0, 18) + "..";
        }
        guiGraphics.drawString(this.font, name, x + 22, y + 6, hovered ? 0xFFFFD700 : 0xFFFFFF, false);
    }

    @Override
    protected void onClickEntry(Item entry) {
        this.onSelect.accept(entry);
    }
}
