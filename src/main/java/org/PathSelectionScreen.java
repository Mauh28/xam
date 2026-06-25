package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;

public class PathSelectionScreen extends AbstractMasteryScreen {
    private final PlayerData playerData;
    private final List<xdAbsoluteMastery.ConfigManager.PathInfo> availablePaths = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 1;

    public PathSelectionScreen(PlayerData playerData) {
        super(Component.literal("SELECCIÓN DE RAMA"));
        this.playerData = playerData;
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

        totalPages = (availablePaths.size() + 2) / 3;
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, "SELECCIÓN DE RAMA", containerX + 15, titleY, TEXT_PRIMARY, false);
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
        }
        drawFlatPanel(graphics, prevX, prevY, 50, 80, prevBg, prevBorder);
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
        }
        drawFlatPanel(graphics, nextX, nextY, 50, 80, nextBg, nextBorder);
        int nextCol = nextActive ? (nextHovered ? TEXT_PRIMARY : TEXT_SECONDARY) : TEXT_MUTED;
        graphics.drawCenteredString(this.font, "▶", nextX + 25, nextY + 36, nextCol);

        // Render cards grid layout: 3 cards per row
        int startIndex = currentPage * 3;
        int endIndex = Math.min(startIndex + 3, availablePaths.size());

        int viewportW = containerW - 120;
        int gap = 15;
        int cardW = (viewportW - (gap * 4)) / 3;
        int cardH = bodyH - 30;
        int cardY = bodyY + 15;

        for (int i = 0; i < (endIndex - startIndex); i++) {
            xdAbsoluteMastery.ConfigManager.PathInfo path = availablePaths.get(startIndex + i);
            int cardX = containerX + 60 + gap + i * (cardW + gap);

            boolean cardHovered = mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardH;
            int borderColor = cardHovered ? 0xFFAAAAAA : BORDER_INNER;

            // Draw Card Panel
            drawFlatPanel(graphics, cardX, cardY, cardW, cardH, WIDGET_BACKGROUND, borderColor);

            // Card Height Breakdowns:
            int cabH = (int) (cardH * 0.20);
            int bodyReqH = (int) (cardH * 0.60);
            int pieH = cardH - cabH - bodyReqH;

            // 1. Cabeza (20%)
            int sqSize = (int) (cabH * 0.85);
            int sqX = cardX + 8;
            int sqY = cardY + (cabH - sqSize) / 2;
            drawFlatPanel(graphics, sqX, sqY, sqSize, sqSize, INPUT_BACKGROUND, 0xFF555555);

            ItemStack icon = getIconForPath(path.id);
            graphics.renderFakeItem(icon, sqX + (sqSize - 16) / 2, sqY + (sqSize - 16) / 2);

            String nameText = path.name;
            int nameMaxW = cardW - sqSize - 22;
            if (this.font.width(nameText) > nameMaxW) {
                nameText = this.font.plainSubstrByWidth(nameText, nameMaxW - 10) + "...";
            }
            graphics.drawString(this.font, nameText, cardX + sqSize + 14, cardY + (cabH - 18) / 2, TEXT_PRIMARY, false);

            String modText = path.mod_id;
            int labelW = Math.min(this.font.width(modText) + 8, nameMaxW);
            drawFlatPanel(graphics, cardX + sqSize + 14, cardY + cabH - 14, labelW, 11, 0xFF353535, BORDER_INNER);
            String dispMod = modText;
            if (this.font.width(modText) > labelW - 6) {
                dispMod = this.font.plainSubstrByWidth(modText, labelW - 10) + "..";
            }
            graphics.drawString(this.font, dispMod, cardX + sqSize + 18, cardY + cabH - 12, TEXT_MUTED, false);

            // Cabeza bottom line divider
            graphics.fill(cardX + 2, cardY + cabH, cardX + cardW - 2, cardY + cabH + 2, BORDER_INNER);

            // 2. Cuerpo (60%)
            graphics.drawString(this.font, "Requisitos", cardX + 10, cardY + cabH + 6, TEXT_MUTED, false);
            int labelWidth = this.font.width("Requisitos");
            graphics.fill(cardX + 10, cardY + cabH + 15, cardX + 10 + labelWidth, cardY + cabH + 16, 0xFF555555);

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

            // Cuerpo bottom line divider
            graphics.fill(cardX + 2, cardY + cabH + bodyReqH, cardX + cardW - 2, cardY + cabH + bodyReqH + 2, BORDER_INNER);

            // 3. Pie (20%)
            int btnX = cardX + 8;
            int btnY = cardY + cabH + bodyReqH + 4;
            int btnW = cardW - 16;
            int btnH = pieH - 8;
            drawFlatButton(graphics, btnX, btnY, btnW, btnH, "ELEGIR CAMINO", mouseX, mouseY, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
            int startIndex = currentPage * 3;
            int endIndex = Math.min(startIndex + 3, availablePaths.size());

            int viewportW = containerW - 120;
            int gap = 15;
            int cardW = (viewportW - (gap * 4)) / 3;
            int cardH = bodyH - 30;
            int cardY = bodyY + 15;

            int cabH = (int) (cardH * 0.20);
            int bodyReqH = (int) (cardH * 0.60);
            int pieH = cardH - cabH - bodyReqH;

            for (int i = 0; i < (endIndex - startIndex); i++) {
                xdAbsoluteMastery.ConfigManager.PathInfo path = availablePaths.get(startIndex + i);
                int cardX = containerX + 60 + gap + i * (cardW + gap);
                int btnX = cardX + 8;
                int btnY = cardY + cabH + bodyReqH + 4;
                int btnW = cardW - 16;
                int btnH = pieH - 8;

                if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    xdAbsoluteMastery.CHANNEL.sendToServer(new xdAbsoluteMastery.SelectPathPacket(path.id));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private ItemStack getIconForPath(String pathId) {
        if (pathId.equals("botania")) {
            return new ItemStack(Items.POPPY);
        } else if (pathId.equals("mekanism")) {
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

    @Override
    public boolean shouldCloseOnEsc() {
        if (playerData != null && playerData.getCurrentPath() == null) {
            for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
                if (!playerData.getMasteredPaths().contains(path.id)) {
                    return false;
                }
            }
        }
        return true;
    }
}
