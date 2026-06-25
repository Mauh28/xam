package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

public class EntitySelectionScreen extends AbstractPickerScreen<EntityType<?>> {
    public EntitySelectionScreen(Screen parent, Consumer<EntityType<?>> onSelect) {
        super(parent, Component.literal("Seleccionar Entidad"), onSelect);
    }

    @Override
    protected void populateEntries() {
        // ponytail: filtering entities by category != MISC. Ceilings: might exclude some custom entities of MISC type that are alive. Upgrade path: check EntityType hierarchy.
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (type != null && type.getCategory() != MobCategory.MISC) {
                this.allEntries.add(type);
            }
        }
    }

    @Override
    protected void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        String nsFilter = getNamespaceFilter().toLowerCase();
        for (EntityType<?> type : this.allEntries) {
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (rl == null) continue;
            String idStr = rl.toString().toLowerCase();
            String nameStr = type.getDescription().getString().toLowerCase();
            if (!nsFilter.isEmpty() && !rl.getNamespace().toLowerCase().contains(nsFilter)) {
                continue;
            }
            if (idStr.contains(q) || nameStr.contains(q)) {
                this.filteredEntries.add(type);
            }
        }
    }

    @Override
    protected void renderEntry(GuiGraphics guiGraphics, EntityType<?> entry, int x, int y, int index, boolean hovered) {
        // Render entity name
        String name = entry.getDescription().getString();
        if (name.length() > 22) {
            name = name.substring(0, 20) + "..";
        }
        
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(entry);
        String idStr = rl != null ? rl.toString() : "";
        if (idStr.length() > 30) {
            idStr = idStr.substring(0, 28) + "..";
        }

        guiGraphics.drawString(this.font, name, x + 6, y + 2, hovered ? 0xFFFFD700 : 0xFFFFFF, false);
        guiGraphics.drawString(this.font, idStr, x + 6, y + 11, 0x888888, false);
    }

    @Override
    protected void onClickEntry(EntityType<?> entry) {
        this.onSelect.accept(entry);
    }
}
