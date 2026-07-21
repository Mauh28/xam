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
import org.xam.util.PathIcons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class MasteryHubScreen extends AbstractMasteryScreen {
    private final PlayerData playerData;
    private double scrollY = 0;
    private double targetScrollY = 0;
    private String flippedPathId = null;
    private long lastFlipTime = 0;
    private String flippingBackPathId = null;
    private long lastFlipBackTime = 0;
    private int masteredPathsPage = 0;
    private boolean showHelpModal = false;

    public MasteryHubScreen(PlayerData playerData) {
        super(Component.translatable("xam.screen.mastery_hub.title"));
        this.playerData = playerData;
    }

    private boolean hasPlayerMasteredAllBranches() {
        if (playerData == null || ConfigManager.PATHS.isEmpty()) return false;
        for (PathInfo p : ConfigManager.PATHS) {
            if (!p.getRequirements().isEmpty() && !playerData.getMasteredPaths().contains(p.getId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        String titleText = Component.translatable("xam.screen.mastery_hub.header").getString();
        graphics.drawString(this.font, titleText, containerX + 15, titleY, TEXT_SECONDARY, false);

        int btnSize = 20;
        int closeX = containerX + containerW - 15 - btnSize;
        int helpX = closeX - btnSize - 6;
        int btnY = containerY + (headerH - btnSize) / 2;

        // Draw Help button "?" next to Close button
        drawFlatButton(graphics, helpX, btnY, btnSize, btnSize, "?", mouseX, mouseY, true);

        // Draw Close button "✕"
        drawCloseButton(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean isOp = this.minecraft.player != null && (this.minecraft.player.hasPermissions(2) || this.minecraft.player.getAbilities().instabuild);
        int btnW = 120;
        int btnH = 20;
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        String mainBtnText = Component.translatable("xam.screen.mastery_hub.choose_branch").getString();

        if (isOp) {
            int totalW = btnW + 20 + btnW;
            int startX = containerX + (containerW - totalW) / 2;
            drawFlatButton(graphics, startX, btnY, btnW, btnH, mainBtnText, mouseX, mouseY, true, false);
            drawFlatButton(graphics, startX + btnW + 20, btnY, btnW, btnH, Component.translatable("xam.screen.mastery_hub.op_editor").getString(), mouseX, mouseY, true, true);
        } else {
            int startX = containerX + (containerW - btnW) / 2;
            drawFlatButton(graphics, startX, btnY, btnW, btnH, mainBtnText, mouseX, mouseY, true, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int effMouseX = showHelpModal ? -999 : mouseX;
        int effMouseY = showHelpModal ? -999 : mouseY;
        Requirement hoveredRequirement = null;
        // Draw container base and headers/footers
        super.render(graphics, effMouseX, effMouseY, partialTick);

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
        graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.progress").getString(), leftX + 12, leftY + 10, COLOR_BRASS, false);

        String activePathId = playerData != null ? playerData.getCurrentPath() : null;
        PathInfo activePath = activePathId != null ? ConfigManager.PATHS_MAP.get(activePathId) : null;

        if (activePath != null) {
            // Icon slot background
            int iconX = leftX + 12;
            int iconY = leftY + 24;
            int iconW = 20;
            drawFlatPanel(graphics, iconX, iconY, iconW, iconW, INPUT_BACKGROUND, COLOR_COPPER);

            // Icon stack
            net.minecraft.world.item.ItemStack branchIconStack = PathIcons.getIcon(activePath);
            graphics.renderFakeItem(branchIconStack, iconX + 2, iconY + 2);

            // Active Path Name and Mod Namespace
            String pathName = activePath.getName();
            int maxNameW = leftW - 45;
            if (this.font.width(pathName) > maxNameW) {
                pathName = this.font.plainSubstrByWidth(pathName, maxNameW - 10) + "...";
            }
            graphics.drawString(this.font, pathName, leftX + 38, leftY + 25, TEXT_PRIMARY, false);
            graphics.drawString(this.font, activePath.getModId(), leftX + 38, leftY + 35, TEXT_MUTED, false);

            // Calculate progress
            int totalReqs = activePath.getRequirements().size();
            int completedReqs = 0;
            if (this.minecraft.player != null) {
                completedReqs = MasteryService.getCompletedRequirementsCount(this.minecraft.player, playerData, activePath);
            }
            double pct = totalReqs > 0 ? (double) completedReqs / totalReqs : 0.0;
            int pctInt = (int) (pct * 100);

            // Progress text
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.completed_format", pctInt).getString(), leftX + 12, leftY + 54, COLOR_BRASS, false);
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.tasks_format", completedReqs, totalReqs).getString(), leftX + 12, leftY + 66, TEXT_SECONDARY, false);

            // Progress bar box
            int barX = leftX + 12;
            int barY = leftY + 80;
            int barW = leftW - 24;
            int barH = 10;
            drawFlatPanel(graphics, barX, barY, barW, barH, 0xFF140F0D, 0xFF2C221D);
            if (completedReqs > 0) {
                int fillW = (int) (barW * pct);
                if (fillW > 4) {
                    graphics.fillGradient(barX + 2, barY + 2, barX + fillW - 2, barY + barH - 2, COLOR_COPPER, adjustColorBrightness(COLOR_COPPER, -20));
                    graphics.fill(barX + 2, barY + 2, barX + fillW - 2, barY + 3, adjustColorBrightness(COLOR_COPPER, 40));
                }
            }
        } else {
            if (playerData != null && playerData.isCompletedAllMasteries()) {
                graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.all_masteries_completed").getString(), leftX + 12, leftY + 30, COLOR_BRASS, false);
                graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.all_masteries_completed_tip").getString(), leftX + 12, leftY + 45, TEXT_SECONDARY, false);
            } else {
                graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.no_active_branch").getString(), leftX + 12, leftY + 30, TEXT_MUTED, false);
                graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.select_branch_tip").getString(), leftX + 12, leftY + 45, TEXT_SECONDARY, false);
            }
        }

        // Ramas Empezadas list - Grid layout
        int empY = leftY + 98;
        graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.in_progress").getString(), leftX + 12, empY, COLOR_BRASS, false);
        graphics.fill(leftX + 12, empY + 11, leftX + leftW - 12, empY + 12, 0xFF2C221D);

        int empGridStartY = empY + 16;
        int medalSize = 22;
        int medalGap = 6;
        int medalsPerRow = Math.max(1, (leftW - 24) / (medalSize + medalGap));

        int empCount = 0;
        String hoveredEmpPathId = null;
        if (playerData != null && playerData.getStartedPaths() != null) {
            for (String id : playerData.getStartedPaths()) {
                if (id.equals(activePathId) || playerData.getMasteredPaths().contains(id)) {
                    continue;
                }
                net.minecraft.world.item.ItemStack iconStack = getPathIcon(id);
                int row = empCount / medalsPerRow;
                int col = empCount % medalsPerRow;
                int mx = leftX + 12 + col * (medalSize + medalGap);
                int my = empGridStartY + row * (medalSize + medalGap);

                boolean hovered = mouseX >= mx && mouseX < mx + medalSize && mouseY >= my && mouseY < my + medalSize;
                if (hovered) {
                    hoveredEmpPathId = id;
                }

                int border = hovered ? COLOR_BRASS : 0xFF2C221D;
                drawFlatPanel(graphics, mx, my, medalSize, medalSize, PANEL_INNER_BG, border);

                graphics.pose().pushPose();
                graphics.pose().translate(mx + (medalSize - 16) / 2.0F, my + (medalSize - 16) / 2.0F, 0);
                graphics.renderFakeItem(iconStack, 0, 0);
                graphics.pose().popPose();

                empCount++;
            }
        }
        if (empCount == 0) {
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.none").getString(), leftX + 12, empGridStartY, TEXT_MUTED, false);
        }

        // Ramas Dominadas list - 2D Medals Grid
        int empRows = empCount > 0 ? (empCount + medalsPerRow - 1) / medalsPerRow : 1;
        int empHeight = empRows * (medalSize + medalGap);
        int domY = empGridStartY + empHeight + 10;

        int domGridStartY = domY + 16;
        int totalMastered = playerData != null ? playerData.getMasteredPaths().size() : 0;
        int maxDomRows = (leftY + leftH - domGridStartY) / (medalSize + medalGap);
        if (maxDomRows < 1) maxDomRows = 1;
        int medalsPerPage = medalsPerRow * maxDomRows;
        int totalPages = (totalMastered + medalsPerPage - 1) / medalsPerPage;
        if (totalPages < 1) totalPages = 1;

        if (masteredPathsPage >= totalPages) {
            masteredPathsPage = totalPages - 1;
        }
        if (masteredPathsPage < 0) {
            masteredPathsPage = 0;
        }

        // Draw horizontal line (dynamically sized based on whether we have pagination buttons)
        int lineEndX = totalPages > 1 ? leftX + leftW - 38 : leftX + leftW - 12;
        graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.mastered_branches").getString(), leftX + 12, domY, COLOR_BRASS, false);
        graphics.fill(leftX + 12, domY + 11, lineEndX, domY + 12, 0xFF2C221D);

        // Draw page selector `<` and `>` buttons
        if (totalPages > 1) {
            int btnW = 12;
            int btnH = 10;
            int btnY = domY - 1;
            int prevBtnX = leftX + leftW - 30;
            int nextBtnX = leftX + leftW - 16;

            boolean prevHovered = mouseX >= prevBtnX && mouseX < prevBtnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
            boolean nextHovered = mouseX >= nextBtnX && mouseX < nextBtnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;

            int prevColor = prevHovered ? COLOR_BRASS : TEXT_SECONDARY;
            graphics.drawString(this.font, "<", prevBtnX, btnY, prevColor, false);

            int nextColor = nextHovered ? COLOR_BRASS : TEXT_SECONDARY;
            graphics.drawString(this.font, ">", nextBtnX, btnY, nextColor, false);
        }

        int domCount = 0;
        if (playerData != null && !playerData.getMasteredPaths().isEmpty()) {
            int startIndex = masteredPathsPage * medalsPerPage;
            int endIndex = Math.min(startIndex + medalsPerPage, totalMastered);

            for (int i = startIndex; i < endIndex; i++) {
                String id = playerData.getMasteredPaths().get(i);
                net.minecraft.world.item.ItemStack iconStack = getPathIcon(id);
                
                int row = domCount / medalsPerRow;
                int col = domCount % medalsPerRow;
                int mx = leftX + 12 + col * (medalSize + medalGap);
                int my = domGridStartY + row * (medalSize + medalGap);

                // Animation calculations
                boolean isFlipped = id.equals(flippedPathId);
                boolean isFlippingBack = id.equals(flippingBackPathId);
                float scaleX = 1.0F;
                boolean flipStateBack = isFlipped;

                if (isFlipped && lastFlipTime > 0) {
                    float elapsed = (System.currentTimeMillis() - lastFlipTime) / 300.0F;
                    if (elapsed < 1.0F) {
                        scaleX = Math.abs(1.0F - 2.0F * elapsed);
                        flipStateBack = (elapsed >= 0.5F);
                    }
                } else if (isFlippingBack && lastFlipBackTime > 0) {
                    float elapsed = (System.currentTimeMillis() - lastFlipBackTime) / 300.0F;
                    if (elapsed < 1.0F) {
                        scaleX = Math.abs(1.0F - 2.0F * elapsed);
                        flipStateBack = (elapsed < 0.5F);
                    } else {
                        flippingBackPathId = null;
                    }
                }
                
                graphics.pose().pushPose();
                graphics.pose().translate(mx + medalSize / 2.0F, my + medalSize / 2.0F, 0);
                graphics.pose().scale(scaleX, 1.0F, 1.0F);
                graphics.pose().translate(-medalSize / 2.0F, -medalSize / 2.0F, 0);
                
                boolean hovered = mouseX >= mx && mouseX < mx + medalSize && mouseY >= my && mouseY < my + medalSize;
                
                if (flipStateBack) {
                    // Back side: Golden info seal
                    drawFlatPanel(graphics, 0, 0, medalSize, medalSize, 0xFFDF9E3F, 0xFFCD613C);
                    graphics.drawCenteredString(this.font, "i", medalSize / 2, (medalSize - 8) / 2, 0xFFFFFFFF);
                } else {
                    // Front side: framed item icon
                    int border = hovered ? COLOR_BRASS : 0xFF2C221D;
                    drawFlatPanel(graphics, 0, 0, medalSize, medalSize, PANEL_INNER_BG, border);
                    
                    graphics.pose().pushPose();
                    graphics.pose().translate((medalSize - 16) / 2.0F, (medalSize - 16) / 2.0F, 0);
                    graphics.renderFakeItem(iconStack, 0, 0);
                    graphics.pose().popPose();
                }
                
                graphics.pose().popPose();
                domCount++;
            }
        } else {
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.none").getString(), leftX + 12, domGridStartY, TEXT_MUTED, false);
        }

        // Render hovered/flipped medal detail popup
        if (flippedPathId != null && playerData != null) {
            int fIdx = playerData.getMasteredPaths().indexOf(flippedPathId);
            int startIndex = masteredPathsPage * medalsPerPage;
            int endIndex = Math.min(startIndex + medalsPerPage, totalMastered);
            if (fIdx >= startIndex && fIdx < endIndex) {
                int pageIdx = fIdx - startIndex;
                int row = pageIdx / medalsPerRow;
                int col = pageIdx % medalsPerRow;
                int mx = leftX + 12 + col * (medalSize + medalGap);
                int my = domGridStartY + row * (medalSize + medalGap);
                
                int popX = mx + medalSize + 6;
                int popY = my - 10;
                int popW = 115;
                int popH = 50;
                
                if (popX + popW > containerX + containerW) {
                    popX = mx - popW - 6;
                }
                
                drawFlatPanel(graphics, popX, popY, popW, popH, PANEL_BACKGROUND, COLOR_BRASS);
                
                PathInfo path = ConfigManager.PATHS_MAP.get(flippedPathId);
                if (path != null) {
                    String pName = Component.translatable(path.getName()).getString();
                    if (this.font.width(pName) > popW - 10) {
                        pName = this.font.plainSubstrByWidth(pName, popW - 15) + "...";
                    }
                    graphics.drawString(this.font, pName, popX + 6, popY + 6, TEXT_PRIMARY, false);
                    graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.completed").getString(), popX + 6, popY + 18, 0xFF4ADE80, false);
                    
                    String perkText = Component.translatable("xam.screen.mastery_hub.perk_none").getString();
                    if (path.getPerkEffect() != null && !path.getPerkEffect().isEmpty()) {
                        String name = path.getPerkEffect();
                        if (name.contains(":")) name = name.split(":")[1];
                        perkText = Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + (path.getPerkAmplifier() + 1);
                    }
                    graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.perk_label", perkText).getString(), popX + 6, popY + 32, COLOR_BRASS, false);
                }
            }
        }

        // --- RIGHT PANEL: Tasks details list ---
        drawFlatPanel(graphics, rightX, rightY, rightW, rightH, currentWidgetBg, currentBorderStd);
        graphics.drawString(this.font, Component.translatable("xam.screen.mastery_hub.tasks_and_requirements").getString(), rightX + 15, rightY + 10, COLOR_BRASS, false);

        if (activePath == null) {
            graphics.drawCenteredString(this.font, Component.translatable("xam.screen.mastery_hub.no_active_tasks").getString(), rightX + rightW / 2, rightY + rightH / 2 - 10, TEXT_MUTED);
            graphics.drawCenteredString(this.font, Component.translatable("xam.screen.mastery_hub.start_tip").getString(), rightX + rightW / 2, rightY + rightH / 2 + 5, TEXT_SECONDARY);
        } else {
            int listX = rightX + 15;
            int listY = rightY + 26;
            int listW = rightW - 30;
            int listH = rightH - 35;

            int cardH = 38;
            int gap = 6;
            int totalReqsH = activePath.getRequirements().size() * (cardH + gap);

            // Smooth Lerp scroll
            scrollY = scrollY + (targetScrollY - scrollY) * 0.15;
            if (Math.abs(targetScrollY - scrollY) < 0.5) {
                scrollY = targetScrollY;
            }

            int maxScroll = Math.max(0, totalReqsH - listH);
            if (targetScrollY > maxScroll) {
                targetScrollY = maxScroll;
            }
            if (scrollY > maxScroll) {
                scrollY = maxScroll;
            }

            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            int scissorX = (int) (listX * guiScale);
            int scissorY = (int) ((Minecraft.getInstance().getWindow().getGuiScaledHeight() - (listY + listH)) * guiScale);
            int scissorW = (int) (listW * guiScale);
            int scissorH = (int) (listH * guiScale);

            com.mojang.blaze3d.systems.RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

            for (int i = 0; i < activePath.getRequirements().size(); i++) {
                Requirement req = activePath.getRequirements().get(i);
                int cardY = listY + i * (cardH + gap) - (int) scrollY;

                // Optimization: Skip rendering cards that are completely out of view bounds
                if (cardY + cardH < listY || cardY > listY + listH) {
                    continue;
                }

                // Check card hover
                boolean cardHovered = effMouseX >= listX && effMouseX < listX + listW && effMouseY >= cardY && effMouseY < cardY + cardH && effMouseY >= listY && effMouseY < listY + listH;
                if (cardHovered) {
                    hoveredRequirement = req;
                }

                // Draw task card background based on completed status & hover
                boolean isCompleted = MasteryService.isRequirementCompleted(this.minecraft.player, playerData, activePath.getId(), req);
                int cardBg;
                int cardBorder;
                if (isCompleted) {
                    cardBg = cardHovered ? 0xFF1C3124 : 0xFF17251C;
                    cardBorder = cardHovered ? 0xFF3D7D57 : 0xFF2A593E;
                } else {
                    cardBg = cardHovered ? 0xFF181312 : PANEL_INNER_BG;
                    cardBorder = cardHovered ? COLOR_BRASS : WARM_BORDER;
                }

                drawFlatPanel(graphics, listX, cardY, listW, cardH, cardBg, cardBorder);

                // Draw checkmark or cross indicator
                if (isCompleted) {
                    graphics.drawString(this.font, "✔", listX + 10, cardY + (cardH - 8) / 2, 0xFF4ADE80, false);
                } else {
                    graphics.drawString(this.font, "✘", listX + 10, cardY + (cardH - 8) / 2, 0xFFF87171, false);
                }

                // Task Name
                String reqNameText = req.getName().isEmpty() ? req.getId() : req.getName();
                int reqNameMaxW = listW - 120;
                if (this.font.width(reqNameText) > reqNameMaxW) {
                    reqNameText = this.font.plainSubstrByWidth(reqNameText, reqNameMaxW - 10) + "...";
                }
                graphics.drawString(this.font, reqNameText, listX + 25, cardY + 6, isCompleted ? 0xFF8AA893 : TEXT_PRIMARY, false);

                // Task Description
                String reqDescText = req.getDescription();
                if (reqDescText.isEmpty()) reqDescText = req.getId();
                if (this.font.width(reqDescText) > reqNameMaxW) {
                    reqDescText = this.font.plainSubstrByWidth(reqDescText, reqNameMaxW - 10) + "...";
                }
                graphics.drawString(this.font, reqDescText, listX + 25, cardY + 20, isCompleted ? 0xFF657E6D : TEXT_SECONDARY, false);

                // Task Type badge on the right
                String badge = Component.translatable("xam.req_type.badge." + req.getType().toLowerCase()).getString();
                int badgeW = this.font.width(badge) + 10;
                int badgeX = listX + listW - badgeW - 10;
                int badgeY = cardY + (cardH - 12) / 2;
                drawFlatPanel(graphics, badgeX, badgeY, badgeW, 12, 0xFF140F0D, 0xFF2C221D);
                graphics.drawCenteredString(this.font, badge, badgeX + badgeW / 2, badgeY + 2, COLOR_BRASS);

                // Track / Pin Badge Button
                String reqKey = MasteryService.getRequirementShortKey(req);
                boolean isTracked = reqKey.equals(playerData.getTrackedRequirementKey());
                String trackText = isTracked 
                    ? Component.translatable("xam.screen.mastery_hub.tracking").getString()
                    : Component.translatable("xam.screen.mastery_hub.track").getString();
                int trackBtnW = this.font.width(trackText) + 8;
                int trackBtnX = badgeX - trackBtnW - 6;
                int trackBtnY = badgeY;

                boolean trackHovered = mouseX >= trackBtnX && mouseX < trackBtnX + trackBtnW && mouseY >= trackBtnY && mouseY < trackBtnY + 12;

                if (isTracked) {
                    drawFlatPanel(graphics, trackBtnX, trackBtnY, trackBtnW, 12, 0xFF4A3816, COLOR_BRASS);
                    graphics.drawCenteredString(this.font, trackText, trackBtnX + trackBtnW / 2, trackBtnY + 2, 0xFFFFD700);
                } else {
                    drawFlatPanel(graphics, trackBtnX, trackBtnY, trackBtnW, 12, trackHovered ? 0xFF2C221D : 0xFF140F0D, trackHovered ? COLOR_BRASS : WARM_BORDER);
                    graphics.drawCenteredString(this.font, trackText, trackBtnX + trackBtnW / 2, trackBtnY + 2, trackHovered ? COLOR_BRASS : TEXT_SECONDARY);
                }

                // Draw availability badge
                if (req.getType().equals("craft") || req.getType().equals("collect")) {
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(req.getId()));
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        net.minecraft.world.item.ItemStack dummyStack = new net.minecraft.world.item.ItemStack(item);
                        boolean isAvailable = MasteryService.isItemValid(dummyStack, playerData);
                        String availText = isAvailable ? Component.translatable("xam.screen.mastery_hub.available").getString() : Component.translatable("xam.screen.mastery_hub.locked").getString();
                        int availCol = isAvailable ? 0xFF55FF55 : 0xFFFF5555;
                        int availBorder = isAvailable ? 0xFF2A593E : 0xFF592A2A;
                        int availBg = isAvailable ? 0xFF152615 : 0xFF2A1515;
                        int availW = this.font.width(availText) + 8;
                        int availX = trackBtnX - availW - 6;
                        drawFlatPanel(graphics, availX, badgeY, availW, 12, availBg, availBorder);
                        graphics.drawCenteredString(this.font, availText, availX + availW / 2, badgeY + 2, availCol);
                    }
                }
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

        // Render rich tooltip if hovered
        if (hoveredRequirement != null) {
            int ttX = mouseX + 12;
            int ttY = mouseY - 12;
            int ttW = 160;
            int ttH = 68;

            if (ttX + ttW > width) {
                ttX = mouseX - ttW - 12;
            }
            if (ttY + ttH > height) {
                ttY = height - ttH - 6;
            }

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);

            drawFlatPanel(graphics, ttX, ttY, ttW, ttH, 0xFF120E0D, COLOR_COPPER);

            // 1. Draw large item icon
            net.minecraft.world.item.ItemStack dummyStack = net.minecraft.world.item.ItemStack.EMPTY;
            if (hoveredRequirement.getType().equals("craft") || hoveredRequirement.getType().equals("collect")) {
                net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(hoveredRequirement.getId()));
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    dummyStack = new net.minecraft.world.item.ItemStack(item);
                }
            }
            if (dummyStack.isEmpty()) {
                dummyStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
            }

            graphics.pose().pushPose();
            graphics.pose().translate(ttX + 8, ttY + 8, 10);
            graphics.pose().scale(1.5F, 1.5F, 1.0F);
            graphics.renderFakeItem(dummyStack, 0, 0);
            graphics.pose().popPose();

            // 2. Draw Type Badge
            String typeBadge = hoveredRequirement.getType().toLowerCase();
            String displayBadge = Component.translatable("xam.req_type.badge." + typeBadge).getString();
            int badgeCol = 0xFFDF9E3F;
            if (typeBadge.equals("craft")) badgeCol = 0xFF5DADE2;
            else if (typeBadge.equals("collect")) badgeCol = 0xFF58D68D;
            else if (typeBadge.equals("advancement")) badgeCol = 0xFFF5B041;
            else if (typeBadge.equals("kill")) badgeCol = 0xFFEC7063;

            graphics.drawString(this.font, displayBadge, ttX + 38, ttY + 8, badgeCol, false);

            // 3. Draw Dependency Status
            String depStatus = Component.translatable("xam.hub.unlocked").getString();
            int depCol = 0xFF55FF55;
            if (playerData != null && activePath != null) {
                boolean isUnlocked = true;
                if (hoveredRequirement.getDependencies() != null) {
                    for (String depId : hoveredRequirement.getDependencies()) {
                        if (!MasteryService.isRequirementCompleted(this.minecraft.player, playerData, activePath.getId(), findRequirementById(activePath, depId))) {
                            isUnlocked = false;
                            break;
                        }
                    }
                }
                if (!isUnlocked) {
                    depStatus = Component.translatable("xam.hub.locked_dependencies").getString();
                    depCol = 0xFFFF5555;
                }
            }

            String depText = depStatus;
            if (this.font.width(depText) > ttW - 44) {
                depText = this.font.plainSubstrByWidth(depText, ttW - 50) + "...";
            }
            graphics.drawString(this.font, depText, ttX + 38, ttY + 20, depCol, false);

            // 4. Draw Description
            String helpText = hoveredRequirement.getDescription().isEmpty() ? hoveredRequirement.getId() : Component.translatable(hoveredRequirement.getDescription()).getString();
            if (this.font.width(helpText) > ttW - 16) {
                helpText = this.font.plainSubstrByWidth(helpText, ttW - 22) + "...";
            }
            graphics.drawString(this.font, helpText, ttX + 8, ttY + 44, TEXT_SECONDARY, false);

            graphics.pose().popPose();
        }

        if (hoveredEmpPathId != null) {
            PathInfo path = ConfigManager.PATHS_MAP.get(hoveredEmpPathId);
            if (path != null) {
                int ttX = mouseX + 12;
                int ttY = mouseY - 12;
                String translatedName = Component.translatable(path.getName()).getString();
                int ttW = Math.max(100, this.font.width(translatedName) + 16);
                int ttH = 20;

                if (ttX + ttW > width) {
                    ttX = mouseX - ttW - 12;
                }
                if (ttY + ttH > height) {
                    ttY = height - ttH - 6;
                }

                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 400);

                drawFlatPanel(graphics, ttX, ttY, ttW, ttH, 0xFF120E0D, COLOR_COPPER);
                graphics.drawString(this.font, translatedName, ttX + 8, ttY + 6, TEXT_PRIMARY, false);

                graphics.pose().popPose();
            }
        }

        if (showHelpModal) {
            renderHelpModal(graphics, mouseX, mouseY);
        }
    }

    private void renderHelpModal(GuiGraphics graphics, int mouseX, int mouseY) {
        int modalW = Math.min(340, (int) (this.width * 0.95));
        int modalH = 195;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);

        graphics.fill(0, 0, this.width, this.height, 0x90000000);

        drawFlatPanel(graphics, modalX, modalY, modalW, modalH, PANEL_BACKGROUND, COLOR_BRASS);
        drawFlatPanel(graphics, modalX + 2, modalY + 2, modalW - 4, 22, PANEL_INNER_BG, WARM_BORDER);
        graphics.drawString(this.font, "❓ Guía de Maestrías - XAM", modalX + 10, modalY + 8, COLOR_BRASS, false);

        int closeX = modalX + modalW - 18;
        int closeY = modalY + 6;
        boolean closeHovered = mouseX >= closeX && mouseX < closeX + 12 && mouseY >= closeY && mouseY < closeY + 12;
        graphics.drawString(this.font, "✕", closeX, closeY, closeHovered ? COLOR_BRASS : TEXT_MUTED, false);

        int textY = modalY + 30;
        int textX = modalX + 12;
        int maxTextW = modalW - 24;

        String[] rawRules = {
            "§61. Selección e Inicio:§r Elige una rama activa en el Hub para especializarte en sus misiones.",
            "§62. Uso de Objetos:§r Solo puedes usar ítems de tu rama activa, ramas dominadas o ítems universales/vanilla.",
            "§63. Dominio de Ramas:§r Al completar el 100% de misiones, la rama quedará §aDominada§r permanentemente y sus ítems permitidos.",
            "§64. Fijar Misión (📍):§r Haz clic en el botón [📍 RASTREAR] de cualquier misión para seguir su progreso en tu pantalla."
        };

        for (String raw : rawRules) {
            var formattedLines = this.font.split(Component.literal(raw), maxTextW);
            for (var line : formattedLines) {
                graphics.drawString(this.font, line, textX, textY, TEXT_PRIMARY, false);
                textY += 11;
            }
            textY += 3;
        }

        graphics.pose().popPose();
    }

    private net.minecraft.world.item.ItemStack getPathIcon(String pathId) {
        PathInfo path = ConfigManager.PATHS_MAP.get(pathId);
        if (path != null) {
            return PathIcons.getIcon(path);
        }
        return PathIcons.getDefaultIcon(pathId);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showHelpModal) return true;
        if (playerData != null && playerData.getCurrentPath() != null) {
            PathInfo activePath = ConfigManager.PATHS_MAP.get(playerData.getCurrentPath());
            if (activePath != null) {
                int rightH = bodyH - 20;
                int listH = rightH - 35;
                int cardH = 38;
                int gap = 6;
                int totalReqsH = activePath.getRequirements().size() * (cardH + gap);
                if (totalReqsH > listH) {
                    targetScrollY = Math.max(0, Math.min(totalReqsH - listH, targetScrollY - delta * 18));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (showHelpModal) {
                int modalW = Math.min(340, (int) (this.width * 0.95));
                int modalH = 195;
                int modalX = (this.width - modalW) / 2;
                int modalY = (this.height - modalH) / 2;
                int closeX = modalX + modalW - 18;
                int closeY = modalY + 6;
                if (mouseX >= closeX && mouseX < closeX + 12 && mouseY >= closeY && mouseY < closeY + 12) {
                    playClickSound();
                    showHelpModal = false;
                    return true;
                }
                if (mouseX < modalX || mouseX >= modalX + modalW || mouseY < modalY || mouseY >= modalY + modalH) {
                    showHelpModal = false;
                    return true;
                }
                return true;
            }

            int btnSize = 20;
            int closeX = containerX + containerW - 15 - btnSize;
            int helpX = closeX - btnSize - 6;
            int headerBtnY = containerY + (headerH - btnSize) / 2;
            if (mouseX >= helpX && mouseX < helpX + btnSize && mouseY >= headerBtnY && mouseY < headerBtnY + btnSize) {
                playClickSound();
                showHelpModal = true;
                return true;
            }

            String prevFlippedPathId = flippedPathId;
            flippedPathId = null;
            if (prevFlippedPathId != null) {
                flippingBackPathId = prevFlippedPathId;
                lastFlipBackTime = System.currentTimeMillis();
            }

            // Header Close Button
            if (isCloseButtonClicked(mouseX, mouseY)) {
                playClickSound();
                this.onClose();
                return true;
            }

            // Check task card clicks in right panel (pinning)
            if (playerData != null && playerData.getCurrentPath() != null) {
                PathInfo activePath = ConfigManager.PATHS_MAP.get(playerData.getCurrentPath());
                if (activePath != null) {
                    int leftW = (int) (containerW * 0.35);
                    int rightX = containerX + leftW + 20;
                    int rightW = containerW - leftW - 30;
                    int rightY = bodyY + 10;
                    int rightH = bodyH - 20;
                    int listX = rightX + 10;
                    int listY = rightY + 35;
                    int listW = rightW - 24;
                    int listH = rightH - 35;
                    int cardH = 38;
                    int gap = 6;

                    if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                        for (int i = 0; i < activePath.getRequirements().size(); i++) {
                            Requirement req = activePath.getRequirements().get(i);
                            int cardY = (int) (listY + i * (cardH + gap) - targetScrollY);
                            if (mouseY >= cardY && mouseY < cardY + cardH) {
                                String reqKey = MasteryService.getRequirementShortKey(req);
                                String currentTracked = playerData.getTrackedRequirementKey();
                                String nextTracked = reqKey.equals(currentTracked) ? "" : reqKey;
                                playerData.setTrackedRequirementKey(nextTracked);
                                org.xam.network.XamNetwork.CHANNEL.sendToServer(new org.xam.network.TrackRequirementPacket(nextTracked));
                                playClickSound();
                                return true;
                            }
                        }
                    }
                }
            }

            int leftW = (int) (containerW * 0.35);
            int leftX = containerX + 10;
            int leftY = bodyY + 10;
            int leftH = bodyH - 20;

            // Recalculate domY dynamically based on in-progress count
            int empY = leftY + 98;
            int empGridStartY = empY + 16;
            int medalSize = 22;
            int medalGap = 6;
            int medalsPerRow = Math.max(1, (leftW - 24) / (medalSize + medalGap));

            int empCount = 0;
            if (playerData != null && playerData.getStartedPaths() != null) {
                String activePathId = playerData.getCurrentPath();
                for (String id : playerData.getStartedPaths()) {
                    if (id.equals(activePathId) || playerData.getMasteredPaths().contains(id)) {
                        continue;
                    }
                    empCount++;
                }
            }
            int empRows = empCount > 0 ? (empCount + medalsPerRow - 1) / medalsPerRow : 1;
            int empHeight = empRows * (medalSize + medalGap);
            int domY = empGridStartY + empHeight + 10;
            int domGridStartY = domY + 16;

            if (playerData != null && !playerData.getMasteredPaths().isEmpty()) {
                int totalMastered = playerData.getMasteredPaths().size();
                int maxDomRows = (leftY + leftH - domGridStartY) / (medalSize + medalGap);
                if (maxDomRows < 1) maxDomRows = 1;
                int medalsPerPage = medalsPerRow * maxDomRows;
                int totalPages = (totalMastered + medalsPerPage - 1) / medalsPerPage;
                if (totalPages < 1) totalPages = 1;

                if (masteredPathsPage >= totalPages) {
                    masteredPathsPage = totalPages - 1;
                }
                if (masteredPathsPage < 0) {
                    masteredPathsPage = 0;
                }

                // Check pagination buttons first
                if (totalPages > 1) {
                    int btnY = domY - 1;
                    int prevBtnX = leftX + leftW - 30;
                    int nextBtnX = leftX + leftW - 16;
                    int btnW = 12;
                    int btnH = 10;

                    // Prev "<" clicked
                    if (mouseX >= prevBtnX && mouseX < prevBtnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                        playClickSound();
                        masteredPathsPage = (masteredPathsPage - 1 + totalPages) % totalPages;
                        return true;
                    }
                    // Next ">" clicked
                    if (mouseX >= nextBtnX && mouseX < nextBtnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                        playClickSound();
                        masteredPathsPage = (masteredPathsPage + 1) % totalPages;
                        return true;
                    }
                }

                int startIndex = masteredPathsPage * medalsPerPage;
                int endIndex = Math.min(startIndex + medalsPerPage, totalMastered);
                int domCount = 0;

                for (int i = startIndex; i < endIndex; i++) {
                    int row = domCount / medalsPerRow;
                    int col = domCount % medalsPerRow;
                    int mx = leftX + 12 + col * (medalSize + medalGap);
                    int my = domGridStartY + row * (medalSize + medalGap);
                    
                    if (mouseX >= mx && mouseX < mx + medalSize && mouseY >= my && mouseY < my + medalSize) {
                        String id = playerData.getMasteredPaths().get(i);
                        if (id.equals(prevFlippedPathId)) {
                            // Clicked the same medal -> keep it null (flips back)
                            playGearSound();
                        } else {
                            flippedPathId = id;
                            lastFlipTime = System.currentTimeMillis();
                            playGearSound();
                        }
                        return true;
                    }
                    domCount++;
                }
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_R) {
            String activePathId = playerData != null ? playerData.getCurrentPath() : null;
            PathInfo activePath = activePathId != null ? ConfigManager.PATHS_MAP.get(activePathId) : null;

            if (activePath != null && this.minecraft != null) {
                double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getWidth();
                double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getHeight();

                int leftW = (int) (containerW * 0.35);
                int leftX = containerX + 10;
                int rightX = leftX + leftW + 10;
                int rightW = containerW - leftW - 30;
                int rightY = bodyY + 10;
                int rightH = bodyH - 20;

                int listX = rightX + 15;
                int listY = rightY + 26;
                int listW = rightW - 30;
                int listH = rightH - 35;

                if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                    int cardH = 38;
                    int gap = 6;
                    double clickedY = mouseY + scrollY - listY;
                    int cardIndex = (int) (clickedY / (cardH + gap));
                    double relativeY = clickedY % (cardH + gap);

                    if (cardIndex >= 0 && cardIndex < activePath.getRequirements().size() && relativeY <= cardH) {
                        Requirement req = activePath.getRequirements().get(cardIndex);
                        if (req.getType().equals("craft") || req.getType().equals("collect")) {
                            if (net.minecraftforge.fml.ModList.get().isLoaded("jei")) {
                                playClickSound();
                                try {
                                    Class.forName("org.xam.compat.JeiIntegrationHelper")
                                         .getMethod("showRecipe", String.class)
                                         .invoke(null, req.getId());
                                } catch (Exception e) {
                                    XamConstants.LOGGER.error("Failed to show recipe via JEI", e);
                                }
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private Requirement findRequirementById(PathInfo path, String reqId) {
        if (path != null && reqId != null) {
            for (Requirement r : path.getRequirements()) {
                if (r.getId().equals(reqId)) return r;
            }
        }
        return null;
    }
}
