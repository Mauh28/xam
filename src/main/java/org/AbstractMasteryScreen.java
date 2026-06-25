package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbstractMasteryScreen extends Screen {
    // Design System Constants (ARGB)
    public static final int PANEL_BACKGROUND = 0xFF1E1E1E;
    public static final int WIDGET_BACKGROUND = 0xFF252525;
    public static final int WIDGET_INNER = 0xFF2A2A2A;
    public static final int INPUT_BACKGROUND = 0xFF111111;
    public static final int BORDER_STANDARD = 0xFF4E4E4E;
    public static final int BORDER_INNER = 0xFF3A3A3A;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;
    public static final int TEXT_MUTED = 0xFF888888;
    public static final int BUTTON_BACKGROUND = 0xFF353535;
    public static final int BUTTON_BORDER = 0xFF555555;
    public static final int BUTTON_HOVER_BG = 0xFF454545;
    public static final int BUTTON_HOVER_BORDER = 0xFF777777;
    public static final int ACCENT_OP_BORDER = 0xFFFFFFFF;

    // Relative container geometry
    protected int containerW;
    protected int containerH;
    protected int containerX;
    protected int containerY;
    
    protected int headerH;
    protected int footerH;
    protected int bodyH;
    protected int bodyY;

    protected AbstractMasteryScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        
        // Calculate relative sizing to exploit space on GUI Scale 3
        this.containerW = (int) (this.width * 0.9);
        this.containerH = (int) (this.height * 0.85);
        this.containerX = (this.width - this.containerW) / 2;
        this.containerY = (this.height - this.containerH) / 2;

        this.headerH = (int) (this.containerH * 0.10);
        this.footerH = (int) (this.containerH * 0.12);
        this.bodyH = this.containerH - this.headerH - this.footerH;
        this.bodyY = this.containerY + this.headerH;
    }

    // Helper: draw flat panel with 2px border
    public static void drawFlatPanel(GuiGraphics graphics, int x, int y, int w, int h, int colorFondo, int colorBorde) {
        graphics.fill(x, y, x + w, y + h, colorBorde);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, colorFondo);
    }

    // Helper: draw flat button with hover states
    public boolean drawFlatButton(GuiGraphics graphics, int x, int y, int w, int h, String text, int mouseX, int mouseY, boolean enabled, boolean isOp) {
        boolean hovered = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bg = hovered ? BUTTON_HOVER_BG : BUTTON_BACKGROUND;
        int border = isOp ? ACCENT_OP_BORDER : (hovered ? BUTTON_HOVER_BORDER : BUTTON_BORDER);
        if (!enabled) {
            bg = 0xFF222222;
            border = 0xFF444444;
        }
        drawFlatPanel(graphics, x, y, w, h, bg, border);
        int textColor = enabled ? (hovered ? TEXT_PRIMARY : TEXT_SECONDARY) : TEXT_MUTED;
        int textX = x + (w - this.font.width(text)) / 2;
        int textY = y + (h - 8) / 2;
        graphics.drawString(this.font, text, textX, textY, textColor, false);
        return hovered;
    }

    public boolean drawFlatButton(GuiGraphics graphics, int x, int y, int w, int h, String text, int mouseX, int mouseY, boolean enabled) {
        return drawFlatButton(graphics, x, y, w, h, text, mouseX, mouseY, enabled, false);
    }

    protected void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
                )
        );
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Nativo blur overlay de Minecraft
        super.renderBackground(graphics);
    }

    protected abstract void renderHeader(GuiGraphics graphics, int mouseX, int mouseY);
    protected abstract void renderFooter(GuiGraphics graphics, int mouseX, int mouseY);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        // Main Panel Container
        drawFlatPanel(graphics, containerX, containerY, containerW, containerH, PANEL_BACKGROUND, BORDER_STANDARD);

        // Header Background & Border
        graphics.fill(containerX + 2, containerY + 2, containerX + containerW - 2, containerY + headerH, WIDGET_BACKGROUND);
        graphics.fill(containerX + 2, containerY + headerH - 2, containerX + containerW - 2, containerY + headerH, BORDER_STANDARD);

        // Footer Background & Border
        graphics.fill(containerX + 2, containerY + containerH - footerH, containerX + containerW - 2, containerY + containerH - 2, WIDGET_BACKGROUND);
        graphics.fill(containerX + 2, containerY + containerH - footerH, containerX + containerW - 2, containerY + containerH - footerH + 2, BORDER_STANDARD);

        renderHeader(graphics, mouseX, mouseY);
        renderFooter(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
