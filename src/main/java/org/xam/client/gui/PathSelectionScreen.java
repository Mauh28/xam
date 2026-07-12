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

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;

public class PathSelectionScreen extends AbstractMasteryScreen {
    private final Screen parent;
    private final PlayerData playerData;
    private final List<PathInfo> availablePaths = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 1;
    private int cardsPerPage = 3;
    private PathInfo selectedPathForDetails = null;
    private int modalScrollOffset = 0;
    private int modalContentHeight = 0;
    private boolean isDraggingScrollbar = false;
    private long lastScrollTime = 0;

    public PathSelectionScreen(Screen parent, PlayerData playerData) {
        super(Component.translatable("xam.screen.path_selection.title"));
        this.parent = parent;
        this.playerData = playerData;
        this.lastScrollTime = System.currentTimeMillis();
    }

    public PathSelectionScreen(PlayerData playerData) {
        this(null, playerData);
    }

    @Override
    protected void init() {
        super.init();
        
        availablePaths.clear();
        availablePaths.addAll(ConfigManager.PATHS);

        // Dynamically adjust column cards based on container width
        if (containerW < 360) {
            cardsPerPage = 1;
        } else if (containerW < 500) {
            cardsPerPage = 2;
        } else {
            cardsPerPage = 3;
        }

        totalPages = (availablePaths.size() + (cardsPerPage - 1)) / cardsPerPage;
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, Component.translatable("xam.screen.path_selection.title").getString(), containerX + 15, titleY, TEXT_PRIMARY, false);
        if (canExit()) {
            drawBackButton(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        String pageIndicator = Component.translatable("xam.screen.path_selection.page_format", currentPage + 1, Math.max(1, totalPages)).getString();
        int textX = containerX + (containerW - this.font.width(pageIndicator)) / 2;
        int textY = containerY + containerH - footerH + (footerH - 8) / 2;
        graphics.drawString(this.font, pageIndicator, textX, textY, TEXT_MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Body area has PANEL_BACKGROUND from container panel, no custom solid background drawn

        // Render Pagination buttons
        // Left Button
        int prevX = containerX + 10;
        int prevY = bodyY + (bodyH - 80) / 2;
        boolean prevActive = currentPage > 0;
        boolean prevHovered = prevActive && mouseX >= prevX && mouseX < prevX + 50 && mouseY >= prevY && mouseY < prevY + 80;
        int prevBg = prevHovered ? BUTTON_HOVER_BG : WIDGET_BACKGROUND;
        int prevBorder = prevHovered ? BUTTON_HOVER_BORDER : BORDER_INNER;
        if (!prevActive) {
            prevBg = 0xFF181818;
            prevBorder = 0xFF282828;
            drawFlatPanel(graphics, prevX, prevY, 50, 80, prevBg, prevBorder);
        } else {
            int prevBgTop = adjustColorBrightness(prevBg, 12);
            int prevBgBottom = adjustColorBrightness(prevBg, -15);
            drawGradientPanel(graphics, prevX, prevY, 50, 80, prevBgTop, prevBgBottom, prevBorder);
        }
        int prevCol = prevActive ? (prevHovered ? TEXT_PRIMARY : TEXT_SECONDARY) : TEXT_MUTED;
        graphics.drawCenteredString(this.font, "◀", prevX + 25, prevY + 36, prevCol);

        // Right Button
        int nextX = containerX + containerW - 60;
        int nextY = bodyY + (bodyH - 80) / 2;
        boolean nextActive = currentPage < totalPages - 1;
        boolean nextHovered = nextActive && mouseX >= nextX && mouseX < nextX + 50 && mouseY >= nextY && mouseY < nextY + 80;
        int nextBg = nextHovered ? BUTTON_HOVER_BG : WIDGET_BACKGROUND;
        int nextBorder = nextHovered ? BUTTON_HOVER_BORDER : BORDER_INNER;
        if (!nextActive) {
            nextBg = 0xFF181818;
            nextBorder = 0xFF282828;
            drawFlatPanel(graphics, nextX, nextY, 50, 80, nextBg, nextBorder);
        } else {
            int nextBgTop = adjustColorBrightness(nextBg, 12);
            int nextBgBottom = adjustColorBrightness(nextBg, -15);
            drawGradientPanel(graphics, nextX, nextY, 50, 80, nextBgTop, nextBgBottom, nextBorder);
        }
        int nextCol = nextActive ? (nextHovered ? TEXT_PRIMARY : TEXT_SECONDARY) : TEXT_MUTED;
        graphics.drawCenteredString(this.font, "▶", nextX + 25, nextY + 36, nextCol);

        // Render cards grid layout: dynamic columns based on cardsPerPage
        int startIndex = currentPage * cardsPerPage;
        int endIndex = Math.min(startIndex + cardsPerPage, availablePaths.size());

        int viewportW = containerW - 120;
        int gap = 15;
        int cardW = (viewportW - (gap * (cardsPerPage + 1))) / cardsPerPage;
        int cardH = bodyH - 30;
        int cardY = bodyY + 15;

        for (int i = 0; i < (endIndex - startIndex); i++) {
            PathInfo path = availablePaths.get(startIndex + i);
            int cardX = containerX + 60 + gap + i * (cardW + gap);

            boolean hasTasks = !path.getRequirements().isEmpty();
            boolean isUnlocked = hasTasks && MasteryService.areDependenciesMastered(net.minecraft.client.Minecraft.getInstance().player, playerData, path);
            boolean isActivePath = playerData != null && path.getId().equals(playerData.getCurrentPath());
            boolean hasActivePath = playerData != null && playerData.getCurrentPath() != null;
            boolean canSwitch = MasteryService.canSwitchFromCurrentPath(net.minecraft.client.Minecraft.getInstance().player, playerData);
            boolean isMastered = playerData != null && playerData.getMasteredPaths().contains(path.getId());
            boolean isSelectable = isUnlocked && !isMastered && (!hasActivePath || isActivePath || canSwitch);

            boolean cardHovered = mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardH;
            
            
            int cardBg = isMastered ? 0xFF121B16 : (isSelectable ? (cardHovered ? adjustColorBrightness(currentWidgetBg, 12) : currentWidgetBg) : 0xFF181515);
            int cardBorder;
            if (isActivePath) {
                cardBorder = COLOR_BRASS;
            } else if (isMastered) {
                cardBorder = 0xFF5F9E8E;
            } else if (isSelectable) {
                cardBorder = cardHovered ? COLOR_BRASS : currentBorderStd;
            } else {
                cardBorder = cardHovered ? 0xFF884444 : 0xFF553333;
            }

            // Draw Card Panel
            if (isMastered) {
                drawScannedPanel(graphics, cardX, cardY, cardW, cardH, cardBg, cardBorder);
            } else {
                drawFlatPanel(graphics, cardX, cardY, cardW, cardH, cardBg, cardBorder);
            }

            // Card Height Breakdowns:
            int cabH = (int) (cardH * 0.20);
            int bodyReqH = (int) (cardH * 0.60);
            int pieH = cardH - cabH - bodyReqH;

            // 1. Cabeza (20%)
            int sqSize = (int) (cabH * 0.85);
            int sqX = cardX + 8;
            int sqY = cardY + (cabH - sqSize) / 2;
            drawFlatPanel(graphics, sqX, sqY, sqSize, sqSize, INPUT_BACKGROUND, 0xFF555555);

            ItemStack icon = getIconForPath(path);
            graphics.renderFakeItem(icon, sqX + (sqSize - 16) / 2, sqY + (sqSize - 16) / 2);

            String nameText = Component.translatable(path.getName()).getString();
            int nameMaxW = cardW - sqSize - 22;
            if (this.font.width(nameText) > nameMaxW) {
                nameText = this.font.plainSubstrByWidth(nameText, nameMaxW - 10) + "...";
            }
            graphics.drawString(this.font, nameText, cardX + sqSize + 14, cardY + (cabH - 18) / 2, COLOR_BRASS, false);

            // Flip indicator in top-right corner
            String flipHint = "ℹ";
            int hintX = cardX + cardW - this.font.width(flipHint) - 6;
            int hintY = cardY + 4;
            int hintColor = cardHovered ? COLOR_BRASS : TEXT_MUTED;
            graphics.drawString(this.font, flipHint, hintX, hintY, hintColor, false);

            String modText = path.getModId();
            int labelW = Math.min(this.font.width(modText) + 8, nameMaxW);
            drawFlatPanel(graphics, cardX + sqSize + 14, cardY + cabH - 14, labelW, 11, 0xFF2A201C, currentBorderStd);
            String dispMod = modText;
            if (this.font.width(modText) > labelW - 6) {
                dispMod = this.font.plainSubstrByWidth(modText, labelW - 10) + "..";
            }
            graphics.drawString(this.font, dispMod, cardX + sqSize + 18, cardY + cabH - 12, TEXT_MUTED, false);

            // Cabeza bottom line divider
            graphics.fill(cardX + 2, cardY + cabH, cardX + cardW - 2, cardY + cabH + 2, currentBorderStd);

            // 2. Cuerpo (60%)
            // 2. Cuerpo (60%)
            if (isUnlocked) {
                if (hasActivePath && !isActivePath && !canSwitch) {
                    String blockedText = Component.translatable("xam.screen.path_selection.blocked_by_progress").getString();
                    graphics.drawString(this.font, blockedText, cardX + 10, cardY + cabH + 6, 0xFFFF5555, false);
                    int labelWidth = this.font.width(blockedText);
                    graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, 0xFF772222);

                    int reqY = cardY + cabH + 25;
                    String activeId = playerData.getCurrentPath();
                    PathInfo activePath = ConfigManager.PATHS_MAP.get(activeId);
                    String activeName = activePath != null ? activePath.getName() : activeId;
                    int min = activePath != null ? activePath.getMinToSwitch() : 0;
                    
                    String line1 = Component.translatable("xam.screen.path_selection.complete_mastery").getString();
                    String line2 = Component.translatable("xam.screen.path_selection.or_reach_reqs", min).getString();
                    String line3 = Component.translatable("xam.screen.path_selection.of_path", activeName).getString();
                    
                    graphics.drawString(this.font, line1, cardX + 10, reqY, TEXT_MUTED, false);
                    graphics.drawString(this.font, line2, cardX + 10, reqY + 11, TEXT_MUTED, false);
                    graphics.drawString(this.font, line3, cardX + 10, reqY + 22, TEXT_MUTED, false);
                } else {
                    String reqText = Component.translatable("xam.screen.path_selection.requirements").getString();
                    graphics.drawString(this.font, reqText, cardX + 10, cardY + cabH + 6, COLOR_BRASS, false);
                    int labelWidth = this.font.width(reqText);
                    graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, COLOR_COPPER);

                    // Clipping/Scissor region for requirements list text
                    double scale = Minecraft.getInstance().getWindow().getGuiScale();
                    int scissorX = (int) (cardX * scale);
                    int scissorY = (int) ((this.height - (cardY + cabH + bodyReqH)) * scale);
                    int scissorW = (int) (cardW * scale);
                    int scissorH = (int) ((bodyReqH - 18) * scale);

                    RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

                    int reqY = cardY + cabH + 20;
                    int maxReqsToShow = 4;
                    int shown = 0;
                    for (Requirement req : path.getRequirements()) {
                        if (shown >= maxReqsToShow) {
                            String moreText = Component.translatable("xam.screen.path_selection.more_reqs").getString();
                            graphics.drawString(this.font, moreText, cardX + 14, reqY, TEXT_MUTED, false);
                            break;
                        }

                        // Requirement name only
                        String rName = resolveRequirementName(req);
                        if (this.font.width(rName) > cardW - 26) {
                            rName = this.font.plainSubstrByWidth(rName, cardW - 36) + "...";
                        }
                        graphics.fill(cardX + 10, reqY + 2, cardX + 14, reqY + 6, TEXT_MUTED);
                        graphics.drawString(this.font, rName, cardX + 18, reqY, TEXT_SECONDARY, false);
                        reqY += 12;

                        shown++;
                    }

                    RenderSystem.disableScissor();
                }
            } else {
                if (!hasTasks) {
                    String noTasksText = Component.translatable("xam.screen.path_selection.no_tasks").getString();
                    graphics.drawString(this.font, noTasksText, cardX + 10, cardY + cabH + 6, 0xFFFF5555, false);
                    int labelWidth = this.font.width(noTasksText);
                    graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, 0xFF772222);

                    int reqY = cardY + cabH + 25;
                    graphics.drawString(this.font, Component.translatable("xam.screen.path_selection.no_tasks_line1").getString(), cardX + 10, reqY, TEXT_MUTED, false);
                    graphics.drawString(this.font, Component.translatable("xam.screen.path_selection.no_tasks_line2").getString(), cardX + 10, reqY + 11, TEXT_MUTED, false);
                } else {
                    String requiresText = Component.translatable("xam.screen.path_selection.requires").getString();
                    graphics.drawString(this.font, requiresText, cardX + 10, cardY + cabH + 6, 0xFFFF5555, false);
                    int labelWidth = this.font.width(requiresText);
                    graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, 0xFF772222);

                    int reqY = cardY + cabH + 20;
                    List<String> missingNames = new ArrayList<>();
                    for (String dep : path.getDependencies()) {
                        if (!MasteryService.isDependencyMet(Minecraft.getInstance().player, playerData, dep)) {
                            String[] parts = dep.split(":");
                            String depId = parts[0];
                            String amt = parts.length > 1 ? parts[1] : "mastered";

                            PathInfo depPath = ConfigManager.PATHS_MAP.get(depId);
                            String name = depPath != null ? depPath.getName() : depId;
                            if (amt.equalsIgnoreCase("mastered") || amt.equalsIgnoreCase("all")) {
                                missingNames.add(Component.translatable("xam.screen.path_selection.master_path", name).getString());
                            } else {
                                missingNames.add(Component.translatable("xam.screen.path_selection.reqs_format", name, amt).getString());
                            }
                        }
                    }
                    for (String missingName : missingNames) {
                        String label = "• " + missingName;
                        if (this.font.width(label) > cardW - 20) {
                            label = this.font.plainSubstrByWidth(label, cardW - 30) + "...";
                        }
                        graphics.drawString(this.font, label, cardX + 10, reqY, TEXT_MUTED, false);
                        reqY += 10;
                    }
                }
            }

            // Cuerpo bottom line divider
            graphics.fill(cardX + 2, cardY + cabH + bodyReqH, cardX + cardW - 2, cardY + cabH + bodyReqH + 2, currentBorderStd);

            // 3. Pie (20%)
            int btnX = cardX + 8;
            int btnY = cardY + cabH + bodyReqH + 4;
            int btnW = cardW - 16;
            int btnH = pieH - 8;

            String btnText;
            boolean btnEnabled;
            if (isActivePath) {
                btnText = Component.translatable("xam.screen.path_selection.btn.equipped").getString();
                btnEnabled = false;
            } else if (isMastered) {
                btnText = Component.translatable("xam.screen.path_selection.btn.mastered").getString();
                btnEnabled = false;
            } else if (isSelectable) {
                boolean hasBeenStarted = playerData != null && playerData.getStartedPaths().contains(path.getId());
                btnText = hasBeenStarted 
                    ? Component.translatable("xam.screen.path_selection.btn.continue").getString() 
                    : Component.translatable("xam.screen.path_selection.btn.choose_path").getString();
                btnEnabled = true;
            } else {
                btnText = Component.translatable("xam.screen.path_selection.btn.locked").getString();
                btnEnabled = false;
            }

            drawFlatButton(graphics, btnX, btnY, btnW, btnH, btnText, mouseX, mouseY, btnEnabled);
        }

        renderDetailsModal(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectedPathForDetails != null) {
            if (button == 0) {
                int modalW = Math.min(320, (int) (this.width * 0.9));
                int modalH = Math.min(220, (int) (this.height * 0.85));
                int modalX = (this.width - modalW) / 2;
                int modalY = (this.height - modalH) / 2;

                // Close button check
                int closeX = modalX + modalW - 18;
                int closeY = modalY + 8;
                if (mouseX >= closeX && mouseX < closeX + 12 && mouseY >= closeY && mouseY < closeY + 12) {
                    selectedPathForDetails = null;
                    modalScrollOffset = 0;
                    isDraggingScrollbar = false;
                    playClickSound();
                    return true;
                }

                // Scrollbar click check
                int viewportY = modalY + 34;
                int viewportH = modalH - 44;
                int scrollbarX = modalX + modalW - 6;
                if (modalContentHeight > viewportH) {
                    if (mouseX >= scrollbarX - 4 && mouseX < scrollbarX + 8 && mouseY >= viewportY && mouseY < viewportY + viewportH) {
                        isDraggingScrollbar = true;
                        lastScrollTime = System.currentTimeMillis();
                        int thumbH = Math.max(12, (viewportH * viewportH) / modalContentHeight);
                        int maxScroll = modalContentHeight - viewportH;
                        double relativeY = mouseY - viewportY;
                        int newOffset = (int) ((relativeY - thumbH / 2.0) / (viewportH - thumbH) * maxScroll);
                        modalScrollOffset = Math.max(0, Math.min(maxScroll, newOffset));
                        playClickSound();
                        return true;
                    }
                }

                // Outside check
                if (mouseX < modalX || mouseX >= modalX + modalW || mouseY < modalY || mouseY >= modalY + modalH) {
                    selectedPathForDetails = null;
                    modalScrollOffset = 0;
                    isDraggingScrollbar = false;
                    playClickSound();
                    return true;
                }
            }
            return true;
        }

        if (button == 0 && canExit() && isBackButtonClicked(mouseX, mouseY)) {
            playClickSound();
            if (this.parent != null) {
                this.minecraft.setScreen(this.parent);
            } else {
                this.onClose();
            }
            return true;
        }
        if (button == 0) {
            // Check left page button
            int prevX = containerX + 10;
            int prevY = bodyY + (bodyH - 80) / 2;
            if (currentPage > 0 && mouseX >= prevX && mouseX < prevX + 50 && mouseY >= prevY && mouseY < prevY + 80) {
                playClickSound();
                currentPage--;
                selectedPathForDetails = null;
                modalScrollOffset = 0;
                return true;
            }

            // Check right page button
            int nextX = containerX + containerW - 60;
            int nextY = bodyY + (bodyH - 80) / 2;
            if (currentPage < totalPages - 1 && mouseX >= nextX && mouseX < nextX + 50 && mouseY >= nextY && mouseY < nextY + 80) {
                playClickSound();
                currentPage++;
                selectedPathForDetails = null;
                modalScrollOffset = 0;
                return true;
            }

            // Check Card choose button clicks or body modal click
            int startIndex = currentPage * cardsPerPage;
            int endIndex = Math.min(startIndex + cardsPerPage, availablePaths.size());

            int viewportW = containerW - 120;
            int gap = 15;
            int cardW = (viewportW - (gap * (cardsPerPage + 1))) / cardsPerPage;
            int cardH = bodyH - 30;
            int cardY = bodyY + 15;

            int cabH = (int) (cardH * 0.20);
            int bodyReqH = (int) (cardH * 0.60);
            int pieH = cardH - cabH - bodyReqH;

            // 1. Check card body clicks to open details modal
            for (int i = 0; i < (endIndex - startIndex); i++) {
                int cardX = containerX + 60 + gap + i * (cardW + gap);
                int clickAreaY = cardY;
                int clickAreaH = cabH + bodyReqH;

                if (mouseX >= cardX && mouseX < cardX + cardW
                        && mouseY >= clickAreaY && mouseY < clickAreaY + clickAreaH) {
                    selectedPathForDetails = availablePaths.get(startIndex + i);
                    modalScrollOffset = 0;
                    playClickSound();
                    return true;
                }
            }

            // 2. Check Card choose button clicks (standard selection button)
            for (int i = 0; i < (endIndex - startIndex); i++) {
                PathInfo path = availablePaths.get(startIndex + i);
                boolean hasTasks = !path.getRequirements().isEmpty();
                boolean isUnlocked = hasTasks && MasteryService.areDependenciesMastered(Minecraft.getInstance().player, playerData, path);
                boolean isActivePath = playerData != null && path.getId().equals(playerData.getCurrentPath());
                boolean hasActivePath = playerData != null && playerData.getCurrentPath() != null;
                boolean canSwitch = MasteryService.canSwitchFromCurrentPath(Minecraft.getInstance().player, playerData);
                boolean isMastered = playerData != null && playerData.getMasteredPaths().contains(path.getId());
                boolean isSelectable = isUnlocked && !isMastered && (!hasActivePath || isActivePath || canSwitch);

                int cardX = containerX + 60 + gap + i * (cardW + gap);
                int btnX = cardX + 8;
                int btnY = cardY + cabH + bodyReqH + 4;
                int btnW = cardW - 16;
                int btnH = pieH - 8;

                if (isSelectable && !isActivePath && mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    XamNetwork.CHANNEL.sendToServer(new SelectPathPacket(path.getId()));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ItemStack getIconForPath(PathInfo path) {
        return PathIcons.getIcon(path);
    }

    private String formatRequirement(Requirement req) {
        if (req.getName() != null && !req.getName().isEmpty()) {
            return req.getName();
        }
        String name = req.getId();
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
        return name;
    }

    private void renderDetailsModal(GuiGraphics graphics, int mouseX, int mouseY) {
        if (selectedPathForDetails == null) return;

        int modalW = Math.min(320, (int) (this.width * 0.9));
        int modalH = Math.min(220, (int) (this.height * 0.85));
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        // Dark overlay cover background
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);

        // Modal panel background
        drawFlatPanel(graphics, modalX, modalY, modalW, modalH, PANEL_BACKGROUND, COLOR_BRASS);

        // Header elements (icon, title, close button)
        int sqSize = 18;
        int sqX = modalX + 10;
        int sqY = modalY + 8;
        drawFlatPanel(graphics, sqX, sqY, sqSize, sqSize, INPUT_BACKGROUND, 0xFF555555);

        ItemStack icon = getIconForPath(selectedPathForDetails);
        graphics.renderFakeItem(icon, sqX + (sqSize - 16) / 2, sqY + (sqSize - 16) / 2);

        String titleText = Component.translatable(selectedPathForDetails.getName()).getString();
        int titleMaxW = modalW - 55;
        if (this.font.width(titleText) > titleMaxW) {
            titleText = this.font.plainSubstrByWidth(titleText, titleMaxW - 10) + "...";
        }
        graphics.drawString(this.font, titleText, sqX + sqSize + 6, modalY + 12, COLOR_BRASS, false);

        // Close button '✕' redesigned as a button
        int closeW = 12;
        int closeH = 12;
        int closeX = modalX + modalW - 18;
        int closeY = modalY + 8;
        drawFlatButton(graphics, closeX, closeY, closeW, closeH, "✕", mouseX, mouseY, true);

        // Divider
        graphics.fill(modalX + 4, modalY + 28, modalX + modalW - 4, modalY + 29, currentBorderStd);

        // Content Area (scrollable body)
        int viewportY = modalY + 34;
        int viewportH = modalH - 44;
        int contentX = modalX + 10;
        int contentW = modalW - 20;

        graphics.enableScissor(contentX, viewportY, contentX + contentW, viewportY + viewportH);

        int cursorY = viewportY - modalScrollOffset;
        int startY = cursorY;

        // 1. Perk Section
        String perkEffect = selectedPathForDetails.getPerkEffect();
        if (perkEffect != null && !perkEffect.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("xam.screen.path_selection.perk_header").getString(), contentX, cursorY, COLOR_COPPER, false);
            cursorY += 12;
            if (perkEffect.contains(":")) perkEffect = perkEffect.split(":")[1];
            perkEffect = Character.toUpperCase(perkEffect.charAt(0)) + perkEffect.substring(1);
            String pText = Component.translatable("xam.screen.path_selection.perk_format", perkEffect, selectedPathForDetails.getPerkAmplifier() + 1).getString();
            graphics.drawString(this.font, pText, contentX + 6, cursorY, 0xFF4ADE80, false);
            cursorY += 14;
        }

        // 2. Cooldown Switch Section
        int minVal = selectedPathForDetails.getMinToSwitch();
        if (minVal > 0) {
            String switchText = Component.translatable("xam.screen.path_selection.min_to_switch_label", minVal).getString();
            graphics.drawString(this.font, switchText, contentX, cursorY, TEXT_MUTED, false);
            cursorY += 14;
        }

        // 3. Dependencies
        if (!selectedPathForDetails.getDependencies().isEmpty()) {
            graphics.drawString(this.font, Component.translatable("xam.screen.path_selection.deps_header").getString(), contentX, cursorY, COLOR_COPPER, false);
            cursorY += 12;
            for (String dep : selectedPathForDetails.getDependencies()) {
                boolean met = MasteryService.isDependencyMet(net.minecraft.client.Minecraft.getInstance().player, playerData, dep);
                String[] parts = dep.split(":");
                String depId = parts[0];
                String amt = parts.length > 1 ? parts[1] : "mastered";
                PathInfo depPath = ConfigManager.PATHS_MAP.get(depId);
                String name = depPath != null ? depPath.getName() : depId;
                String depLabel;
                if (amt.equalsIgnoreCase("mastered") || amt.equalsIgnoreCase("all")) {
                    depLabel = met ? "§a[✓]§r " : "§c[✘]§r ";
                    depLabel += Component.translatable("xam.screen.path_selection.master_path", name).getString();
                } else {
                    depLabel = met ? "§a[✓]§r " : "§c[✘]§r ";
                    depLabel += Component.translatable("xam.screen.path_selection.reqs_format", name, amt).getString();
                }
                graphics.drawString(this.font, depLabel, contentX + 6, cursorY, met ? 0xFF8FD68F : TEXT_SECONDARY, false);
                cursorY += 11;
            }
            cursorY += 4;
        }

        String collectBadge = Component.translatable("xam.req_type.badge.collect").getString();
        int maxBadgeW = this.font.width(collectBadge) + 6;

        for (Requirement req : selectedPathForDetails.getRequirements()) {
            boolean done = isRequirementCompletedClient(selectedPathForDetails, req);
            String symbol = done ? "§a[✓]§r" : "§c[✘]§r";
            String reqName = resolveRequirementName(req);
            String typeBadge = Component.translatable("xam.req_type.badge." + req.getType().toLowerCase()).getString();
            int rNameMaxW = contentW - 24 - maxBadgeW - 4;
            if (this.font.width(reqName) > rNameMaxW) {
                reqName = this.font.plainSubstrByWidth(reqName, rNameMaxW - 10) + "...";
            }
            graphics.drawString(this.font, symbol + " " + reqName, contentX + 6, cursorY, done ? 0xFF8FD68F : TEXT_PRIMARY, false);
            drawFlatPanel(graphics, contentX + contentW - maxBadgeW - 2, cursorY - 1, maxBadgeW, 11, 0xFF140F0D, 0xFF2C221D);
            graphics.drawString(this.font, typeBadge, contentX + contentW - maxBadgeW + 1, cursorY, COLOR_BRASS, false);
            cursorY += 12;

            String rDesc = resolveRequirementDescription(req);
            if (!rDesc.isEmpty()) {
                java.util.List<String> wrapLines = wrapText(rDesc, contentW - 20, this.font);
                for (String line : wrapLines) {
                    graphics.drawString(this.font, line, contentX + 18, cursorY, TEXT_MUTED, false);
                    cursorY += 10;
                }
            }
            cursorY += 4;
        }

        graphics.disableScissor();

        // Update content height
        modalContentHeight = cursorY - startY;

        // Draw manual scrollbar on the right side of the viewport if there is overflow
        int maxScroll = Math.max(0, modalContentHeight - viewportH);
        if (modalContentHeight > viewportH) {
            boolean isScrollingActive = (System.currentTimeMillis() - lastScrollTime < 3000) || isDraggingScrollbar;
            if (isScrollingActive) {
                int scrollbarX = modalX + modalW - 6;
                int scrollbarY = viewportY;
                int scrollbarW = 3;
                int scrollbarH = viewportH;

                // Draw track line
                graphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarW, scrollbarY + scrollbarH, 0xFF140F0D);

                // Draw thumb
                int thumbH = Math.max(12, (viewportH * viewportH) / modalContentHeight);
                int thumbY = viewportY + (modalScrollOffset * (viewportH - thumbH)) / maxScroll;
                graphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarW, thumbY + thumbH, COLOR_BRASS);
            }
        }

        graphics.pose().popPose();
    }

    private String resolveRequirementDescription(Requirement req) {
        String desc = req.getDescription();
        if (desc == null || desc.isEmpty()) return "";
        String translated = Component.translatable(desc).getString();
        if (!translated.equals(desc)) {
            return translated;
        }
        return desc;
    }

    private String resolveRequirementName(Requirement req) {
        String name = req.getName();
        if (name != null && !name.isEmpty()) {
            String translated = Component.translatable(name).getString();
            return translated.equals(name) ? name : translated;
        }
        return formatRequirement(req);
    }

    private java.util.List<String> wrapText(String text, int maxWidth, net.minecraft.client.gui.Font font) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text.isEmpty()) return lines;
        if (font.width(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
            } else if (font.width(current + " " + word) <= maxWidth) {
                current.append(" ").append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
                while (font.width(current.toString()) > maxWidth && current.length() > 1) {
                    int cut = 1;
                    while (cut < current.length() && font.width(current.substring(0, cut + 1)) <= maxWidth) {
                        cut++;
                    }
                    lines.add(current.substring(0, cut) + "-");
                    current = new StringBuilder(current.substring(cut));
                }
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private boolean canExit() {
        return true;
    }

    private boolean isRequirementCompletedClient(PathInfo path, Requirement req) {
        if (req.getType().equals("advancement")) {
            return org.xam.client.ClientPacketHandler.isClientAdvancementCompleted(req.getId());
        } else {
            String reqKey = org.xam.progression.MasteryService.getRequirementKey(path.getId(), req);
            return playerData != null && playerData.getCompletedRequirements().contains(reqKey);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return canExit();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedPathForDetails == null) return super.mouseScrolled(mouseX, mouseY, delta);

        int scrollAmount = (int) (-delta * 12);
        int modalH = Math.min(220, (int) (this.height * 0.85));
        int viewportH = modalH - 44;
        int maxScroll = Math.max(0, modalContentHeight - viewportH);

        modalScrollOffset = Math.max(0, Math.min(maxScroll, modalScrollOffset + scrollAmount));
        lastScrollTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && selectedPathForDetails != null) {
            int modalW = Math.min(320, (int) (this.width * 0.9));
            int modalH = Math.min(220, (int) (this.height * 0.85));
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;
            int viewportY = modalY + 34;
            int viewportH = modalH - 44;

            if (modalContentHeight > viewportH) {
                int thumbH = Math.max(12, (viewportH * viewportH) / modalContentHeight);
                int maxScroll = modalContentHeight - viewportH;
                double relativeY = mouseY - viewportY;
                int newOffset = (int) ((relativeY - thumbH / 2.0) / (viewportH - thumbH) * maxScroll);
                modalScrollOffset = Math.max(0, Math.min(maxScroll, newOffset));
                lastScrollTime = System.currentTimeMillis();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && selectedPathForDetails != null) {
            selectedPathForDetails = null;
            modalScrollOffset = 0;
            isDraggingScrollbar = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
