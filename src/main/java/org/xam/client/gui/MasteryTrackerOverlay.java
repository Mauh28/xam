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
import org.xam.data.PlayerDataProvider;
import org.xam.network.TrackRequirementPacket;
import org.xam.network.XamNetwork;
import org.xam.progression.MasteryService;
import org.xam.progression.RequirementFormatter;
import org.xam.util.PathIcons;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = XamConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MasteryTrackerOverlay {

    private static String lastCompletedKey = "";
    private static long completionStartTime = 0;
    private static Requirement completedReqRef = null;
    private static final Map<String, Boolean> prevCompletionMap = new HashMap<>();

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

            long now = System.currentTimeMillis();

            // Detect requirement completion state changes
            for (Requirement r : path.getRequirements()) {
                String rKey = MasteryService.getRequirementShortKey(r);
                boolean isComp = MasteryService.isRequirementCompleted(mc.player, data, path.getId(), r);
                Boolean wasComp = prevCompletionMap.get(rKey);

                if (wasComp != null && !wasComp && isComp) {
                    // Requirement transitioned from incomplete -> complete!
                    lastCompletedKey = rKey;
                    completedReqRef = r;
                    completionStartTime = now;
                    mc.player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);

                    // If tracked requirement key matches, auto-advance tracked key to next uncompleted requirement
                    String trackedKey = data.getTrackedRequirementKey();
                    if (trackedKey != null && trackedKey.equals(rKey)) {
                        Requirement nextReq = null;
                        for (Requirement nextR : path.getRequirements()) {
                            if (!MasteryService.isRequirementCompleted(mc.player, data, path.getId(), nextR)) {
                                nextReq = nextR;
                                break;
                            }
                        }
                        String nextKey = nextReq != null ? MasteryService.getRequirementShortKey(nextReq) : "";
                        data.setTrackedRequirementKey(nextKey);
                        XamNetwork.CHANNEL.sendToServer(new TrackRequirementPacket(nextKey));
                    }
                }
                prevCompletionMap.put(rKey, isComp);
            }

            boolean showingCompletion = completionStartTime > 0 && (now - completionStartTime < 3000) && completedReqRef != null;

            Requirement reqToDisplay = null;

            if (showingCompletion) {
                reqToDisplay = completedReqRef;
            } else {
                String trackedKey = data.getTrackedRequirementKey();
                if (trackedKey != null && !trackedKey.isEmpty()) {
                    for (Requirement r : path.getRequirements()) {
                        if (MasteryService.getRequirementShortKey(r).equals(trackedKey)) {
                            reqToDisplay = r;
                            break;
                        }
                    }
                }
                if (reqToDisplay == null) {
                    for (Requirement r : path.getRequirements()) {
                        if (!MasteryService.isRequirementCompleted(mc.player, data, path.getId(), r)) {
                            reqToDisplay = r;
                            break;
                        }
                    }
                }
            }

            if (reqToDisplay == null) return;

            GuiGraphics g = event.getGuiGraphics();
            Font font = mc.font;

            int completedCount = MasteryService.getCompletedRequirementsCount(mc.player, data, path);
            int totalCount = path.getRequirements().size();

            String titleText = "⚡ " + (path.getName().startsWith("xam.") ? net.minecraft.network.chat.Component.translatable(path.getName()).getString() : path.getName()) + " (" + completedCount + "/" + totalCount + ")";
            String descText = showingCompletion ? net.minecraft.network.chat.Component.translatable("xam.overlay.mission_completed").getString() : (reqToDisplay.getName() != null && !reqToDisplay.getName().isEmpty() ? (reqToDisplay.getName().startsWith("xam.") ? net.minecraft.network.chat.Component.translatable(reqToDisplay.getName()).getString() : reqToDisplay.getName()) : RequirementFormatter.formatRequirementDescription(reqToDisplay));

            int titleW = font.width(titleText);
            int descW = font.width(descText);
            int cardW = Math.max(150, Math.max(titleW, descW) + 36);
            int cardH = 32;

            // Position at (4, 4)
            int cardX = 4;
            int cardY = 4;

            int bg = 0xD8120E0D;
            int border = showingCompletion ? 0xFF55FF55 : 0xFFDF9E3F;

            g.pose().pushPose();
            AbstractMasteryScreen.drawFlatPanel(g, cardX, cardY, cardW, cardH, bg, border);

            ItemStack iconStack = PathIcons.getIcon(path);
            if (!iconStack.isEmpty()) {
                g.renderFakeItem(iconStack, cardX + 8, cardY + 8);
            }

            int textX = cardX + 30;
            g.drawString(font, titleText, textX, cardY + 6, showingCompletion ? 0xFF55FF55 : 0xFFDF9E3F, false);
            g.drawString(font, descText, textX, cardY + 18, showingCompletion ? 0xFF55FF55 : 0xFFFFFFFF, false);

            g.pose().popPose();
        });
    }
}