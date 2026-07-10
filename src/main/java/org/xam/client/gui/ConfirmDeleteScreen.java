package org.xam.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// --- Extracted from MasteryEditorScreen nested class (issue #17 step 1/5) ---

public class ConfirmDeleteScreen extends AbstractMasteryScreen {
    private final Screen parent;
    private final Runnable onConfirm;
    private final String targetName;
    private final Screen returnScreen;

    public ConfirmDeleteScreen(Screen parent, Runnable onConfirm, String targetName, Screen returnScreen) {
        super(Component.translatable("xam.screen.mastery_editor.confirm_delete.title"));
        this.parent = parent;
        this.onConfirm = onConfirm;
        this.targetName = targetName;
        this.returnScreen = returnScreen;
    }

    public ConfirmDeleteScreen(Screen parent, Runnable onConfirm, String targetName) {
        this(parent, onConfirm, targetName, parent);
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.confirm_delete.header").getString(), containerX + 15, titleY, TEXT_PRIMARY, false);
        drawBackButton(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnW = 100;
        int btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        // Confirmar button (danger/red hover)
        boolean confirmHovered = mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int confirmBg = confirmHovered ? 0xFF3A1111 : 0xFF140F0D;
        int confirmBorder = confirmHovered ? 0xFFFF5555 : 0xFF2C221D;
        drawFlatPanel(graphics, startX, btnY, btnW, btnH, confirmBg, confirmBorder);
        graphics.drawCenteredString(this.font, Component.translatable("xam.screen.mastery_editor.confirm_delete.btn_confirm").getString(), startX + btnW / 2, btnY + 6, confirmHovered ? TEXT_PRIMARY : TEXT_SECONDARY);

        // Regresar button (standard copper)
        drawFlatButton(graphics, startX + btnW + 10, btnY, btnW, btnH, Component.translatable("xam.screen.mastery_editor.confirm_delete.btn_back").getString(), mouseX, mouseY, true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int panelW = (int) (containerW * 0.80);
        int panelH = (int) (bodyH * 0.50);
        int panelX = containerX + (containerW - panelW) / 2;
        int panelY = bodyY + (bodyH - panelH) / 2;

        drawFlatPanel(graphics, panelX, panelY, panelW, panelH, PANEL_INNER_BG, WARM_BORDER);

        String text1 = Component.translatable("xam.screen.mastery_editor.confirm_delete.warn_sure").getString();
        String text2 = "\"" + targetName + "\"?";
        String text3 = Component.translatable("xam.screen.mastery_editor.confirm_delete.warn_undone").getString();

        graphics.drawCenteredString(this.font, text1, panelX + panelW / 2, panelY + 25, TEXT_PRIMARY);
        graphics.drawCenteredString(this.font, text2, panelX + panelW / 2, panelY + 45, COLOR_BRASS);
        graphics.drawCenteredString(this.font, text3, panelX + panelW / 2, panelY + 65, 0xFFFF5555);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isBackButtonClicked(mouseX, mouseY)) {
                playClickSound();
                Minecraft.getInstance().setScreen(this.parent);
                return true;
            }

            int btnW = 100;
            int btnH = 20;
            int startX = containerX + containerW - 15 - (btnW * 2 + 10);
            int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

            // Confirmar
            if (mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                onConfirm.run();
                Minecraft.getInstance().setScreen(this.returnScreen);
                return true;
            }

            // Regresar
            if (mouseX >= startX + btnW + 10 && mouseX < startX + btnW + 10 + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                Minecraft.getInstance().setScreen(this.parent);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
