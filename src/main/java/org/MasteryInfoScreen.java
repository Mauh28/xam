package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MasteryInfoScreen extends Screen {
    private final PlayerData playerData;

    public MasteryInfoScreen(PlayerData playerData) {
        super(Component.literal("Tu Maestría (XAM)"));
        this.playerData = playerData;
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 80;
        int btnHeight = 20;
        int panelWidth = 220;
        int panelHeight = 150;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelY = this.height / 2 - panelHeight / 2;

        int spacing = 10;
        int totalBtnWidth = (btnWidth * 2) + spacing;
        int startBtnX = panelX + (panelWidth - totalBtnWidth) / 2;

        Button verRamasBtn = Button.builder(Component.literal("Ver Ramas"), b -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(new MasteryListScreen(this.playerData, this));
        }).bounds(startBtnX, panelY + panelHeight - 30, btnWidth, btnHeight).build();

        Button closeBtn = Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(startBtnX + btnWidth + spacing, panelY + panelHeight - 30, btnWidth, btnHeight).build();

        this.addRenderableWidget(verRamasBtn);
        this.addRenderableWidget(closeBtn);

        boolean hasAvailablePaths = false;
        for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
            if (this.playerData == null || !this.playerData.getMasteredPaths().contains(path.id)) {
                hasAvailablePaths = true;
                break;
            }
        }

        if (this.playerData != null && this.playerData.getCurrentPath() == null && hasAvailablePaths) {
            Button selectBtn = Button.builder(Component.literal("Elegir Rama"), b -> {
                net.minecraft.client.Minecraft.getInstance().setScreen(new PathSelectionScreen(this.playerData));
            }).bounds(panelX + 15, panelY + 60, 90, 18).build();
            this.addRenderableWidget(selectBtn);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int panelWidth = 220;
        int panelHeight = 150;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelY = this.height / 2 - panelHeight / 2;

        // Draw Panel Background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD0F0F12);
        // Draw Gold Border
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFFFFD700);

        // Render Book Icon near title
        guiGraphics.renderFakeItem(new ItemStack(Items.WRITABLE_BOOK), panelX + 15, panelY + 15);

        // Draw Title
        guiGraphics.drawString(this.font, "MAESTRÍA ABSOLUTA", panelX + 38, panelY + 19, 0xFFFFD700, false);

        // Draw Separator Line
        guiGraphics.fill(panelX + 15, panelY + 38, panelX + panelWidth - 15, panelY + 39, 0x55FFFFFF);

        // Draw Active Path Section
        String currentPathName = null;
        int activeColor = 0x55FF55; // Green if active
        if (playerData != null && playerData.getCurrentPath() != null) {
            for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (path.id.equals(playerData.getCurrentPath())) {
                    currentPathName = path.name;
                    break;
                }
            }
        }
        guiGraphics.drawString(this.font, "Rama Activa:", panelX + 15, panelY + 50, 0x888888, false);
        if (currentPathName != null) {
            guiGraphics.drawString(this.font, currentPathName, panelX + 15, panelY + 62, activeColor, false);
        } else {
            boolean hasAvailablePaths = false;
            for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (playerData == null || !playerData.getMasteredPaths().contains(path.id)) {
                    hasAvailablePaths = true;
                    break;
                }
            }
            if (!hasAvailablePaths) {
                guiGraphics.drawString(this.font, "Todas Dominadas", panelX + 15, panelY + 62, 0x55FF55, false);
            }
        }

        // Draw Mastered Paths Section
        StringBuilder mastered = new StringBuilder();
        if (playerData != null && !playerData.getMasteredPaths().isEmpty()) {
            for (int i = 0; i < playerData.getMasteredPaths().size(); i++) {
                String id = playerData.getMasteredPaths().get(i);
                String name = id;
                for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                    if (path.id.equals(id)) {
                        name = path.name;
                        break;
                    }
                }
                mastered.append(name);
                if (i < playerData.getMasteredPaths().size() - 1) {
                    mastered.append(", ");
                }
            }
        } else {
            mastered.append("Ninguna");
        }
        guiGraphics.drawString(this.font, "Ramas Dominadas:", panelX + 15, panelY + 82, 0x888888, false);

        // Word wrap for mastered paths string if it is too long
        String masteredStr = mastered.toString();
        int textWidth = panelWidth - 30;
        int textY = panelY + 94;
        if (this.font.width(masteredStr) > textWidth) {
            var list = this.font.split(Component.literal(masteredStr), textWidth);
            for (int i = 0; i < Math.min(list.size(), 2); i++) {
                guiGraphics.drawString(this.font, list.get(i), panelX + 15, textY + i * 10, 0xDAA520, false); // Goldenrod color
            }
        } else {
            guiGraphics.drawString(this.font, masteredStr, panelX + 15, textY, 0xDAA520, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
