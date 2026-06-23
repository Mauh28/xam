package org;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class PathSelectionScreen extends Screen {
    private final PlayerData playerData;
    private final List<CardInfo> cards = new ArrayList<>();
    private final List<xdAbsoluteMastery.ConfigManager.PathInfo> availablePaths = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 1;

    private static class CardInfo {
        xdAbsoluteMastery.ConfigManager.PathInfo path;
        int x, y, width, height;
        boolean hovered;
    }

    public PathSelectionScreen(PlayerData playerData) {
        super(Component.literal("Selecciona tu Rama de Maestría"));
        this.playerData = playerData;
    }

    @Override
    protected void init() {
        super.init();
        cards.clear();

        availablePaths.clear();
        for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
            if (playerData == null || !playerData.getMasteredPaths().contains(path.id)) {
                availablePaths.add(path);
            }
        }

        int CARDS_PER_PAGE = 2;
        totalPages = (availablePaths.size() + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE;
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        int startIndex = currentPage * CARDS_PER_PAGE;
        int endIndex = Math.min(startIndex + CARDS_PER_PAGE, availablePaths.size());
        
        List<xdAbsoluteMastery.ConfigManager.PathInfo> pagePaths = new ArrayList<>();
        if (startIndex < availablePaths.size()) {
            pagePaths = availablePaths.subList(startIndex, endIndex);
        }

        int cardWidth = 150;
        int cardHeight = 160;
        int spacing = 20;
        int totalWidth = (pagePaths.size() * cardWidth) + ((pagePaths.size() - 1) * spacing);
        int startX = this.width / 2 - totalWidth / 2;
        int startY = this.height / 2 - cardHeight / 2 + 5;

        for (int i = 0; i < pagePaths.size(); i++) {
            CardInfo card = new CardInfo();
            card.path = pagePaths.get(i);
            card.x = startX + i * (cardWidth + spacing);
            card.y = startY;
            card.width = cardWidth;
            card.height = cardHeight;
            cards.add(card);
        }

        if (totalPages > 1) {
            net.minecraft.client.gui.components.Button prevBtn = net.minecraft.client.gui.components.Button.builder(Component.literal("<-"), b -> {
                if (currentPage > 0) {
                    currentPage--;
                    this.init(this.minecraft, this.width, this.height);
                }
            }).bounds(this.width / 2 - 50, startY + cardHeight + 8, 20, 16).build();
            prevBtn.active = currentPage > 0;
            this.addRenderableWidget(prevBtn);

            net.minecraft.client.gui.components.Button nextBtn = net.minecraft.client.gui.components.Button.builder(Component.literal("->"), b -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    this.init(this.minecraft, this.width, this.height);
                }
            }).bounds(this.width / 2 + 30, startY + cardHeight + 8, 20, 16).build();
            nextBtn.active = currentPage < totalPages - 1;
            this.addRenderableWidget(nextBtn);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 25, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Elige sabiamente. Una vez iniciada, deberás dominarla para cambiar.", this.width / 2, 40, 0x888888);

        for (CardInfo card : cards) {
            card.hovered = mouseX >= card.x && mouseX < card.x + card.width && mouseY >= card.y && mouseY < card.y + card.height;

            int bgColor = card.hovered ? 0xDD15151A : 0xAA0F0F12;
            guiGraphics.fill(card.x, card.y, card.x + card.width, card.y + card.height, bgColor);

            int borderColor = card.hovered ? 0xFFFFD700 : 0x44FFFFFF;
            guiGraphics.renderOutline(card.x, card.y, card.width, card.height, borderColor);

            int titleColor = card.hovered ? 0xFFFFD700 : 0xFFFFFF;
            guiGraphics.drawCenteredString(this.font, card.path.name, card.x + card.width / 2, card.y + 15, titleColor);

            ItemStack icon = getIconForPath(card.path.id);
            int iconX = card.x + card.width / 2 - 8;
            int iconY = card.y + 35;
            guiGraphics.renderFakeItem(icon, iconX, iconY);

            int reqY = card.y + 60;
            guiGraphics.drawCenteredString(this.font, "Requisitos:", card.x + card.width / 2, reqY, 0x888888);

            int index = 0;
            for (xdAbsoluteMastery.ConfigManager.Requirement req : card.path.requirements) {
                String label = formatRequirement(req);
                // Splitting if too long
                if (this.font.width(label) > card.width - 10) {
                    label = this.font.plainSubstrByWidth(label, card.width - 20) + "...";
                }
                guiGraphics.drawCenteredString(this.font, "- " + label, card.x + card.width / 2, reqY + 12 + index * 10, 0xAAAAAA);
                index++;
                if (index >= 7) break; // Limit render size to prevent card overflow
            }

            if (card.hovered) {
                guiGraphics.drawCenteredString(this.font, "[ Haz click ]", card.x + card.width / 2, card.y + card.height - 18, 0x55FF55);
            }
        }

        if (totalPages > 1) {
            int cardHeight = 160;
            int startY = this.height / 2 - cardHeight / 2 + 5;
            guiGraphics.drawCenteredString(this.font, (currentPage + 1) + " / " + totalPages, this.width / 2, startY + cardHeight + 12, 0x888888);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (CardInfo card : cards) {
                if (mouseX >= card.x && mouseX < card.x + card.width && mouseY >= card.y && mouseY < card.y + card.height) {
                    xdAbsoluteMastery.CHANNEL.sendToServer(new xdAbsoluteMastery.SelectPathPacket(card.path.id));

                    net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F
                            )
                    );

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
