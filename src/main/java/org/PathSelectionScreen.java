package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PathSelectionScreen extends Screen {
    private final PlayerData playerData;

    public PathSelectionScreen(PlayerData playerData) {
        super(Component.literal("Selecciona tu Rama de Maestría"));
        this.playerData = playerData;
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 200;
        int btnHeight = 20;
        int x = this.width / 2 - btnWidth / 2;

        List<xdAbsoluteMastery.ConfigManager.PathInfo> availablePaths = new ArrayList<>();
        for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
            if (playerData == null || !playerData.getMasteredPaths().contains(path.id)) {
                availablePaths.add(path);
            }
        }

        int startY = this.height / 2 - (availablePaths.size() * 24) / 2;

        for (int i = 0; i < availablePaths.size(); i++) {
            xdAbsoluteMastery.ConfigManager.PathInfo path = availablePaths.get(i);
            Button btn = Button.builder(Component.literal(path.name), b -> {
                xdAbsoluteMastery.CHANNEL.sendToServer(new xdAbsoluteMastery.SelectPathPacket(path.id));
                this.onClose();
            }).bounds(x, startY + i * 24, btnWidth, btnHeight).build();

            this.addRenderableWidget(btn);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Prevent closing the screen without picking a path if currentPath is null and paths are available
        if (playerData != null && playerData.getCurrentPath() == null) {
            for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (!playerData.getMasteredPaths().contains(path.id)) {
                    return false;
                }
            }
        }
        return true;
    }
}
