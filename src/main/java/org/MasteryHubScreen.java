package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class MasteryHubScreen extends AbstractMasteryScreen {
    private final PlayerData playerData;

    public MasteryHubScreen(PlayerData playerData) {
        super(Component.literal("Sistema de Maestría"));
        this.playerData = playerData;
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, "SISTEMA DE MAESTRÍA", containerX + 15, titleY, TEXT_SECONDARY, false);

        int closeW = 60;
        int closeH = 20;
        int closeX = containerX + containerW - 15 - closeW;
        int closeY = containerY + (headerH - closeH) / 2;
        drawFlatButton(graphics, closeX, closeY, closeW, closeH, "Cerrar", mouseX, mouseY, true);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean isOp = this.minecraft.player != null && (this.minecraft.player.hasPermissions(2) || this.minecraft.player.getAbilities().instabuild);
        int btnW = 120;
        int btnH = 20;
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        if (isOp) {
            int totalW = btnW + 20 + btnW;
            int startX = containerX + (containerW - totalW) / 2;
            drawFlatButton(graphics, startX, btnY, btnW, btnH, "Elegir Rama", mouseX, mouseY, true, false);
            drawFlatButton(graphics, startX + btnW + 20, btnY, btnW, btnH, "[OP] Editor Maestrías", mouseX, mouseY, true, true);
        } else {
            int startX = containerX + (containerW - btnW) / 2;
            drawFlatButton(graphics, startX, btnY, btnW, btnH, "Elegir Rama", mouseX, mouseY, true, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw container base and headers/footers (with correct nativo blurred screen background)
        super.render(graphics, mouseX, mouseY, partialTick);

        // Body area does NOT have its own filled background, keeping PANEL_BACKGROUND from container panel.
        int panelW = (int) (containerW * 0.60);
        int panelH = (int) (bodyH * 0.50);
        int panelX = containerX + (containerW - panelW) / 2;
        int panelY = bodyY + (bodyH - panelH) / 2;

        drawFlatPanel(graphics, panelX, panelY, panelW, panelH, WIDGET_BACKGROUND, BORDER_STANDARD);

        String activePath = "Ninguna";
        int activeColor = TEXT_MUTED;
        if (playerData != null && playerData.getCurrentPath() != null) {
            for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (path.id.equals(playerData.getCurrentPath())) {
                    activePath = path.name;
                    activeColor = TEXT_PRIMARY;
                    break;
                }
            }
        }

        String masteredPaths = "Ninguna";
        int masteredColor = TEXT_MUTED;
        if (playerData != null && !playerData.getMasteredPaths().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < playerData.getMasteredPaths().size(); i++) {
                String id = playerData.getMasteredPaths().get(i);
                String name = id;
                for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                    if (path.id.equals(id)) {
                        name = path.name;
                        break;
                    }
                }
                sb.append(name);
                if (i < playerData.getMasteredPaths().size() - 1) {
                    sb.append(", ");
                }
            }
            masteredPaths = sb.toString();
            masteredColor = TEXT_PRIMARY;
        }

        // Row 1: Rama Activa
        int row1Y = panelY + (panelH / 2 - 8) / 2;
        graphics.drawString(this.font, "Rama Activa:", panelX + 20, row1Y, TEXT_SECONDARY, false);
        
        int activeTextW = this.font.width(activePath);
        int maxValW = panelW / 2 - 25;
        if (activeTextW > maxValW) {
            activePath = this.font.plainSubstrByWidth(activePath, maxValW - 10) + "...";
            activeTextW = this.font.width(activePath);
        }
        graphics.drawString(this.font, activePath, panelX + panelW - 20 - activeTextW, row1Y, activeColor, false);

        // Stats Separator Line: 1px (0xFF333333)
        int sepY = panelY + panelH / 2;
        graphics.fill(panelX + 20, sepY, panelX + panelW - 20, sepY + 1, 0xFF333333);

        // Row 2: Ramas Dominadas
        int row2Y = panelY + panelH / 2 + (panelH / 2 - 8) / 2;
        graphics.drawString(this.font, "Ramas Dominadas:", panelX + 20, row2Y, TEXT_SECONDARY, false);
        
        int masteredTextW = this.font.width(masteredPaths);
        if (masteredTextW > maxValW) {
            masteredPaths = this.font.plainSubstrByWidth(masteredPaths, maxValW - 10) + "...";
            masteredTextW = this.font.width(masteredPaths);
        }
        graphics.drawString(this.font, masteredPaths, panelX + panelW - 20 - masteredTextW, row2Y, masteredColor, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Header Close Button
            int closeW = 60;
            int closeH = 20;
            int closeX = containerX + containerW - 15 - closeW;
            int closeY = containerY + (headerH - closeH) / 2;
            if (mouseX >= closeX && mouseX < closeX + closeW && mouseY >= closeY && mouseY < closeY + closeH) {
                playClickSound();
                this.onClose();
                return true;
            }

            // Footer Buttons
            boolean isOp = this.minecraft.player != null && (this.minecraft.player.hasPermissions(2) || this.minecraft.player.getAbilities().instabuild);
            int btnW = 120;
            int btnH = 20;
            int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;
            if (isOp) {
                int totalW = btnW + 20 + btnW;
                int startX = containerX + (containerW - totalW) / 2;
                // Elegir Rama
                if (mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    this.minecraft.setScreen(new PathSelectionScreen(playerData));
                    return true;
                }
                // Editor
                int editorX = startX + btnW + 20;
                if (mouseX >= editorX && mouseX < editorX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    this.minecraft.setScreen(new MasteryEditorScreen(this));
                    return true;
                }
            } else {
                int startX = containerX + (containerW - btnW) / 2;
                if (mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    this.minecraft.setScreen(new PathSelectionScreen(playerData));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
