package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class MasteryInfoScreen extends Screen {
    private final PlayerData playerData;

    public MasteryInfoScreen(PlayerData playerData) {
        super(Component.literal("Tu Maestría (XAM)"));
        this.playerData = playerData;
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 100;
        int btnHeight = 20;
        int x = this.width / 2 - btnWidth / 2;
        int y = this.height / 2 + 40;

        Button closeBtn = Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(x, y, btnWidth, btnHeight).build();
        this.addRenderableWidget(closeBtn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);

        String currentPathName = "Ninguno";
        if (playerData != null && playerData.getCurrentPath() != null) {
            for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (path.id.equals(playerData.getCurrentPath())) {
                    currentPathName = path.name;
                    break;
                }
            }
        }
        guiGraphics.drawCenteredString(this.font, "Rama Activa: " + currentPathName, this.width / 2, this.height / 2 - 30, 0x55FF55);

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
        guiGraphics.drawCenteredString(this.font, "Ramas Dominadas: " + mastered.toString(), this.width / 2, this.height / 2 - 10, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
