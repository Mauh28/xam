package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.minecraft.resources.ResourceLocation;

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
    private static final ResourceLocation FALLBACK_ICON = new ResourceLocation("minecraft", "textures/item/writable_book.png");
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
                            ResourceLocation resLoc = new ResourceLocation("xam", "textures/mod_logos/" + modId.toLowerCase());
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
    protected void renderEntry(GuiGraphics guiGraphics, String entry, int x, int y, int index, boolean hovered) {
        TextureInfo info = null;
        if (!entry.equals("Todos")) {
            info = getOrCreateModLogo(entry);
        }
        if (info == null) {
            info = FALLBACK_INFO;
        }

        // Draw mod logo icon
        guiGraphics.blit(info.resourceLocation, x + 6, y + (this.entryHeight - 16) / 2, 16, 16, 0.0F, 0.0F, info.width, info.height, info.width, info.height);

        // Draw mod name/id text shifted to the right
        guiGraphics.drawString(this.font, entry, x + 28, y + (this.entryHeight - 8) / 2, hovered ? COLOR_BRASS : 0xFFFFFF, false);
    }

    @Override
    protected void onClickEntry(String entry) {
        this.onSelect.accept(entry);
    }
}
