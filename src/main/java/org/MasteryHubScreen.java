package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class MasteryHubScreen extends AbstractMasteryScreen {
    private final PlayerData playerData;
    private double scrollY = 0;

    public MasteryHubScreen(PlayerData playerData) {
        super(Component.literal("Sistema de Maestría"));
        this.playerData = playerData;
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, "SISTEMA DE MAESTRÍA", containerX + 15, titleY, TEXT_SECONDARY, false);
        drawCloseButton(graphics, mouseX, mouseY);
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
        // Draw container base and headers/footers
        super.render(graphics, mouseX, mouseY, partialTick);

        // Layout variables
        int leftW = (int) (containerW * 0.35);
        int leftH = bodyH - 20;
        int leftX = containerX + 10;
        int leftY = bodyY + 10;

        int rightX = leftX + leftW + 10;
        int rightW = containerW - leftW - 30;
        int rightH = bodyH - 20;
        int rightY = bodyY + 10;

        // --- LEFT PANEL: Active branch progress & mastered list ---
        drawFlatPanel(graphics, leftX, leftY, leftW, leftH, currentWidgetBg, currentBorderStd);
        graphics.drawString(this.font, "PROGRESO", leftX + 12, leftY + 10, COLOR_BRASS, false);

        String activePathId = playerData != null ? playerData.getCurrentPath() : null;
        xdAbsoluteMastery.ConfigManager.PathInfo activePath = null;
        if (activePathId != null) {
            for (xdAbsoluteMastery.ConfigManager.PathInfo p : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (p.id.equals(activePathId)) {
                    activePath = p;
                    break;
                }
            }
        }

        if (activePath != null) {
            // Icon slot background
            int iconX = leftX + 12;
            int iconY = leftY + 24;
            int iconW = 20;
            drawFlatPanel(graphics, iconX, iconY, iconW, iconW, INPUT_BACKGROUND, COLOR_COPPER);

            // Icon stack
            net.minecraft.world.item.ItemStack branchIconStack = net.minecraft.world.item.ItemStack.EMPTY;
            if (activePath.icon != null) {
                net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(activePath.icon));
                if (item != null) {
                    branchIconStack = new net.minecraft.world.item.ItemStack(item);
                }
            }
            if (branchIconStack.isEmpty()) {
                branchIconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
            }
            graphics.renderFakeItem(branchIconStack, iconX + 2, iconY + 2);

            // Active Path Name and Mod Namespace
            String pathName = activePath.name;
            int maxNameW = leftW - 45;
            if (this.font.width(pathName) > maxNameW) {
                pathName = this.font.plainSubstrByWidth(pathName, maxNameW - 10) + "...";
            }
            graphics.drawString(this.font, pathName, leftX + 38, leftY + 25, TEXT_PRIMARY, false);
            graphics.drawString(this.font, activePath.mod_id, leftX + 38, leftY + 35, TEXT_MUTED, false);

            // Calculate progress
            int totalReqs = activePath.requirements.size();
            int completedReqs = 0;
            if (this.minecraft.player != null) {
                completedReqs = xdAbsoluteMastery.getCompletedRequirementsCount(this.minecraft.player, playerData, activePath);
            }
            double pct = totalReqs > 0 ? (double) completedReqs / totalReqs : 0.0;
            int pctInt = (int) (pct * 100);

            // Progress text
            graphics.drawString(this.font, "Completado: " + pctInt + "%", leftX + 12, leftY + 54, COLOR_BRASS, false);
            graphics.drawString(this.font, completedReqs + " / " + totalReqs + " tareas", leftX + 12, leftY + 66, TEXT_SECONDARY, false);

            // Progress bar box
            int barX = leftX + 12;
            int barY = leftY + 80;
            int barW = leftW - 24;
            int barH = 10;
            drawFlatPanel(graphics, barX, barY, barW, barH, 0xFF140F0D, 0xFF2C221D);
            if (completedReqs > 0) {
                int fillW = (int) (barW * pct);
                graphics.fill(barX + 1, barY + 1, barX + fillW - 1, barY + barH - 1, COLOR_COPPER);
            }
        } else {
            graphics.drawString(this.font, "Ninguna Rama Activa", leftX + 12, leftY + 30, TEXT_MUTED, false);
            graphics.drawString(this.font, "Selecciona una en 'Elegir Rama'", leftX + 12, leftY + 45, TEXT_SECONDARY, false);
        }

        // Ramas Empezadas list
        int empY = leftY + 98;
        graphics.drawString(this.font, "EN PROGRESO", leftX + 12, empY, COLOR_BRASS, false);
        graphics.fill(leftX + 12, empY + 11, leftX + leftW - 12, empY + 12, 0xFF2C221D);

        int empListStartY = empY + 16;
        int empCount = 0;
        if (playerData != null && playerData.getStartedPaths() != null) {
            for (String id : playerData.getStartedPaths()) {
                if (id.equals(activePathId) || playerData.getMasteredPaths().contains(id)) {
                    continue;
                }
                String name = id;
                for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                    if (path.id.equals(id)) {
                        name = path.name;
                        break;
                    }
                }
                int maxEmpNameW = leftW - 24;
                if (this.font.width(name) > maxEmpNameW) {
                    name = this.font.plainSubstrByWidth(name, maxEmpNameW - 10) + "...";
                }
                graphics.drawString(this.font, "▶ " + name, leftX + 12, empListStartY + empCount * 11, TEXT_SECONDARY, false);
                empCount++;
                if (empListStartY + (empCount + 1) * 11 >= leftY + 150) {
                    break;
                }
            }
        }
        if (empCount == 0) {
            graphics.drawString(this.font, "Ninguna", leftX + 12, empListStartY, TEXT_MUTED, false);
        }

        // Ramas Dominadas list
        int domY = leftY + 152;
        graphics.drawString(this.font, "RAMAS DOMINADAS", leftX + 12, domY, COLOR_BRASS, false);
        graphics.fill(leftX + 12, domY + 11, leftX + leftW - 12, domY + 12, 0xFF2C221D);

        int domListStartY = domY + 16;
        int domCount = 0;
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
                int maxDomNameW = leftW - 24;
                if (this.font.width(name) > maxDomNameW) {
                    name = this.font.plainSubstrByWidth(name, maxDomNameW - 10) + "...";
                }
                graphics.drawString(this.font, "✔ " + name, leftX + 12, domListStartY + domCount * 11, TEXT_PRIMARY, false);
                domCount++;
                if (domListStartY + (domCount + 1) * 11 >= leftY + leftH) {
                    break;
                }
            }
        }
        if (domCount == 0) {
            graphics.drawString(this.font, "Ninguna", leftX + 12, domListStartY, TEXT_MUTED, false);
        }

        // --- RIGHT PANEL: Tasks details list ---
        drawFlatPanel(graphics, rightX, rightY, rightW, rightH, currentWidgetBg, currentBorderStd);
        graphics.drawString(this.font, "TAREAS Y REQUISITOS", rightX + 15, rightY + 10, COLOR_BRASS, false);

        if (activePath == null) {
            graphics.drawCenteredString(this.font, "No hay tareas activas disponibles.", rightX + rightW / 2, rightY + rightH / 2 - 10, TEXT_MUTED);
            graphics.drawCenteredString(this.font, "Equipa una rama para comenzar tu camino.", rightX + rightW / 2, rightY + rightH / 2 + 5, TEXT_SECONDARY);
        } else {
            int listX = rightX + 15;
            int listY = rightY + 26;
            int listW = rightW - 30;
            int listH = rightH - 35;

            int cardH = 38;
            int gap = 6;
            int totalReqsH = activePath.requirements.size() * (cardH + gap);

            if (scrollY > Math.max(0, totalReqsH - listH)) {
                scrollY = Math.max(0, totalReqsH - listH);
            }

            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            int scissorX = (int) (listX * guiScale);
            int scissorY = (int) ((Minecraft.getInstance().getWindow().getGuiScaledHeight() - (listY + listH)) * guiScale);
            int scissorW = (int) (listW * guiScale);
            int scissorH = (int) (listH * guiScale);

            com.mojang.blaze3d.systems.RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

            for (int i = 0; i < activePath.requirements.size(); i++) {
                xdAbsoluteMastery.ConfigManager.Requirement req = activePath.requirements.get(i);
                int cardY = listY + i * (cardH + gap) - (int) scrollY;

                // Draw task card background based on completed status
                boolean isCompleted = xdAbsoluteMastery.isRequirementCompleted(this.minecraft.player, playerData, activePath.id, req);
                int cardBg = isCompleted ? 0xFF17251C : PANEL_INNER_BG;
                int cardBorder = isCompleted ? 0xFF2A593E : WARM_BORDER;

                drawFlatPanel(graphics, listX, cardY, listW, cardH, cardBg, cardBorder);

                // Draw checkmark or cross indicator
                if (isCompleted) {
                    graphics.drawString(this.font, "✔", listX + 10, cardY + (cardH - 8) / 2, 0xFF4ADE80, false);
                } else {
                    graphics.drawString(this.font, "✘", listX + 10, cardY + (cardH - 8) / 2, 0xFFF87171, false);
                }

                // Task Name
                String reqNameText = req.name.isEmpty() ? req.id : req.name;
                int reqNameMaxW = listW - 120;
                if (this.font.width(reqNameText) > reqNameMaxW) {
                    reqNameText = this.font.plainSubstrByWidth(reqNameText, reqNameMaxW - 10) + "...";
                }
                graphics.drawString(this.font, reqNameText, listX + 25, cardY + 6, isCompleted ? 0xFFBBF7D0 : TEXT_PRIMARY, false);

                // Task Description
                String reqDescText = req.description;
                if (reqDescText.isEmpty()) reqDescText = req.id;
                if (this.font.width(reqDescText) > reqNameMaxW) {
                    reqDescText = this.font.plainSubstrByWidth(reqDescText, reqNameMaxW - 10) + "...";
                }
                graphics.drawString(this.font, reqDescText, listX + 25, cardY + 20, isCompleted ? 0xFF86EFAC : TEXT_SECONDARY, false);

                // Task Type badge on the right
                String badge = req.type.toUpperCase();
                int badgeW = this.font.width(badge) + 10;
                int badgeX = listX + listW - badgeW - 10;
                int badgeY = cardY + (cardH - 12) / 2;
                drawFlatPanel(graphics, badgeX, badgeY, badgeW, 12, 0xFF140F0D, 0xFF2C221D);
                graphics.drawCenteredString(this.font, badge, badgeX + badgeW / 2, badgeY + 2, COLOR_BRASS);
            }

            com.mojang.blaze3d.systems.RenderSystem.disableScissor();

            // Render scrollbar if needed
            if (totalReqsH > listH) {
                int scrollbarX = rightX + rightW - 10;
                int scrollbarY = listY;
                int thumbHeight = Math.max(15, (int) (((float) listH / totalReqsH) * listH));
                int maxScrollY = totalReqsH - listH;
                int thumbY = scrollbarY + (int) ((scrollY / maxScrollY) * (listH - thumbHeight));

                drawFlatPanel(graphics, scrollbarX, scrollbarY, 4, listH, 0xFF140F0D, 0xFF2C221D);
                drawFlatPanel(graphics, scrollbarX, thumbY, 4, thumbHeight, COLOR_COPPER, COLOR_BRASS);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (playerData != null && playerData.getCurrentPath() != null) {
            xdAbsoluteMastery.ConfigManager.PathInfo activePath = xdAbsoluteMastery.ConfigManager.PATHS_MAP.get(playerData.getCurrentPath());
            if (activePath != null) {
                int rightH = bodyH - 20;
                int listH = rightH - 35;
                int cardH = 38;
                int gap = 6;
                int totalReqsH = activePath.requirements.size() * (cardH + gap);
                if (totalReqsH > listH) {
                    scrollY = Math.max(0, Math.min(totalReqsH - listH, scrollY - delta * 12));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Header Close Button
            if (isCloseButtonClicked(mouseX, mouseY)) {
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
                    this.minecraft.setScreen(new PathSelectionScreen(this, playerData));
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
                    this.minecraft.setScreen(new PathSelectionScreen(this, playerData));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
