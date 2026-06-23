package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.Advancement;

import java.util.function.Consumer;

public class AdvancementSelectionScreen extends AbstractPickerScreen<Advancement> {
    public AdvancementSelectionScreen(Screen parent, Consumer<Advancement> onSelect) {
        super(parent, Component.literal("Seleccionar Logro"), onSelect);
    }

    @Override
    protected void populateEntries() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            var clientAdvs = connection.getAdvancements();
            var advList = clientAdvs.getAdvancements();
            
            // ponytail: using reflection to read client advancements list. Ceilings: requires Minecraft connection active on client thread. Upgrade path: query network packet.
            // Try standard method first: getAllAdvancements()
            try {
                java.lang.reflect.Method m = advList.getClass().getMethod("getAllAdvancements");
                @SuppressWarnings("unchecked")
                java.util.Collection<Advancement> coll = (java.util.Collection<Advancement>) m.invoke(advList);
                for (Advancement adv : coll) {
                    if (adv != null) {
                        this.allEntries.add(adv);
                    }
                }
                return;
            } catch (Exception ignored) {}
            
            // Fallback: reflectively find any Map field inside AdvancementList
            try {
                for (java.lang.reflect.Field field : advList.getClass().getDeclaredFields()) {
                    if (java.util.Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) field.get(advList);
                        for (Object value : map.values()) {
                            if (value instanceof Advancement) {
                                this.allEntries.add((Advancement) value);
                            }
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        for (Advancement adv : this.allEntries) {
            String idStr = adv.getId().toString().toLowerCase();
            String titleText = "";
            if (adv.getDisplay() != null) {
                titleText = adv.getDisplay().getTitle().getString().toLowerCase();
            }
            if (idStr.contains(q) || titleText.contains(q)) {
                this.filteredEntries.add(adv);
            }
        }
    }

    @Override
    protected void renderEntry(GuiGraphics guiGraphics, Advancement entry, int x, int y, int index, boolean hovered) {
        String titleText = null;
        if (entry.getDisplay() != null) {
            titleText = entry.getDisplay().getTitle().getString();
        }
        if (titleText == null || titleText.isEmpty()) {
            titleText = entry.getId().getPath();
            if (titleText.contains("/")) {
                String[] split = titleText.split("/");
                titleText = split[split.length - 1];
            }
            titleText = titleText.replace("_", " ");
            if (!titleText.isEmpty()) {
                titleText = Character.toUpperCase(titleText.charAt(0)) + titleText.substring(1);
            }
        }
        
        if (titleText.length() > 22) {
            titleText = titleText.substring(0, 20) + "..";
        }
        
        String idStr = entry.getId().toString();
        if (idStr.length() > 30) {
            idStr = idStr.substring(0, 28) + "..";
        }
        
        guiGraphics.drawString(this.font, titleText, x + 6, y + 2, hovered ? 0xFFFFD700 : 0xFFFFFF, false);
        guiGraphics.drawString(this.font, idStr, x + 6, y + 11, 0x888888, false);
    }

    @Override
    protected void onClickEntry(Advancement entry) {
        this.onSelect.accept(entry);
    }
}
