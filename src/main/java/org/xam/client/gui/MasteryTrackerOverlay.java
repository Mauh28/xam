package org.xam.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xam.XamConstants;
import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.data.PlayerData;
import org.xam.data.PlayerDataProvider;
import org.xam.progression.MasteryService;
import org.xam.progression.RequirementFormatter;
import org.xam.util.PathIcons;

@Mod.EventBusSubscriber(modid = XamConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MasteryTrackerOverlay {

    private static String lastTrackedReqKey = "";
    private static boolean lastCompleted = false;
    private static long completionTime = 0;

    private static java.lang.reflect.Field toastVisibleField = null;

    static {
        try {
            toastVisibleField = net.minecraft.client.gui.components.toasts.ToastComponent.class.getDeclaredField("f_94917_");
            toastVisibleField.setAccessible(true);
        } catch (Exception e1) {
            try {
                toastVisibleField = net.minecraft.client.gui.components.toasts.ToastComponent.class.getDeclaredField("occupied");
                toastVisibleField.setAccessible(true);
            } catch (Exception e2) {
                try {
                    for (java.lang.reflect.Field f : net.minecraft.client.gui.components.toasts.ToastComponent.class.getDeclaredFields()) {
                        if (java.util.List.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            toastVisibleField = f;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private static boolean isToastActive(Minecraft mc) {
        if (mc.getToasts() == null || toastVisibleField == null) return false;
        try {
            Object val = toastVisibleField.get(mc.getToasts());
            if (val instanceof java.util.List<?> list) {
                return !list.isEmpty();
            }
        } catch (Exception ignored) {}
        return false;
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        // Hide overlay if a Toast notification is currently active
        if (isToastActive(mc)) return;

        mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            String currentPath = data.getCurrentPath();
            if (currentPath == null) return;

            PathInfo path = ConfigManager.PATHS_MAP.get(currentPath);
            if (path == null || path.getRequirements().isEmpty()) return;

            if (data.isCompletedAllMasteries()) return;

            String trackedKey = data.getTrackedRequirementKey();
            Requirement trackedReq = null;

            if (trackedKey != null && !trackedKey.isEmpty()) {
                for (Requirement r : path.getRequirements()) {
                    if (MasteryService.getRequirementShortKey(r).equals(trackedKey)) {
                        trackedReq = r;
                        break;
                    }
                }
            }

            if (trackedReq == null) {
                for (Requirement r : path.getRequirements()) {
                    if (!MasteryService.isRequirementCompleted(mc.player, data, path.getId(), r)) {
                        trackedReq = r;
                        break;
                    }
                }
            }

            if (trackedReq == null) return;

            String currentKey = MasteryService.getRequirementShortKey(trackedReq);
            boolean isCompleted = MasteryService.isRequirementCompleted(mc.player, data, path.getId(), trackedReq);
            long now = System.currentTimeMillis();

            if (!currentKey.equals(lastTrackedReqKey)) {
                lastTrackedReqKey = currentKey;
                lastCompleted = isCompleted;
                completionTime = isCompleted ? now : 0;
            } else if (isCompleted && !lastCompleted) {
                lastCompleted = true;
                completionTime = now;
                mc.player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
            }

            if (isCompleted && completionTime > 0 && (now - completionTime > 3000)) {
                return;
            }

            GuiGraphics g = event.getGuiGraphics();
            Font font = mc.font;

            int completedCount = MasteryService.getCompletedRequirementsCount(mc.player, data, path);
            int totalCount = path.getRequirements().size();

            String titleText = "⚡ " + path.getName() + " (" + completedCount + "/" + totalCount + ")";
            String descText = isCompleted ? "✔ ¡Misión Completada!" : (trackedReq.getName() != null && !trackedReq.getName().isEmpty() ? trackedReq.getName() : RequirementFormatter.formatRequirementDescription(trackedReq));

            int titleW = font.width(titleText);
            int descW = font.width(descText);
            int cardW = Math.max(150, Math.max(titleW, descW) + 36);
            int cardH = 32;

            // Position: Top-Left corner next to toasts
            int cardX = 12;
            int cardY = 12;

            int bg = 0xD8120E0D;
            int border = isCompleted ? 0xFF55FF55 : 0xFFDF9E3F;

            g.pose().pushPose();
            AbstractMasteryScreen.drawFlatPanel(g, cardX, cardY, cardW, cardH, bg, border);

            ItemStack iconStack = PathIcons.getIcon(path);
            if (!iconStack.isEmpty()) {
                g.renderFakeItem(iconStack, cardX + 8, cardY + 8);
            }

            int textX = cardX + 30;
            g.drawString(font, titleText, textX, cardY + 6, isCompleted ? 0xFF55FF55 : 0xFFDF9E3F, false);
            g.drawString(font, descText, textX, cardY + 18, isCompleted ? 0xFF55FF55 : 0xFFFFFFFF, false);

            g.pose().popPose();
        });
    }
}