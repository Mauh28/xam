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
        if (connection == null) return;
        
        var clientAdvs = connection.getAdvancements();
        var advList = clientAdvs.getAdvancements();
        if (advList == null) return;

        java.util.Set<Advancement> uniqueAdvs = new java.util.LinkedHashSet<>();

        // Source 0: Integrated Server (Complete advancements list)
        try {
            var server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                var serverAdvs = server.getAdvancements().getAllAdvancements();
                for (Advancement adv : serverAdvs) {
                    if (adv != null) {
                        uniqueAdvs.add(adv);
                    }
                }
            }
        } catch (Exception ignored) {}

        // Source 1: getAllAdvancements()
        try {
            java.lang.reflect.Method m = advList.getClass().getMethod("getAllAdvancements");
            @SuppressWarnings("unchecked")
            java.util.Collection<Advancement> coll = (java.util.Collection<Advancement>) m.invoke(advList);
            if (coll != null) {
                for (Advancement adv : coll) {
                    if (adv != null) uniqueAdvs.add(adv);
                }
            }
        } catch (Exception ignored) {
            try {
                java.lang.reflect.Method m = advList.getClass().getMethod("m_137965_");
                @SuppressWarnings("unchecked")
                java.util.Collection<Advancement> coll = (java.util.Collection<Advancement>) m.invoke(advList);
                if (coll != null) {
                    for (Advancement adv : coll) {
                        if (adv != null) uniqueAdvs.add(adv);
                    }
                }
            } catch (Exception ignored2) {}
        }

        // Source 2: roots traversal
        java.lang.Iterable<Advancement> roots = null;
        try {
            java.lang.reflect.Method m = advList.getClass().getMethod("getRoots");
            roots = (java.lang.Iterable<Advancement>) m.invoke(advList);
        } catch (Exception ignored) {
            try {
                java.lang.reflect.Method m = advList.getClass().getMethod("m_137976_");
                roots = (java.lang.Iterable<Advancement>) m.invoke(advList);
            } catch (Exception ignored2) {}
        }
        
        if (roots != null) {
            java.util.Set<Advancement> visited = new java.util.HashSet<>();
            for (Advancement root : roots) {
                traverseRoots(root, visited, uniqueAdvs);
            }
        }

        // Source 3: reflectively check all fields in AdvancementList (Maps or Collections)
        try {
            for (java.lang.reflect.Field field : advList.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object val = field.get(advList);
                if (val instanceof java.util.Map) {
                    for (Object v : ((java.util.Map<?, ?>) val).values()) {
                        if (v instanceof Advancement) {
                            uniqueAdvs.add((Advancement) v);
                        }
                    }
                } else if (val instanceof java.util.Collection) {
                    for (Object v : (java.util.Collection<?>) val) {
                        if (v instanceof Advancement) {
                            uniqueAdvs.add((Advancement) v);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        this.allEntries.addAll(uniqueAdvs);
    }

    private void traverseRoots(Advancement adv, java.util.Set<Advancement> visited, java.util.Set<Advancement> result) {
        if (adv == null || !visited.add(adv)) return;
        result.add(adv);
        
        java.util.Collection<Advancement> children = null;
        try {
            java.lang.reflect.Method m = adv.getClass().getMethod("getChildren");
            children = (java.util.Collection<Advancement>) m.invoke(adv);
        } catch (Exception ignored) {
            try {
                java.lang.reflect.Method m = adv.getClass().getMethod("m_137950_");
                children = (java.util.Collection<Advancement>) m.invoke(adv);
            } catch (Exception ignored2) {}
        }
        
        if (children != null) {
            for (Advancement child : children) {
                traverseRoots(child, visited, result);
            }
        } else {
            try {
                for (java.lang.reflect.Field field : adv.getClass().getDeclaredFields()) {
                    if (java.util.Collection.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        java.util.Collection<?> coll = (java.util.Collection<?>) field.get(adv);
                        if (coll != null) {
                            for (Object val : coll) {
                                if (val instanceof Advancement) {
                                    traverseRoots((Advancement) val, visited, result);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void filterEntries(String query) {
        this.filteredEntries.clear();
        String q = query.toLowerCase();
        String nsFilter = getNamespaceFilter().toLowerCase();
        for (Advancement adv : this.allEntries) {
            String idStr = adv.getId().toString().toLowerCase();
            String titleText = "";
            if (adv.getDisplay() != null) {
                titleText = adv.getDisplay().getTitle().getString().toLowerCase();
            }
            if (!nsFilter.isEmpty() && !adv.getId().getNamespace().toLowerCase().contains(nsFilter)) {
                continue;
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
        
        guiGraphics.drawString(this.font, titleText, x + 6, y + 2, hovered ? COLOR_BRASS : 0xFFFFFF, false);
        guiGraphics.drawString(this.font, idStr, x + 6, y + 11, 0x888888, false);
    }

    @Override
    protected void onClickEntry(Advancement entry) {
        this.onSelect.accept(entry);
    }
}
