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

        List<xdAbsoluteMastery.ConfigManager.PathInfo> availablePaths = new ArrayList<>();
        for (xdAbsoluteMastery.ConfigManager.PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
            if (playerData == null || !playerData.getMasteredPaths().contains(path.id)) {
                availablePaths.add(path);
            }
        }

        int cardWidth = 150;
        int cardHeight = 160;
        int spacing = 25;
        int totalWidth = (availablePaths.size() * cardWidth) + ((availablePaths.size() - 1) * spacing);
        int startX = this.width / 2 - totalWidth / 2;
        int startY = this.height / 2 - cardHeight / 2 + 10;

        for (int i = 0; i < availablePaths.size(); i++) {
            CardInfo card = new CardInfo();
            card.path = availablePaths.get(i);
            card.x = startX + i * (cardWidth + spacing);
            card.y = startY;
            card.width = cardWidth;
            card.height = cardHeight;
            cards.add(card);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw a dark gradient background
        this.renderBackground(guiGraphics);

        // Draw title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 25, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Elige sabiamente. Una vez iniciada, deberás dominarla para cambiar.", this.width / 2, 40, 0x888888);

        for (CardInfo card : cards) {
            card.hovered = mouseX >= card.x && mouseX < card.x + card.width && mouseY >= card.y && mouseY < card.y + card.height;

            // Draw Card Background (glassmorphism/dark panel look)
            int bgColor = card.hovered ? 0xDD15151A : 0xAA0F0F12;
            guiGraphics.fill(card.x, card.y, card.x + card.width, card.y + card.height, bgColor);

            // Draw Border Outline
            int borderColor = card.hovered ? 0xFFFFD700 : 0x44FFFFFF; // Gold vs White
            guiGraphics.renderOutline(card.x, card.y, card.width, card.height, borderColor);

            // Draw Path Name
            int titleColor = card.hovered ? 0xFFFFD700 : 0xFFFFFF;
            guiGraphics.drawCenteredString(this.font, card.path.name, card.x + card.width / 2, card.y + 15, titleColor);

            // Draw Icon (Book and quill or path specific)
            ItemStack icon = getIconForPath(card.path.id);
            int iconX = card.x + card.width / 2 - 8;
            int iconY = card.y + 40;
            guiGraphics.renderFakeItem(icon, iconX, iconY);

            // Draw requirements list
            int reqY = card.y + 75;
            guiGraphics.drawCenteredString(this.font, "Requisitos:", card.x + card.width / 2, reqY, 0x888888);

            int index = 0;
            for (String adv : card.path.mastery_advancements) {
                String label = simplifyAdvancementName(adv);
                guiGraphics.drawCenteredString(this.font, "- " + label, card.x + card.width / 2, reqY + 12 + index * 10, 0xAAAAAA);
                index++;
            }

            if (card.hovered) {
                guiGraphics.drawCenteredString(this.font, "[ Haz click para elegir ]", card.x + card.width / 2, card.y + card.height - 18, 0x55FF55);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            for (CardInfo card : cards) {
                if (mouseX >= card.x && mouseX < card.x + card.width && mouseY >= card.y && mouseY < card.y + card.height) {
                    // Send to server
                    xdAbsoluteMastery.CHANNEL.sendToServer(new xdAbsoluteMastery.SelectPathPacket(card.path.id));

                    // Play click sound
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

    private String simplifyAdvancementName(String adv) {
        if (adv.contains(":")) {
            adv = adv.split(":")[1];
        }
        if (adv.contains("/")) {
            String[] split = adv.split("/");
            adv = split[split.length - 1];
        }
        // Capitalize and replace underscores
        adv = adv.replace("_", " ");
        if (!adv.isEmpty()) {
            adv = Character.toUpperCase(adv.charAt(0)) + adv.substring(1);
        }
        return adv;
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
