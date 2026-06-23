package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class MasteryListScreen extends Screen {
    private final PlayerData playerData;
    private final Screen parent;
    private xdAbsoluteMastery.ConfigManager.PathInfo selectedPath;

    public MasteryListScreen(PlayerData playerData, Screen parent) {
        super(Component.literal("Ramas de Maestría"));
        this.playerData = playerData;
        this.parent = parent;
        
        // Select active path by default, otherwise first path in config
        if (playerData != null && playerData.getCurrentPath() != null) {
            for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (path.id.equals(playerData.getCurrentPath())) {
                    this.selectedPath = path;
                    break;
                }
            }
        }
        
        if (this.selectedPath == null && !xdAbsoluteMastery.ConfigManager.PATHS.isEmpty()) {
            this.selectedPath = xdAbsoluteMastery.ConfigManager.PATHS.get(0);
        }
    }

    public xdAbsoluteMastery.ConfigManager.PathInfo getSelectedPath() {
        return this.selectedPath;
    }

    public void setSelectedPath(xdAbsoluteMastery.ConfigManager.PathInfo path) {
        this.selectedPath = path;
    }

    @Override
    protected void init() {
        super.init();
        
        int panelWidth = 320;
        int panelHeight = 180;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelY = this.height / 2 - panelHeight / 2;

        // Add path selection buttons on the left
        int listX = panelX + 12;
        int listY = panelY + 35;
        int listWidth = 120;
        int listHeight = 20;

        for (int i = 0; i < xdAbsoluteMastery.ConfigManager.PATHS.size(); i++) {
            var path = xdAbsoluteMastery.ConfigManager.PATHS.get(i);
            PathButton pathBtn = new PathButton(listX, listY + i * 24, listWidth, listHeight, path, this);
            this.addRenderableWidget(pathBtn);
        }

        // Add Back and Edit buttons at the bottom of the panel
        int backBtnWidth = 80;
        int backBtnHeight = 20;
        
        int backBtnX = panelX + panelWidth / 2 - backBtnWidth / 2;
        Button backBtn = Button.builder(Component.literal("Volver"), b -> {
            Minecraft.getInstance().setScreen(this.parent);
        }).bounds(backBtnX, panelY + panelHeight - 28, backBtnWidth, backBtnHeight).build();
        this.addRenderableWidget(backBtn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int panelWidth = 320;
        int panelHeight = 180;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelY = this.height / 2 - panelHeight / 2;

        // Draw Panel Background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD0F0F12);
        // Draw Gold Border
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFFFFD700);

        // Draw Title
        guiGraphics.drawString(this.font, "RAMAS DISPONIBLES", panelX + 15, panelY + 15, 0xFFFFD700, false);

        // Draw Separator Line between left and right columns
        int sepX = panelX + 140;
        guiGraphics.fill(sepX, panelY + 10, sepX + 1, panelY + panelHeight - 35, 0x33FFFFFF);

        // Render Details on the Right side
        if (selectedPath != null) {
            // Path Name
            guiGraphics.drawString(this.font, selectedPath.name.toUpperCase(), sepX + 10, panelY + 15, 0xFFFFFF, false);

            // Status Badge
            String statusText;
            int statusColor;
            boolean isCompleted = playerData != null && playerData.getMasteredPaths().contains(selectedPath.id);
            boolean isActive = playerData != null && selectedPath.id.equals(playerData.getCurrentPath());
            
            if (isCompleted) {
                statusText = "DOMINADO";
                statusColor = 0xFFFFD700; // Gold
            } else if (isActive) {
                statusText = "ACTIVO";
                statusColor = 0x55FF55; // Green
            } else if (playerData != null && playerData.getCurrentPath() != null) {
                statusText = "BLOQUEADO";
                statusColor = 0xFF5555; // Red
            } else {
                statusText = "DISPONIBLE";
                statusColor = 0x55FFFF; // Cyan
            }

            guiGraphics.drawString(this.font, "Estado: ", sepX + 10, panelY + 35, 0x888888, false);
            guiGraphics.drawString(this.font, statusText, sepX + 55, panelY + 35, statusColor, false);

            // Requirements
            guiGraphics.drawString(this.font, "Requisitos:", sepX + 10, panelY + 52, 0x888888, false);

            int startReqY = panelY + 66;
            for (int i = 0; i < selectedPath.requirements.size(); i++) {
                xdAbsoluteMastery.ConfigManager.Requirement req = selectedPath.requirements.get(i);
                boolean completed = isRequirementCompletedClient(req);
                String prefix = completed ? "[✔] " : "[✘] ";
                int color = completed ? 0x55FF55 : 0xAAAAAA;
                String label = prefix + formatRequirement(req);
                guiGraphics.drawString(this.font, label, sepX + 10, startReqY + i * 12, color, false);

                // If hovered, render tooltip description
                int labelWidth = this.font.width(label);
                if (mouseX >= sepX + 10 && mouseX < sepX + 10 + labelWidth && mouseY >= startReqY + i * 12 && mouseY < startReqY + i * 12 + 10) {
                    if (req.description != null && !req.description.isEmpty()) {
                        guiGraphics.renderTooltip(this.font, Component.literal(req.description), mouseX, mouseY);
                    }
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public boolean isRequirementCompletedClient(xdAbsoluteMastery.ConfigManager.Requirement req) {
        if (req.type.equals("advancement")) {
            return isAdvancementCompleted(req.id);
        } else {
            String reqKey = req.type + ":" + req.id;
            return playerData != null && playerData.getCompletedRequirements().contains(reqKey);
        }
    }

    public String formatRequirement(xdAbsoluteMastery.ConfigManager.Requirement req) {
        if (req.name != null && !req.name.isEmpty()) {
            return req.name;
        }
        String name = req.id;
        if (name.contains(":")) {
            name = name.split(":")[1];
        }
        if (name.contains("/")) {
            String[] split = name.split("/");
            name = split[split.length - 1];
        }
        name = name.replace("_", " ");
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        
        switch (req.type) {
            case "advancement":
                return "Logro: " + name;
            case "craft":
                return "Craft: " + name;
            case "collect":
                return "Recoger: " + name;
            case "kill":
                return "Derrotar: " + name;
            default:
                return req.type + ": " + name;
        }
    }

    private boolean isAdvancementCompleted(String advIdStr) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            var clientAdvs = connection.getAdvancements();
            var adv = clientAdvs.getAdvancements().get(new ResourceLocation(advIdStr));
            if (adv != null) {
                try {
                    var field = clientAdvs.getClass().getDeclaredField("progress");
                    field.setAccessible(true);
                    var progressMap = (java.util.Map<?, ?>) field.get(clientAdvs);
                    var progress = (net.minecraft.advancements.AdvancementProgress) progressMap.get(adv);
                    return progress != null && progress.isDone();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private static class PathButton extends Button {
        private final xdAbsoluteMastery.ConfigManager.PathInfo path;
        private final MasteryListScreen screen;

        public PathButton(int x, int y, int width, int height, xdAbsoluteMastery.ConfigManager.PathInfo path, MasteryListScreen screen) {
            super(x, y, width, height, Component.literal(path.name), b -> {
                screen.setSelectedPath(path);
            }, supplier -> supplier.get());
            this.path = path;
            this.screen = screen;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean isSelected = screen.getSelectedPath() == path;
            if (isSelected) {
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xDD15151A);
                guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, 0xFFFFD700);
                int textWidth = Minecraft.getInstance().font.width(this.getMessage());
                int textX = this.getX() + (this.width - textWidth) / 2;
                guiGraphics.drawString(Minecraft.getInstance().font, this.getMessage(), textX, this.getY() + 6, 0xFFFFD700, false);
            } else {
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
}
