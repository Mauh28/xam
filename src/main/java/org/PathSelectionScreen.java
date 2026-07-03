package org;

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
    private final List<xdAbsoluteMastery.ConfigManager.PathInfo> availablePaths = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 1;
    private int cardsPerPage = 3;

    public PathSelectionScreen(Screen parent, PlayerData playerData) {
        super(Component.literal("SELECCIÓN DE RAMA"));
        this.parent = parent;
        this.playerData = playerData;
    }

    public PathSelectionScreen(PlayerData playerData) {
        this(null, playerData);
    }

    @Override
    protected void init() {
        super.init();
        
        availablePaths.clear();
        for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
            if (playerData == null || !playerData.getMasteredPaths().contains(path.id)) {
                availablePaths.add(path);
            }
        }

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
        graphics.drawString(this.font, "SELECCIÓN DE RAMA", containerX + 15, titleY, TEXT_PRIMARY, false);
        if (canExit()) {
            drawBackButton(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        String pageIndicator = String.format("[ Página %d de %d ]", currentPage + 1, Math.max(1, totalPages));
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
            xdAbsoluteMastery.ConfigManager.PathInfo path = availablePaths.get(startIndex + i);
            int cardX = containerX + 60 + gap + i * (cardW + gap);

            boolean hasTasks = !path.requirements.isEmpty();
            boolean isUnlocked = hasTasks && xdAbsoluteMastery.areDependenciesMastered(net.minecraft.client.Minecraft.getInstance().player, playerData, path);
            boolean isActivePath = playerData != null && path.id.equals(playerData.getCurrentPath());
            boolean hasActivePath = playerData != null && playerData.getCurrentPath() != null;
            boolean canSwitch = xdAbsoluteMastery.canSwitchFromCurrentPath(net.minecraft.client.Minecraft.getInstance().player, playerData);
            boolean isSelectable = isUnlocked && (!hasActivePath || isActivePath || canSwitch);

            boolean cardHovered = mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardH;
            
            int cardBg = isSelectable ? (cardHovered ? adjustColorBrightness(currentWidgetBg, 12) : currentWidgetBg) : 0xFF181515;
            int cardBorder;
            if (isActivePath) {
                cardBorder = COLOR_BRASS;
            } else if (isSelectable) {
                cardBorder = cardHovered ? COLOR_BRASS : currentBorderStd;
            } else {
                cardBorder = cardHovered ? 0xFF884444 : 0xFF553333;
            }

            // Draw Card Panel
            drawFlatPanel(graphics, cardX, cardY, cardW, cardH, cardBg, cardBorder);

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

            String nameText = path.name;
            int nameMaxW = cardW - sqSize - 22;
            if (this.font.width(nameText) > nameMaxW) {
                nameText = this.font.plainSubstrByWidth(nameText, nameMaxW - 10) + "...";
            }
            graphics.drawString(this.font, nameText, cardX + sqSize + 14, cardY + (cabH - 18) / 2, COLOR_BRASS, false);

            String modText = path.mod_id;
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
                    graphics.drawString(this.font, "Bloqueado por progreso", cardX + 10, cardY + cabH + 6, 0xFFFF5555, false);
                    int labelWidth = this.font.width("Bloqueado por progreso");
                    graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, 0xFF772222);

                    int reqY = cardY + cabH + 25;
                    String activeId = playerData.getCurrentPath();
                    xdAbsoluteMastery.ConfigManager.PathInfo activePath = xdAbsoluteMastery.ConfigManager.PATHS_MAP.get(activeId);
                    String activeName = activePath != null ? activePath.name : activeId;
                    int min = activePath != null ? activePath.min_to_switch : 0;
                    
                    String line1 = "Completa la maestría";
                    String line2 = "o llega a " + min + " reqs";
                    String line3 = "de " + activeName;
                    
                    graphics.drawString(this.font, line1, cardX + 10, reqY, TEXT_MUTED, false);
                    graphics.drawString(this.font, line2, cardX + 10, reqY + 11, TEXT_MUTED, false);
                    graphics.drawString(this.font, line3, cardX + 10, reqY + 22, TEXT_MUTED, false);
                } else {
                    graphics.drawString(this.font, "Requisitos", cardX + 10, cardY + cabH + 6, COLOR_BRASS, false);
                    int labelWidth = this.font.width("Requisitos");
                    graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, COLOR_COPPER);

                    // Clipping/Scissor region for requirements list text
                    double scale = Minecraft.getInstance().getWindow().getGuiScale();
                    int scissorX = (int) (cardX * scale);
                    int scissorY = (int) ((this.height - (cardY + cabH + bodyReqH)) * scale);
                    int scissorW = (int) (cardW * scale);
                    int scissorH = (int) ((bodyReqH - 18) * scale);

                    RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

                    int reqY = cardY + cabH + 20;
                    for (xdAbsoluteMastery.ConfigManager.Requirement req : path.requirements) {
                        String label = formatRequirement(req);
                        if (this.font.width(label) > cardW - 26) {
                            label = this.font.plainSubstrByWidth(label, cardW - 36) + "...";
                        }
                        graphics.fill(cardX + 10, reqY + 2, cardX + 14, reqY + 6, TEXT_MUTED);
                        graphics.drawString(this.font, label, cardX + 18, reqY, TEXT_SECONDARY, false);
                        reqY += 10;
                    }

                    RenderSystem.disableScissor();
                }
            } else {
                if (!hasTasks) {
                    graphics.drawString(this.font, "Sin tareas asignadas", cardX + 10, cardY + cabH + 6, 0xFFFF5555, false);
                    int labelWidth = this.font.width("Sin tareas asignadas");
                    graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, 0xFF772222);

                    int reqY = cardY + cabH + 25;
                    graphics.drawString(this.font, "Esta maestría no tiene", cardX + 10, reqY, TEXT_MUTED, false);
                    graphics.drawString(this.font, "tareas registradas aún.", cardX + 10, reqY + 11, TEXT_MUTED, false);
                } else {
                    graphics.drawString(this.font, "Requiere:", cardX + 10, cardY + cabH + 6, 0xFFFF5555, false);
                    int labelWidth = this.font.width("Requiere:");
                    graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, 0xFF772222);

                    int reqY = cardY + cabH + 20;
                    List<String> missingNames = new ArrayList<>();
                    for (String dep : path.dependencies) {
                        if (!xdAbsoluteMastery.isDependencyMet(Minecraft.getInstance().player, playerData, dep)) {
                            String[] parts = dep.split(":");
                            String depId = parts[0];
                            String amt = parts.length > 1 ? parts[1] : "mastered";

                            xdAbsoluteMastery.ConfigManager.PathInfo depPath = xdAbsoluteMastery.ConfigManager.PATHS_MAP.get(depId);
                            String name = depPath != null ? depPath.name : depId;
                            if (amt.equalsIgnoreCase("mastered") || amt.equalsIgnoreCase("all")) {
                                missingNames.add("Dominar " + name);
                            } else {
                                missingNames.add(name + " (" + amt + " reqs)");
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
                btnText = "EQUIPADO";
                btnEnabled = false;
            } else if (isSelectable) {
                btnText = "ELEGIR CAMINO";
                btnEnabled = true;
            } else {
                btnText = "BLOQUEADO";
                btnEnabled = false;
            }

            drawFlatButton(graphics, btnX, btnY, btnW, btnH, btnText, mouseX, mouseY, btnEnabled);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                return true;
            }

            // Check right page button
            int nextX = containerX + containerW - 60;
            int nextY = bodyY + (bodyH - 80) / 2;
            if (currentPage < totalPages - 1 && mouseX >= nextX && mouseX < nextX + 50 && mouseY >= nextY && mouseY < nextY + 80) {
                playClickSound();
                currentPage++;
                return true;
            }

            // Check Card choose button clicks
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

            for (int i = 0; i < (endIndex - startIndex); i++) {
                xdAbsoluteMastery.ConfigManager.PathInfo path = availablePaths.get(startIndex + i);
                boolean hasTasks = !path.requirements.isEmpty();
                boolean isUnlocked = hasTasks && xdAbsoluteMastery.areDependenciesMastered(Minecraft.getInstance().player, playerData, path);
                boolean isActivePath = playerData != null && path.id.equals(playerData.getCurrentPath());
                boolean hasActivePath = playerData != null && playerData.getCurrentPath() != null;
                boolean canSwitch = xdAbsoluteMastery.canSwitchFromCurrentPath(Minecraft.getInstance().player, playerData);
                boolean isSelectable = isUnlocked && (!hasActivePath || isActivePath || canSwitch);

                int cardX = containerX + 60 + gap + i * (cardW + gap);
                int btnX = cardX + 8;
                int btnY = cardY + cabH + bodyReqH + 4;
                int btnW = cardW - 16;
                int btnH = pieH - 8;

                if (isSelectable && !isActivePath && mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    xdAbsoluteMastery.CHANNEL.sendToServer(new xdAbsoluteMastery.SelectPathPacket(path.id));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ItemStack getIconForPath(xdAbsoluteMastery.ConfigManager.PathInfo path) {
        if (path.icon != null) {
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(path.icon));
            if (item != null) {
                return new ItemStack(item);
            }
        }
        if (path.id.equals("botania")) {
            return new ItemStack(Items.POPPY);
        } else if (path.id.equals("mekanism")) {
            return new ItemStack(Items.REDSTONE);
        }
        return new ItemStack(Items.WRITABLE_BOOK);
    }

    private String formatRequirement(xdAbsoluteMastery.ConfigManager.Requirement req) {
        if (req.name != null && !req.name.isEmpty()) {
            return req.name;
        }
        String name = req.id;
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

    private boolean canExit() {
        if (playerData == null) return true;
        if (playerData.getCurrentPath() != null) return true;
        for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
            if (!playerData.getMasteredPaths().contains(path.id) && xdAbsoluteMastery.areDependenciesMastered(Minecraft.getInstance().player, playerData, path)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return canExit();
    }
}
