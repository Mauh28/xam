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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public class PerksConfigScreen extends AbstractMasteryScreen {
    private final Screen parent;
    private final PathInfo path;

    private EditBox effectIdEdit;
    private int perkAmplifier;

    // Preset options
    private static class PerkPreset {
        final String name;
        final String effectId;

        PerkPreset(String name, String effectId) {
            this.name = name;
            this.effectId = effectId;
        }
    }
    private final List<PerkPreset> presets = new ArrayList<>();

    // Layout fields
    private int panelW, panelH, panelX, panelY;
    private int fieldX, fieldY, fieldW;
    private int ampX, ampY, ampW;
    private int btnPickerX, btnPickerY, btnPickerW, btnPickerH;

    public PerksConfigScreen(Screen parent, PathInfo path) {
        super(Component.literal("CONFIGURAR PERKS DE LA RAMA"));
        this.parent = parent;
        this.path = path;
        this.perkAmplifier = path.perkAmplifier;

        // Populate presets
        presets.add(new PerkPreset("Velocidad", "minecraft:speed"));
        presets.add(new PerkPreset("Prisa Minera", "minecraft:haste"));
        presets.add(new PerkPreset("Resistencia", "minecraft:resistance"));
        presets.add(new PerkPreset("Regeneración", "minecraft:regeneration"));
        presets.add(new PerkPreset("Fuerza", "minecraft:strength"));
        presets.add(new PerkPreset("Sin Perk", ""));
    }

    @Override
    protected void init() {
        super.init();

        this.panelW = containerW - 40;
        this.panelH = bodyH - 20;
        this.panelX = containerX + 20;
        this.panelY = bodyY + 10;

        this.fieldX = panelX + 20;
        this.fieldY = panelY + 45;
        this.fieldW = (int) (panelW * 0.65);

        this.btnPickerX = fieldX + fieldW + 10;
        this.btnPickerY = fieldY;
        this.btnPickerW = 24;
        this.btnPickerH = 20;

        this.ampX = fieldX;
        this.ampY = fieldY + 38;
        this.ampW = fieldW;

        this.effectIdEdit = new EditBox(this.font, fieldX + 4, fieldY + 5, fieldW - 8, 12, Component.literal("ID del Efecto"));
        this.effectIdEdit.setBordered(false);
        this.effectIdEdit.setTextColor(TEXT_PRIMARY);
        this.effectIdEdit.setValue(path.perkEffect != null ? path.perkEffect : "");
        this.addRenderableWidget(this.effectIdEdit);
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, Component.translatable("xam.screen.perks_config.title", Component.translatable(path.name).getString().toUpperCase()).getString(), containerX + 15, titleY, COLOR_BRASS, false);
        drawBackButton(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnW = 90;
        int btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        // Custom Discard button
        boolean discHovered = mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int discBg = discHovered ? 0xFF2A1C1A : 0xFF1C1312;
        int discBorder = discHovered ? 0xFFFF5555 : 0xFF3E2D2B;
        drawFlatPanel(graphics, startX, btnY, btnW, btnH, discBg, discBorder);
        graphics.drawCenteredString(this.font, Component.translatable("xam.editor.cancel").getString(), startX + btnW / 2, btnY + 6, discHovered ? TEXT_PRIMARY : TEXT_SECONDARY);

        // Custom Save button
        int saveX = startX + btnW + 10;
        boolean saveHovered = mouseX >= saveX && mouseX < saveX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int saveBg = saveHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int saveBorder = saveHovered ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, saveX, btnY, btnW, btnH, saveBg, saveBorder);
        graphics.drawCenteredString(this.font, Component.translatable("xam.editor.accept").getString(), saveX + btnW / 2, btnY + 6, TEXT_PRIMARY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Draw main config card
        drawFlatPanel(graphics, panelX, panelY, panelW, panelH, PANEL_INNER_BG, COLOR_COPPER);

        // Title of details
        graphics.drawString(this.font, Component.translatable("xam.screen.perks_config.header").getString(), panelX + 15, panelY + 12, COLOR_BRASS, false);
        graphics.fill(panelX + 15, panelY + 23, panelX + panelW - 15, panelY + 24, 0xFF2C221D);

        // Label for Effect ID input
        graphics.drawString(this.font, Component.translatable("xam.screen.perks_config.effect_id").getString(), fieldX, fieldY - 10, TEXT_SECONDARY, false);
        drawFlatPanel(graphics, fieldX, fieldY, fieldW, 20, INPUT_BACKGROUND, COLOR_COPPER);

        // Draw "..." picker button
        boolean pickerHover = mouseX >= btnPickerX && mouseX < btnPickerX + btnPickerW && mouseY >= btnPickerY && mouseY < btnPickerY + btnPickerH;
        int pickerBg = pickerHover ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int pickerBorder = pickerHover ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, btnPickerX, btnPickerY, btnPickerW, btnPickerH, pickerBg, pickerBorder);
        graphics.drawCenteredString(this.font, "...", btnPickerX + btnPickerW / 2, btnPickerY + 6, TEXT_PRIMARY);

        // Label and selector for level (Amplifier)
        graphics.drawString(this.font, Component.translatable("xam.screen.perks_config.intensity").getString(), ampX, ampY - 10, TEXT_SECONDARY, false);
        
        // Decrement button (-)
        int decX = ampX;
        boolean decHover = mouseX >= decX && mouseX < decX + 20 && mouseY >= ampY && mouseY < ampY + 16;
        drawFlatPanel(graphics, decX, ampY, 20, 16, decHover ? COLOR_COPPER_HOVER : COLOR_COPPER, decHover ? COLOR_BRASS : 0xFF2C221D);
        graphics.drawCenteredString(this.font, "-", decX + 10, ampY + 4, TEXT_PRIMARY);

        // Level text display (e.g. Level I, Level II, Level III)
        String levelStr = Component.translatable("xam.screen.perks_config.level_format", perkAmplifier + 1).getString();
        graphics.drawString(this.font, levelStr, decX + 20 + 8, ampY + 4, COLOR_BRASS, false);

        // Increment button (+)
        int incX = decX + 20 + 8 + this.font.width(levelStr) + 8;
        boolean incHover = mouseX >= incX && mouseX < incX + 20 && mouseY >= ampY && mouseY < ampY + 16;
        drawFlatPanel(graphics, incX, ampY, 20, 16, incHover ? COLOR_COPPER_HOVER : COLOR_COPPER, incHover ? COLOR_BRASS : 0xFF2C221D);
        graphics.drawCenteredString(this.font, "+", incX + 10, ampY + 4, TEXT_PRIMARY);

        // Render Preset pills/buttons below - Adjustable Grid Layout with Icons and Tooltips
        int presetY = ampY + 22;
        graphics.drawString(this.font, Component.translatable("xam.screen.perks_config.presets").getString(), fieldX, presetY + 6, TEXT_MUTED, false);
        
        int px = fieldX + 52;
        int btnSize = 22;
        int btnGap = 6;
        int maxGridX = panelX + panelW - 20;

        String hoveredPresetName = null;

        for (int i = 0; i < presets.size(); i++) {
            PerkPreset preset = presets.get(i);
            
            if (px + btnSize > maxGridX) {
                presetY += btnSize + btnGap;
                px = fieldX + 52;
            }

            boolean pActive = effectIdEdit.getValue().equals(preset.effectId);
            boolean pHover = mouseX >= px && mouseX < px + btnSize && mouseY >= presetY && mouseY < presetY + btnSize;
            
            if (pHover) {
                hoveredPresetName = preset.name;
            }

            int pBg = pActive ? 0xFF2A593E : (pHover ? 0xFF251E1C : 0xFF171312);
            int pBorder = pActive ? 0xFF55FF55 : (pHover ? COLOR_BRASS : 0xFF3E332E);
            drawFlatPanel(graphics, px, presetY, btnSize, btnSize, pBg, pBorder);

            // Render effect icon or fallback inside the button
            if (preset.effectId.isEmpty()) {
                net.minecraft.world.item.ItemStack barrier = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER);
                graphics.pose().pushPose();
                graphics.pose().translate(px + (btnSize - 16) / 2.0F, presetY + (btnSize - 16) / 2.0F, 0);
                graphics.renderFakeItem(barrier, 0, 0);
                graphics.pose().popPose();
            } else {
                net.minecraft.world.effect.MobEffect effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(net.minecraft.resources.ResourceLocation.tryParse(preset.effectId));
                if (effect != null) {
                    net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
                    if (sprite != null) {
                        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
                        graphics.blit(px + (btnSize - 18) / 2, presetY + (btnSize - 18) / 2, 0, 18, 18, sprite);
                    }
                }
            }

            px += btnSize + btnGap;
        }

        // Draw hovered preset tooltip
        if (hoveredPresetName != null) {
            int ttX = mouseX + 12;
            int ttY = mouseY - 12;
            int ttW = this.font.width(hoveredPresetName) + 12;
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
            graphics.drawString(this.font, hoveredPresetName, ttX + 6, ttY + 6, TEXT_PRIMARY, false);
            graphics.pose().popPose();
        }

        // Draw overlay input boxes
        this.effectIdEdit.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isBackButtonClicked(mouseX, mouseY)) {
            playClickSound();
            Minecraft.getInstance().setScreen(this.parent);
            return true;
        }

        if (button == 0) {
            // "..." button click
            if (mouseX >= btnPickerX && mouseX < btnPickerX + btnPickerW && mouseY >= btnPickerY && mouseY < btnPickerY + btnPickerH) {
                playClickSound();
                Minecraft.getInstance().setScreen(new EffectSelectionScreen(this, (effect) -> {
                    net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect);
                    if (rl != null) {
                        this.effectIdEdit.setValue(rl.toString());
                    }
                }));
                return true;
            }

            // Preset pills clicks - Grid Layout
            int presetY = ampY + 22;
            int px = fieldX + 52;
            int btnSize = 22;
            int btnGap = 6;
            int maxGridX = panelX + panelW - 20;

            for (PerkPreset preset : presets) {
                if (px + btnSize > maxGridX) {
                    presetY += btnSize + btnGap;
                    px = fieldX + 52;
                }
                if (mouseX >= px && mouseX < px + btnSize && mouseY >= presetY && mouseY < presetY + btnSize) {
                    playClickSound();
                    effectIdEdit.setValue(preset.effectId);
                    return true;
                }
                px += btnSize + btnGap;
            }

            // Decrement Level
            int decX = ampX;
            if (mouseX >= decX && mouseX < decX + 20 && mouseY >= ampY && mouseY < ampY + 16) {
                if (perkAmplifier > 0) {
                    playClickSound();
                    perkAmplifier--;
                }
                return true;
            }

            // Increment Level
            String levelStr = Component.translatable("xam.screen.perks_config.level_format", perkAmplifier + 1).getString();
            int incX = decX + 20 + 8 + this.font.width(levelStr) + 8;
            if (mouseX >= incX && mouseX < incX + 20 && mouseY >= ampY && mouseY < ampY + 16) {
                if (perkAmplifier < 9) { // Max Level 10
                    playClickSound();
                    perkAmplifier++;
                }
                return true;
            }

            // Footer buttons (Cancelar / Aceptar)
            int btnW = 90;
            int btnH = 20;
            int startX = containerX + containerW - 15 - (btnW * 2 + 10);
            int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

            // Cancel
            if (mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                Minecraft.getInstance().setScreen(this.parent);
                return true;
            }

            // Save
            int saveX = startX + btnW + 10;
            if (mouseX >= saveX && mouseX < saveX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                path.perkEffect = effectIdEdit.getValue().trim();
                path.perkAmplifier = perkAmplifier;
                Minecraft.getInstance().setScreen(this.parent);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
