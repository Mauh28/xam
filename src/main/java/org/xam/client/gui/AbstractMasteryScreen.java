package org.xam.client.gui;

import org.xam.XamConstants;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerData;
import org.xam.data.PlayerDataProvider;
import org.xam.network.XamNetwork;
import org.xam.network.SelectPathPacket;
import org.xam.network.UpdateConfigPacket;
import org.xam.progression.MasteryService;
import org.xam.progression.RequirementFormatter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbstractMasteryScreen extends Screen {
    // Design System Constants (ARGB) - Glassmorphism ready
    public static final int PANEL_BACKGROUND = 0xE01E1E1E;
    public static final int WIDGET_BACKGROUND = 0xD0252525;
    public static final int WIDGET_INNER = 0xD82A2A2A;
    public static final int INPUT_BACKGROUND = 0xD0111111;
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

    // Create Ponder Aesthetic Constants
    public static final int PONDER_BG = 0xD81C1A1A;
    public static final int COLOR_BRASS = 0xFFDF9E3F;
    public static final int COLOR_COPPER = 0xFFCD613C;
    public static final int COLOR_COPPER_HOVER = 0xFFE07853;
    public static final int PANEL_INNER_BG = 0xC8120E0D;
    public static final int WARM_BORDER = 0xFF2C221D;

    // Sizing and state
    protected int containerW;
    protected int containerH;
    protected int containerX;
    protected int containerY;
    
    protected int headerH;
    protected int footerH;
    protected int bodyH;
    protected int bodyY;

    protected long openTime = 0;

    // Overridable style variables
    protected int currentPanelBg = PONDER_BG;
    protected int currentWidgetBg = PANEL_INNER_BG;
    protected int currentBorderStd = WARM_BORDER;

    protected AbstractMasteryScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        if (this.openTime == 0) {
            this.openTime = System.currentTimeMillis();
        }
        
        // Calculate relative sizing to exploit space on GUI Scale 3, but fall back to full screen if resolution is too small
        if (this.width < 450) {
            this.containerW = this.width;
            this.containerX = 0;
        } else {
            this.containerW = (int) (this.width * 0.9);
            this.containerX = (this.width - this.containerW) / 2;
        }

        if (this.height < 280) {
            this.containerH = this.height;
            this.containerY = 0;
        } else {
            this.containerH = (int) (this.height * 0.85);
            this.containerY = (this.height - this.containerH) / 2;
        }

        this.headerH = (int) (this.containerH * 0.10);
        this.footerH = (int) (this.containerH * 0.12);
        this.bodyH = this.containerH - this.headerH - this.footerH;
        this.bodyY = this.containerY + this.headerH;
    }

    // Helper: adjust alpha of a color dynamically
    public static int adjustAlpha(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int newA = (int) (a * factor);
        return (newA << 24) | (r << 16) | (g << 8) | b;
    }

    // Helper: adjust brightness (lighten/darken) of a color
    public static int adjustColorBrightness(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        r = Math.max(0, Math.min(255, r + amount));
        g = Math.max(0, Math.min(255, g + amount));
        b = Math.max(0, Math.min(255, b + amount));
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Helper: get fade-in progress factor (0.0F to 1.0F)
    public static float getFadeProgress() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof AbstractMasteryScreen) {
            long elapsed = System.currentTimeMillis() - ((AbstractMasteryScreen) screen).openTime;
            return Math.min(1.0F, elapsed / 200.0F);
        }
        return 1.0F;
    }

    // Helper: draw flat panel with 2px beveled border
    public static void drawFlatPanel(GuiGraphics graphics, int x, int y, int w, int h, int colorFondo, int colorBorde) {
        float alphaFactor = getFadeProgress();
        int finalBg = adjustAlpha(colorFondo, alphaFactor);
        int finalBorder = adjustAlpha(colorBorde, alphaFactor);

        int alpha = (finalBorder >> 24) & 0xFF;
        int r = (finalBorder >> 16) & 0xFF;
        int g = (finalBorder >> 8) & 0xFF;
        int b = finalBorder & 0xFF;

        int hr = Math.min(255, r + 30);
        int hg = Math.min(255, g + 30);
        int hb = Math.min(255, b + 30);
        int highlightColor = (alpha << 24) | (hr << 16) | (hg << 8) | hb;

        int sr = Math.max(0, r - 20);
        int sg = Math.max(0, g - 20);
        int sb = Math.max(0, b - 20);
        int shadowColor = (alpha << 24) | (sr << 16) | (sg << 8) | sr; // ponytail: simple dark shade border fallback

        // Fill background
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, finalBg);

        // Draw bevel border lines
        graphics.fill(x, y, x + w, y + 2, highlightColor); // Top
        graphics.fill(x, y + 2, x + 2, y + h - 2, highlightColor); // Left
        graphics.fill(x, y + h - 2, x + w, y + h, shadowColor); // Bottom
        graphics.fill(x + w - 2, y + 2, x + w, y + h - 2, shadowColor); // Right
    }

    // Helper: draw panel with vertical gradient background and 2px beveled border
    public static void drawGradientPanel(GuiGraphics graphics, int x, int y, int w, int h, int colorFondoTop, int colorFondoBottom, int colorBorde) {
        float alphaFactor = getFadeProgress();
        int finalBgTop = adjustAlpha(colorFondoTop, alphaFactor);
        int finalBgBottom = adjustAlpha(colorFondoBottom, alphaFactor);
        int finalBorder = adjustAlpha(colorBorde, alphaFactor);

        int alpha = (finalBorder >> 24) & 0xFF;
        int r = (finalBorder >> 16) & 0xFF;
        int g = (finalBorder >> 8) & 0xFF;
        int b = finalBorder & 0xFF;

        int hr = Math.min(255, r + 30);
        int hg = Math.min(255, g + 30);
        int hb = Math.min(255, b + 30);
        int highlightColor = (alpha << 24) | (hr << 16) | (hg << 8) | hb;

        int sr = Math.max(0, r - 20);
        int sg = Math.max(0, g - 20);
        int sb = Math.max(0, b - 20);
        int shadowColor = (alpha << 24) | (sr << 16) | (sg << 8) | sb;

        // Fill background gradient
        graphics.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, finalBgTop, finalBgBottom);

        // Draw bevel border lines
        graphics.fill(x, y, x + w, y + 2, highlightColor); // Top
        graphics.fill(x, y + 2, x + 2, y + h - 2, highlightColor); // Left
        graphics.fill(x, y + h - 2, x + w, y + h, shadowColor); // Bottom
        graphics.fill(x + w - 2, y + 2, x + w, y + h - 2, shadowColor); // Right
    }

    // Helper: draw copper oxidized panel with golden sweep scanning border in ping-pong motion
    public static void drawScannedPanel(GuiGraphics graphics, int x, int y, int w, int h, int colorFondo, int colorBorde) {
        // Draw base beveled panel first
        drawFlatPanel(graphics, x, y, w, h, colorFondo, colorBorde);

        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        long time = level != null ? level.getGameTime() : System.currentTimeMillis() / 50;

        int perimeter = 2 * (w + h);
        int scanSize = 16; // Length of the scan glow
        int cycle = perimeter - scanSize;
        if (cycle <= 0) return;

        // Ping-pong travel position along the perimeter
        int t = (int) (time * 1.5);
        int currentPos;
        if ((t / cycle) % 2 == 0) {
            currentPos = t % cycle;
        } else {
            currentPos = cycle - (t % cycle);
        }

        float alphaFactor = getFadeProgress();
        int glowColor = adjustAlpha(COLOR_BRASS, alphaFactor);

        for (int offset = 0; offset < scanSize; offset++) {
            int p = currentPos + offset;
            if (p < 0 || p >= perimeter) continue;

            float intensity = 1.0f - Math.abs(offset - scanSize / 2.0f) / (scanSize / 2.0f);
            int col = adjustAlpha(glowColor, intensity * 0.85f);

            if (p < w) {
                // Top border
                graphics.fill(x + p, y, x + p + 1, y + 2, col);
            } else if (p < w + h) {
                // Right border
                int py = p - w;
                graphics.fill(x + w - 2, y + py, x + w, y + py + 1, col);
            } else if (p < 2 * w + h) {
                // Bottom border
                int px = p - w - h;
                graphics.fill(x + w - px - 1, y + h - 2, x + w - px, y + h, col);
            } else {
                // Left border
                int py = p - 2 * w - h;
                graphics.fill(x, y + h - py - 1, x + 2, y + h - py, col);
            }
        }
    }

    // Helper: draw flat button with vertical gradient & hover states
    public boolean drawFlatButton(GuiGraphics graphics, int x, int y, int w, int h, String text, int mouseX, int mouseY, boolean enabled, boolean isOp) {
        boolean hovered = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bg = hovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int border = isOp ? ACCENT_OP_BORDER : (hovered ? COLOR_BRASS : 0xFF2C221D);
        if (!enabled) {
            bg = 0xFF222222;
            border = 0xFF444444;
        }

        // Draw gradient vertical fill
        int bgTop = adjustColorBrightness(bg, 15);
        int bgBottom = adjustColorBrightness(bg, -25);
        drawGradientPanel(graphics, x, y, w, h, bgTop, bgBottom, border);

        int textColor = enabled ? (hovered ? 0xFFFFF3D1 : TEXT_PRIMARY) : TEXT_MUTED;
        float alphaFactor = getFadeProgress();
        int finalTextColor = adjustAlpha(textColor, alphaFactor);

        int textX = x + (w - this.font.width(text)) / 2;
        int textY = y + (h - 8) / 2;
        graphics.drawString(this.font, text, textX, textY, finalTextColor, false);
        return hovered;
    }

    public boolean drawFlatButton(GuiGraphics graphics, int x, int y, int w, int h, String text, int mouseX, int mouseY, boolean enabled) {
        return drawFlatButton(graphics, x, y, w, h, text, mouseX, mouseY, enabled, false);
    }

    protected void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.LEVER_CLICK, 1.2F, 0.9F
                )
        );
    }

    public void playGearSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN, 1.3F, 0.8F
                )
        );
    }

    public void playSteamSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, 1.2F, 0.5F
                )
        );
    }

    public void playCompleteSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.BELL_BLOCK, 1.0F, 1.1F
                )
        );
    }

    protected boolean drawBackButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnSize = 20;
        int btnX = containerX + containerW - 15 - btnSize;
        int btnY = containerY + (headerH - btnSize) / 2;
        return drawFlatButton(graphics, btnX, btnY, btnSize, btnSize, "◀", mouseX, mouseY, true);
    }

    protected boolean isBackButtonClicked(double mouseX, double mouseY) {
        int btnSize = 20;
        int btnX = containerX + containerW - 15 - btnSize;
        int btnY = containerY + (headerH - btnSize) / 2;
        return mouseX >= btnX && mouseX < btnX + btnSize && mouseY >= btnY && mouseY < btnY + btnSize;
    }

    protected boolean drawCloseButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnSize = 20;
        int btnX = containerX + containerW - 15 - btnSize;
        int btnY = containerY + (headerH - btnSize) / 2;
        return drawFlatButton(graphics, btnX, btnY, btnSize, btnSize, "✕", mouseX, mouseY, true);
    }

    protected boolean isCloseButtonClicked(double mouseX, double mouseY) {
        int btnSize = 20;
        int btnX = containerX + containerW - 15 - btnSize;
        int btnY = containerY + (headerH - btnSize) / 2;
        return mouseX >= btnX && mouseX < btnX + btnSize && mouseY >= btnY && mouseY < btnY + btnSize;
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

        float alphaFactor = getFadeProgress();
        int panelBg = adjustAlpha(currentPanelBg, alphaFactor);
        int borderStd = adjustAlpha(currentBorderStd, alphaFactor);
        int widgetBg = adjustAlpha(currentWidgetBg, alphaFactor);

        // Soft drop shadow around the main container
        int shadowColor = adjustAlpha(0x0C000000, alphaFactor);
        for (int i = 1; i <= 5; i++) {
            graphics.fill(containerX - i, containerY - i, containerX + containerW + i, containerY + containerH + i, shadowColor);
        }

        // Main Panel Container
        drawFlatPanel(graphics, containerX, containerY, containerW, containerH, panelBg, borderStd);

        // Header Background & Border
        graphics.fill(containerX + 2, containerY + 2, containerX + containerW - 2, containerY + headerH, widgetBg);
        graphics.fill(containerX + 2, containerY + headerH - 2, containerX + containerW - 2, containerY + headerH, borderStd);

        // Footer Background & Border
        graphics.fill(containerX + 2, containerY + containerH - footerH, containerX + containerW - 2, containerY + containerH - 2, widgetBg);
        graphics.fill(containerX + 2, containerY + containerH - footerH, containerX + containerW - 2, containerY + containerH - footerH + 2, borderStd);

        renderHeader(graphics, mouseX, mouseY);
        renderFooter(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
