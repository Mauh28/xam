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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class MasteryCompletionToast implements Toast {
    private final Component title;
    private final Component subtitle;
    private final ItemStack icon;
    private final int width;

    public MasteryCompletionToast(Component title, Component subtitle, ItemStack icon) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;

        Font font = Minecraft.getInstance().font;
        int titleW = font.width(title);
        int subtitleW = font.width(subtitle);
        int maxTextW = Math.max(titleW, subtitleW);
        this.width = Math.max(160, 30 + maxTextW + 8);
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long startTime) {
        float f = 1.0F;
        if (startTime < 600L) {
            f = (float)startTime / 600.0F;
        } else if (startTime > 4400L) {
            f = (float)(5000L - startTime) / 600.0F;
            f = Math.max(0.0F, Math.min(1.0F, f));
        }

        float desiredX = -this.width + (this.width + 12.0F) * f;
        int screenWidth = toastComponent.getMinecraft().getWindow().getGuiScaledWidth();
        float currentX = (float)screenWidth - (float)this.width * f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(desiredX - currentX, 12.0F, 0);

        // Draw our premium Ponder-themed notification card (dynamic width x 32)
        // Background: PANEL_INNER_BG (0xFF120E0D), Border: COLOR_BRASS (0xFFDF9E3F)
        AbstractMasteryScreen.drawFlatPanel(guiGraphics, 0, 0, this.width, 32, 0xFF120E0D, 0xFFDF9E3F);

        // Draw item icon or mod logo
        if (!icon.isEmpty()) {
            guiGraphics.renderFakeItem(icon, 8, 8);
        } else {
            net.minecraft.resources.ResourceLocation logoRl = net.minecraft.resources.ResourceLocation.tryParse("xam:textures/logo.png");
            guiGraphics.blit(logoRl, 8, 8, 18, 16, 0.0F, 0.0F, 972, 868, 972, 868);
        }

        // Draw texts
        Font font = toastComponent.getMinecraft().font;
        guiGraphics.drawString(font, title, 30, 7, 0xFFDF9E3F, false);
        guiGraphics.drawString(font, subtitle, 30, 18, 0xFFFFFFFF, false);

        guiGraphics.pose().popPose();

        return startTime >= 5000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}
