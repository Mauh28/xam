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
    private final java.util.Map<EntityType<?>, net.minecraft.world.entity.LivingEntity> entityCache = new java.util.HashMap<>();

    private static class EntitySearchEntry {
        final EntityType<?> type;
        final String idLower;
        final String nameLower;
        final String namespaceLower;

        EntitySearchEntry(EntityType<?> type) {
            this.type = type;
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
            this.idLower = rl != null ? rl.toString().toLowerCase() : "";
            this.nameLower = type.getDescription().getString().toLowerCase();
            this.namespaceLower = rl != null ? rl.getNamespace().toLowerCase() : "";
        }
    }

    private final java.util.List<EntitySearchEntry> cachedEntries = new java.util.ArrayList<>();

    public EntitySelectionScreen(Screen parent, Consumer<EntityType<?>> onSelect) {
        super(parent, Component.translatable("xam.screen.entity_selection.title"), onSelect);
        this.entryHeight = 32;
    }

    private net.minecraft.world.entity.LivingEntity getOrCreateEntity(EntityType<?> type) {
        return entityCache.computeIfAbsent(type, t -> {
            try {
                if (this.minecraft != null && this.minecraft.level != null) {
                    net.minecraft.world.entity.Entity entity = t.create(this.minecraft.level);
                    if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                        return living;
                    }
                }
            } catch (Exception e) {
                // Ignore entity types that cannot be instantiated client-side
            }
            return null;
        });
    }

    @Override
    protected void populateEntries() {
        // ponytail: filtering entities by category != MISC. Ceilings: might exclude some custom entities of MISC type that are alive. Upgrade path: check EntityType hierarchy.
        this.cachedEntries.clear();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (type != null && type.getCategory() != MobCategory.MISC) {
                this.allEntries.add(type);
                this.cachedEntries.add(new EntitySearchEntry(type));
            }
        }
    }

    @Override
    protected void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        String nsFilter = getNamespaceFilter().toLowerCase();
        for (EntitySearchEntry entry : this.cachedEntries) {
            if (!nsFilter.isEmpty() && !entry.namespaceLower.contains(nsFilter)) {
                continue;
            }
            if (entry.idLower.contains(q) || entry.nameLower.contains(q)) {
                this.filteredEntries.add(entry.type);
            }
        }
    }

    @Override
    protected void renderEntry(GuiGraphics guiGraphics, EntityType<?> entry, int x, int y, int index, boolean hovered) {
        // Render entity preview
        net.minecraft.world.entity.LivingEntity living = getOrCreateEntity(entry);
        if (living != null) {
            float width = living.getBbWidth();
            float height = living.getBbHeight();
            float maxDim = Math.max(width, height);
            int scale = (int) (10.0F / Math.max(0.1F, maxDim));

            try {
                net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                        guiGraphics,
                        x + 16,
                        y + 26,
                        scale,
                        (float) (x + 16) - this.lastMouseX,
                        (float) (y + 15) - this.lastMouseY,
                        living
                );
            } catch (Exception e) {
                // Ignore rendering errors for entities that fail to render on client-side
            }
        }

        // Render entity name (shifted to x + 34)
        String name = entry.getDescription().getString();
        if (name.length() > 22) {
            name = name.substring(0, 20) + "..";
        }
        
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(entry);
        String idStr = rl != null ? rl.toString() : "";
        if (idStr.length() > 30) {
            idStr = idStr.substring(0, 28) + "..";
        }

        guiGraphics.drawString(this.font, name, x + 34, y + 4, hovered ? COLOR_BRASS : 0xFFFFFF, false);
        guiGraphics.drawString(this.font, idStr, x + 34, y + 15, 0x888888, false);
    }

    @Override
    protected void onClickEntry(EntityType<?> entry) {
        this.onSelect.accept(entry);
    }
}
