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

        Button closeBtn = Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(panelX + panelWidth / 2 - btnWidth / 2, panelY + panelHeight - 30, btnWidth, btnHeight).build();
        this.addRenderableWidget(closeBtn);
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
        String currentPathName = "Ninguno";
        int activeColor = 0xFF5555; // Red if none
        if (playerData != null && playerData.getCurrentPath() != null) {
            activeColor = 0x55FF55; // Green if active
            for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (path.id.equals(playerData.getCurrentPath())) {
                    currentPathName = path.name;
                    break;
                }
            }
        }
        guiGraphics.drawString(this.font, "Rama Activa:", panelX + 15, panelY + 50, 0x888888, false);
        guiGraphics.drawString(this.font, currentPathName, panelX + 15, panelY + 62, activeColor, false);

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
