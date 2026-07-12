package org.xam.client.gui;

import org.xam.config.ConfigManager;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;
import org.xam.network.XamNetwork;
import org.xam.network.UpdateConfigPacket;
import org.xam.util.PathIcons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;

public class MasteryEditorScreen extends AbstractMasteryScreen {
    private final Screen parent;
    final MasteryEditorModel model = new MasteryEditorModel();

    // Custom text input boxes
    private EditBox pathNameEdit;
    private EditBox pathModIdEdit;
    private EditBox pathDepsEdit;

    // Layout coordinates
    private MasteryEditorLayout layout;
    private final MasteryEditorValidator validator = new MasteryEditorValidator();

    public MasteryEditorScreen(Screen parent) {
        super(Component.translatable("xam.screen.mastery_editor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.layout = new MasteryEditorLayout(containerX, containerY, containerW, bodyY, bodyH);

        // Edit box for Title
        this.pathNameEdit = new EditBox(this.font, layout.titleX + 4, layout.titleY + 5, layout.titleW - 8, 12, Component.translatable("xam.screen.mastery_editor.branch_title"));
        this.pathNameEdit.setBordered(false);
        this.pathNameEdit.setTextColor(TEXT_PRIMARY);
        this.addRenderableWidget(this.pathNameEdit);

        // Edit box for Mod ID (Editable)
        this.pathModIdEdit = new EditBox(this.font, layout.modX + 4, layout.modY + 5, layout.modEditW - 8, 12, Component.translatable("xam.screen.mastery_editor.namespace_mod"));
        this.pathModIdEdit.setBordered(false);
        this.pathModIdEdit.setTextColor(TEXT_PRIMARY);
        this.pathModIdEdit.setEditable(true);
        this.addRenderableWidget(this.pathModIdEdit);

        // Second line editor inputs
        this.pathDepsEdit = new EditBox(this.font, layout.depsX + 4, layout.secondY + 5, layout.depsW - 8, 12, Component.translatable("xam.screen.mastery_editor.branch_dependencies"));
        this.pathDepsEdit.setBordered(false);
        this.pathDepsEdit.setTextColor(TEXT_PRIMARY);
        this.addRenderableWidget(this.pathDepsEdit);

        updateEditors();
    }

    void updateEditors() {
        boolean pathSelected = model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size();
        pathNameEdit.visible = pathSelected;
        pathModIdEdit.visible = pathSelected;
        pathDepsEdit.visible = pathSelected;

        if (pathSelected) {
            PathInfo p = model.getSelectedPath();
            pathNameEdit.setResponder(null);
            pathNameEdit.setValue(p.getName());
            pathNameEdit.setResponder(val -> {
                p.setName(val);
                p.setId(val.toLowerCase().replaceAll("[^a-z0-9_]", "_"));
            });

            pathModIdEdit.setResponder(null);
            pathModIdEdit.setValue(p.getModId());
            pathModIdEdit.setResponder(val -> p.setModId(val));

            pathDepsEdit.setResponder(null);
            pathDepsEdit.setValue(String.join(", ", p.getDependencies()));
            pathDepsEdit.setResponder(val -> {
                p.clearDependencies();
                if (!val.trim().isEmpty()) {
                    for (String dep : val.split(",")) {
                        p.addDependency(dep.trim());
                    }
                }
            });
        }
    }

    private void updateModIdFromRequirements(PathInfo p) {
        if (p.getModId() != null && !p.getModId().isEmpty() && !p.getModId().equals("modid")) {
            return;
        }
        if (p.getRequirements().isEmpty()) return;
        for (Requirement r : p.getRequirements()) {
            if (r.getId() != null && r.getId().contains(":")) {
                String ns = r.getId().split(":")[0];
                if (!ns.equals("minecraft")) {
                    p.setModId(ns);
                    if (pathModIdEdit != null) {
                        pathModIdEdit.setValue(ns);
                    }
                    return;
                }
            }
        }
        String firstId = p.getRequirements().get(0).getId();
        if (firstId != null && firstId.contains(":")) {
            String ns = firstId.split(":")[0];
            p.setModId(ns);
            if (pathModIdEdit != null) {
                pathModIdEdit.setValue(ns);
            }
        }
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.title").getString(), containerX + 15, titleY, COLOR_BRASS, false);
        drawBackButton(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnW = 120;
        int btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        // Render Save Notification if active
        if (System.currentTimeMillis() - model.getSaveNotificationTime() < 3000) {
            int msgY = containerY + containerH - footerH + (footerH - 8) / 2;
            int notifColor = model.getSaveNotificationMsg().startsWith("✕") ? 0xFFFF5555 : 0xFF55FF55;
            graphics.drawString(this.font, model.getSaveNotificationMsg(), containerX + 15, msgY, notifColor, false);
        }

        // Custom Discard button (dark red/grey)
        boolean discHovered = mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int discBg = discHovered ? 0xFF2A1C1A : 0xFF1C1312;
        int discBorder = discHovered ? 0xFFFF5555 : 0xFF3E2D2B;
        drawFlatPanel(graphics, startX, btnY, btnW, btnH, discBg, discBorder);
        graphics.drawCenteredString(this.font, Component.translatable("xam.screen.mastery_editor.discard_all").getString(), startX + btnW / 2, btnY + 6, discHovered ? TEXT_PRIMARY : TEXT_SECONDARY);

        // Custom Save button (copper/brass)
        int saveX = startX + btnW + 10;
        boolean saveHovered = mouseX >= saveX && mouseX < saveX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int saveBg = saveHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int saveBorder = saveHovered ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, saveX, btnY, btnW, btnH, saveBg, saveBorder);
        graphics.drawCenteredString(this.font, Component.translatable("xam.screen.mastery_editor.save_structure").getString(), saveX + btnW / 2, btnY + 6, TEXT_PRIMARY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Sidebar dimensions (25% of body width or fixed width if narrow)
        int sidebarW = layout.sidebarW;
        int sidebarH = bodyH;
        int sidebarX = containerX + 2;
        int sidebarY = bodyY;

        // Draw Sidebar Background & Border right
        graphics.fill(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, PANEL_INNER_BG);
        graphics.fill(sidebarX + sidebarW, sidebarY, sidebarX + sidebarW + 2, sidebarY + sidebarH, COLOR_COPPER);

        // Sidebar title
        graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.branches").getString(), sidebarX + 15, sidebarY + 10, COLOR_BRASS, false);

        // Render sidebar branch items
        int listX = sidebarX + 10;
        int listY = sidebarY + 25;
        int itemW = sidebarW - 20;
        int itemH = 18;

        int addPathBtnY = sidebarY + sidebarH - 25;
        int listH = addPathBtnY - listY - 5;

        double sidebarScale = Minecraft.getInstance().getWindow().getGuiScale();
        int sidebarScissorX = (int) (listX * sidebarScale);
        int sidebarScissorY = (int) ((this.height - (listY + listH)) * sidebarScale);
        int sidebarScissorW = (int) (itemW * sidebarScale);
        int sidebarScissorH = (int) (listH * sidebarScale);

        RenderSystem.enableScissor(sidebarScissorX, sidebarScissorY, sidebarScissorW, sidebarScissorH);

        for (int i = 0; i < model.getPaths().size(); i++) {
            PathInfo p = model.getPaths().get(i);
            int itemY = listY + i * 20 - (int) model.getPathScrollY();

            boolean itemHovered = mouseX >= listX && mouseX < listX + itemW && mouseY >= itemY && mouseY < itemY + itemH && mouseY >= listY && mouseY < listY + listH;
            boolean isActive = (i == model.getSelectedPathIndex());

            int bg = isActive ? 0xFF2C221D : (itemHovered ? 0xFF1C1613 : 0xFF14100E);
            int border = isActive ? COLOR_BRASS : (itemHovered ? COLOR_COPPER : 0xFF332D29);

            drawFlatPanel(graphics, listX, itemY, itemW, itemH, bg, border);

            String name = p.getName();
            int textX = listX + 8;

            net.minecraft.world.item.ItemStack iconStack = PathIcons.getIcon(p);

            if (!iconStack.isEmpty()) {
                graphics.renderFakeItem(iconStack, listX + 2, itemY + 1);
                textX = listX + 20;
            }

            int textMaxW = itemW - (textX - listX) - 4;
            if (this.font.width(name) > textMaxW) {
                name = this.font.plainSubstrByWidth(name, textMaxW - 8) + "..";
            }
            graphics.drawString(this.font, name, textX, itemY + 5, isActive ? COLOR_BRASS : (itemHovered ? TEXT_PRIMARY : TEXT_SECONDARY), false);
        }

        RenderSystem.disableScissor();

        // Render scrollbar if visible paths overflow
        int totalPathsH = model.getPaths().size() * 20;
        if (totalPathsH > listH) {
            int scrollbarX = sidebarX + sidebarW - 6;
            int scrollbarY = listY;
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + listH, 0xFF2A201C);

            float fraction = (float) model.getPathScrollY() / (totalPathsH - listH);
            int thumbH = Math.max(10, (int) (((float) listH / totalPathsH) * listH));
            int thumbY = scrollbarY + (int) (fraction * (listH - thumbH));
            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbH, COLOR_COPPER);
        }

        // Sidebar Add and Delete Buttons (split itemW horizontally)
        addPathBtnY = sidebarY + sidebarH - 25;
        int btnHalfW = itemW / 2 - 2;

        // Button "+ AÑADIR"
        boolean addHovered = mouseX >= listX && mouseX < listX + btnHalfW && mouseY >= addPathBtnY && mouseY < addPathBtnY + 18;
        int addBg = addHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int addBorder = addHovered ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, listX, addPathBtnY, btnHalfW, 18, addBg, addBorder);
        graphics.drawCenteredString(this.font, "+", listX + btnHalfW / 2, addPathBtnY + 5, TEXT_PRIMARY);

        // Button "- BORRAR"
        int delBtnX = listX + btnHalfW + 4;
        boolean delBtnHovered = mouseX >= delBtnX && mouseX < delBtnX + btnHalfW && mouseY >= addPathBtnY && mouseY < addPathBtnY + 18;
        int delBg = delBtnHovered ? 0xFF3A1111 : 0xFF140F0D;
        int delBorder = delBtnHovered ? 0xFFFF5555 : 0xFF2C221D;
        drawFlatPanel(graphics, delBtnX, addPathBtnY, btnHalfW, 18, delBg, delBorder);
        graphics.drawCenteredString(this.font, "✕", delBtnX + btnHalfW / 2, addPathBtnY + 5, delBtnHovered ? TEXT_PRIMARY : TEXT_SECONDARY);

        // --- RIGHT COLUMN (Editor - 75% of body width or more if narrow) ---
        int editorX = layout.editorX;
        int editorW = layout.editorW;
        int editorH = bodyH;

        if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
            PathInfo p = model.getSelectedPath();

            // Enclosing metadata frame
            drawFlatPanel(graphics, editorX + 10, bodyY + 5, editorW - 20, layout.metadataFrameH, PANEL_INNER_BG, 0xFF2A201C);

            // Inputs Labels
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.icon").getString(), layout.iconX, layout.iconY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.branch_title").getString(), layout.titleX, layout.titleY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.namespace_mod").getString(), layout.modX, layout.modY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.branch_dependencies").getString(), layout.depsX, layout.secondY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.switch_rule").getString(), layout.minX, layout.minY - 11, COLOR_BRASS, false);

            // Icon background panel
            int iconW = 20;
            boolean iconHovered = mouseX >= layout.iconX && mouseX < layout.iconX + iconW && mouseY >= layout.iconY && mouseY < layout.iconY + iconW;
            int iconBg = iconHovered ? 0xFF2C221D : INPUT_BACKGROUND;
            int iconBorder = iconHovered ? COLOR_BRASS : COLOR_COPPER;
            drawFlatPanel(graphics, layout.iconX, layout.iconY, iconW, iconW, iconBg, iconBorder);

            // Render current path icon in slot
            net.minecraft.world.item.ItemStack branchIconStack = PathIcons.getIcon(p);
            graphics.renderFakeItem(branchIconStack, layout.iconX + 2, layout.iconY + 2);

            // Inputs Background Panels
            drawFlatPanel(graphics, layout.titleX, layout.titleY, layout.titleW, 20, INPUT_BACKGROUND, COLOR_COPPER);
            drawFlatPanel(graphics, layout.modX, layout.modY, layout.modEditW, 20, INPUT_BACKGROUND, COLOR_COPPER);
            drawFlatPanel(graphics, layout.depsX, layout.secondY, layout.depsW, 20, INPUT_BACKGROUND, COLOR_COPPER);
            // Draw Switch Rule Button instead of edit box
            boolean ruleHovered = mouseX >= layout.minX && mouseX < layout.minX + layout.minW && mouseY >= layout.minY && mouseY < layout.minY + 20;
            int ruleBg = ruleHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int ruleBorder = ruleHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, layout.minX, layout.minY, layout.minW, 20, ruleBg, ruleBorder);

            String ruleText = "";
            if (p.getMinToSwitch() < 0) {
                ruleText = Component.translatable("xam.editor.rule.master").getString();
            } else if (p.getMinToSwitch() == 0) {
                ruleText = Component.translatable("xam.editor.rule.free").getString();
            } else {
                ruleText = Component.translatable("xam.editor.rule.tasks_format", p.getMinToSwitch()).getString();
            }
            graphics.drawCenteredString(this.font, ruleText, layout.minX + layout.minW / 2, layout.minY + 6, TEXT_PRIMARY);

            // Copper "..." dependency picker button (junto al panel de Dependencias de Rama)
            boolean depsBtnHovered = mouseX >= layout.depsBtnX && mouseX < layout.depsBtnX + 20 && mouseY >= layout.secondY && mouseY < layout.secondY + 20;
            int depsBtnBg = depsBtnHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int depsBtnBorder = depsBtnHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, layout.depsBtnX, layout.secondY, 20, 20, depsBtnBg, depsBtnBorder);
            graphics.drawCenteredString(this.font, "...", layout.depsBtnX + 10, layout.secondY + 6, TEXT_PRIMARY);

            // Copper "..." selection button
            boolean browseHovered = mouseX >= layout.browseX && mouseX < layout.browseX + 20 && mouseY >= layout.modY && mouseY < layout.modY + 20;
            int browseBg = browseHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int browseBorder = browseHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, layout.browseX, layout.modY, 20, 20, browseBg, browseBorder);
            graphics.drawCenteredString(this.font, "...", layout.browseX + 10, layout.modY + 6, TEXT_PRIMARY);

            // Requisitos Section
            graphics.drawString(this.font, Component.translatable("xam.screen.mastery_editor.requirements").getString(), editorX + 20, layout.reqTitleY, COLOR_BRASS, false);
            int reqTitleW = this.font.width(Component.translatable("xam.screen.mastery_editor.requirements").getString());
            graphics.fill(editorX + 20, layout.reqTitleY + 10, editorX + 20 + reqTitleW, layout.reqTitleY + 11, COLOR_COPPER);

            // Button "CONFIGURAR PERKS" with Cobre Ponder style
            int perksBtnX = editorX + editorW - 215;
            int perksBtnY = layout.reqTitleY - 4;
            int perksBtnW = 90;
            int perksBtnH = 16;
            boolean perksHovered = mouseX >= perksBtnX && mouseX < perksBtnX + perksBtnW && mouseY >= perksBtnY && mouseY < perksBtnY + perksBtnH;
            int perksBg = perksHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int perksBorder = perksHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, perksBtnX, perksBtnY, perksBtnW, perksBtnH, perksBg, perksBorder);
            graphics.drawCenteredString(this.font, Component.translatable("xam.screen.mastery_editor.perks").getString(), perksBtnX + perksBtnW / 2, perksBtnY + 4, TEXT_PRIMARY);

            // Button "+ AÑADIR TAREA" with Cobre Ponder style
            int addReqBtnX = editorX + editorW - 120;
            int addReqBtnY = layout.reqTitleY - 4;
            int addReqBtnW = 100;
            int addReqBtnH = 16;
            boolean addReqHovered = mouseX >= addReqBtnX && mouseX < addReqBtnX + addReqBtnW && mouseY >= addReqBtnY && mouseY < addReqBtnY + addReqBtnH;
            int addReqBg = addReqHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int addReqBorder = addReqHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, addReqBtnX, addReqBtnY, addReqBtnW, addReqBtnH, addReqBg, addReqBorder);
            graphics.drawCenteredString(this.font, Component.translatable("xam.screen.mastery_editor.add_task").getString(), addReqBtnX + addReqBtnW / 2, addReqBtnY + 4, TEXT_PRIMARY);

            // Requirements Scissor Region & Scrollbar logic
            int startCardY = layout.reqTitleY + 16;
            int reqListH = editorH - (layout.reqTitleY - bodyY + 16) - 10;

            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            int scissorX = (int) ((editorX + 20) * scale);
            int scissorY = (int) ((this.height - (startCardY + reqListH)) * scale);
            int scissorW = (int) ((editorW - 40) * scale);
            int scissorH = (int) (reqListH * scale);

            RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

            int cardW = editorW - 40;
            int cardH = 40;

            for (int j = 0; j < p.getRequirements().size(); j++) {
                Requirement req = p.getRequirements().get(j);
                int cardX = editorX + 20;
                int cardY = startCardY + (j * 46) - (int) model.getScrollY();

                // Render requirement card
                boolean delHovered = mouseX >= cardX + cardW - 40 && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardH;

                drawFlatPanel(graphics, cardX, cardY, cardW, cardH, PANEL_INNER_BG, 0xFF2A201C);

                // 1. Tag Pill & Dynamic Icon
                net.minecraft.world.item.ItemStack renderStack = net.minecraft.world.item.ItemStack.EMPTY;
                int typeBg = 0xFF2C221A;
                int typeBorder = COLOR_COPPER;
                int typeFg = 0xFFFFAA00;
                String typeLabel = Component.translatable("xam.req_type.badge." + req.getType().toLowerCase()).getString();

                if (req.getType().equals("craft")) {
                    typeBg = 0xFF2C221A;
                    typeBorder = COLOR_COPPER;
                    typeFg = 0xFFFFAA00;
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(req.getId()));
                    if (item != null) renderStack = new net.minecraft.world.item.ItemStack(item);
                } else if (req.getType().equals("collect")) {
                    typeBg = 0xFF152615;
                    typeBorder = 0xFF3F8F3F;
                    typeFg = 0xFF55FF55;
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(req.getId()));
                    if (item != null) renderStack = new net.minecraft.world.item.ItemStack(item);
                } else if (req.getType().equals("kill")) {
                    typeBg = 0xFF2A1515;
                    typeBorder = 0xFF9E2A2A;
                    typeFg = 0xFFFF5555;
                    renderStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
                } else if (req.getType().equals("advancement")) {
                    typeBg = 0xFF152A2A;
                    typeBorder = 0xFF2A9E9E;
                    typeFg = 0xFF55FFFF;
                    renderStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                }

                int typeBoxW = 100;

                drawFlatPanel(graphics, cardX + 4, cardY + 4, typeBoxW, cardH - 8, typeBg, typeBorder);
                if (!renderStack.isEmpty()) {
                    graphics.renderFakeItem(renderStack, cardX + 8, cardY + 12);
                    graphics.drawString(this.font, typeLabel, cardX + 28, cardY + 16, typeFg, false);
                } else {
                    graphics.drawCenteredString(this.font, typeLabel, cardX + 4 + typeBoxW / 2, cardY + 16, typeFg);
                }

                // 2. Info (Flex width)
                String nameText = req.getName();
                if (nameText.isEmpty()) nameText = req.getId();
                int infoX = cardX + 4 + typeBoxW + 8;
                int infoMaxW = cardW - 52 - typeBoxW;
                if (this.font.width(nameText) > infoMaxW) {
                    nameText = this.font.plainSubstrByWidth(nameText, infoMaxW - 10) + "...";
                }
                graphics.drawString(this.font, nameText, infoX, cardY + 8, COLOR_BRASS, false);

                String descText = req.getDescription();
                if (descText.isEmpty()) descText = req.getId();
                if (this.font.width(descText) > infoMaxW) {
                    descText = this.font.plainSubstrByWidth(descText, infoMaxW - 10) + "...";
                }
                graphics.drawString(this.font, descText, infoX, cardY + 24, TEXT_SECONDARY, false);

                // 3. Botón Eliminar (40px wide) with Ponder copper/red style
                int cardDelBg = delHovered ? 0xFF3A1111 : 0xFF140F0D;
                int cardDelFg = delHovered ? 0xFFFF5555 : TEXT_MUTED;
                graphics.fill(cardX + cardW - 40, cardY + 2, cardX + cardW - 2, cardY + cardH - 2, cardDelBg);
                graphics.fill(cardX + cardW - 40, cardY + 2, cardX + cardW - 38, cardY + cardH - 2, 0xFF2A201C); // Separator
                graphics.drawCenteredString(this.font, "✕", cardX + cardW - 20, cardY + 16, cardDelFg);
            }

            RenderSystem.disableScissor();

            // Render scrollbar if visible cards overflow with Cobre style
            int totalReqsH = p.getRequirements().size() * 46;
            if (totalReqsH > reqListH) {
                int scrollbarX = editorX + editorW - 15;
                int scrollbarY = startCardY;
                graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + reqListH, 0xFF2A201C);

                float fraction = (float) model.getScrollY() / (totalReqsH - reqListH);
                int thumbH = Math.max(12, (int) (((float) reqListH / totalReqsH) * reqListH));
                int thumbY = scrollbarY + (int) (fraction * (reqListH - thumbH));
                graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbH, COLOR_COPPER);
            }

            // Render unified context menu if active
            if (model.isContextMenuOpen() && !model.getActiveMenuOptions().isEmpty()) {
                int menuW = 80;
                int optionH = 16;
                int menuH = model.getActiveMenuOptions().size() * optionH + 4; // 2px padding top/bottom

                drawFlatPanel(graphics, model.getContextMenuX(), model.getContextMenuY(), menuW, menuH, WIDGET_BACKGROUND, BORDER_INNER);

                for (int o = 0; o < model.getActiveMenuOptions().size(); o++) {
                    MasteryEditorModel.MenuOption opt = model.getActiveMenuOptions().get(o);
                    int optY = model.getContextMenuY() + 2 + o * optionH;
                    boolean optHovered = mouseX >= model.getContextMenuX() && mouseX < model.getContextMenuX() + menuW && mouseY >= optY && mouseY < optY + optionH;

                    if (optHovered) {
                        int hoverBg = opt.isDanger ? 0xFF3A1111 : BUTTON_HOVER_BG;
                        graphics.fill(model.getContextMenuX() + 2, optY, model.getContextMenuX() + menuW - 2, optY + optionH, hoverBg);
                    }

                    int textCol = optHovered ? (opt.isDanger ? 0xFFFF5555 : TEXT_PRIMARY) : TEXT_SECONDARY;
                    graphics.drawCenteredString(this.font, opt.label, model.getContextMenuX() + menuW / 2, optY + (optionH - 8) / 2, textCol);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int sidebarW = layout.sidebarW;
        int editorX = layout.editorX;
        int editorW = layout.editorW;
        int editorH = bodyH;

        if (mouseX < containerX + sidebarW) {
            int listY = bodyY + 25;
            int addPathBtnY = bodyY + bodyH - 25;
            int listH = addPathBtnY - listY - 5;
            int totalPathsH = model.getPaths().size() * 20;
            int maxScroll = Math.max(0, totalPathsH - listH);
            if (maxScroll > 0) {
                model.setPathScrollY(Math.max(0, Math.min(maxScroll, model.getPathScrollY() - delta * 15)));
                return true;
            }
        } else if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size() && mouseX >= editorX + 20) {
            PathInfo p = model.getSelectedPath();
            int reqListH = editorH - (layout.reqTitleY - bodyY + 16) - 10;
            int totalReqsH = p.getRequirements().size() * 46;
            int maxScroll = Math.max(0, totalReqsH - reqListH);

            if (maxScroll > 0) {
                model.setScrollY(Math.max(0, Math.min(maxScroll, model.getScrollY() - delta * 15)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isBackButtonClicked(mouseX, mouseY)) {
            playClickSound();
            if (this.parent != null) {
                this.minecraft.setScreen(this.parent);
            } else {
                this.onClose();
            }
            return true;
        }
        int sidebarW = layout.sidebarW;
        int editorX = layout.editorX;
        int editorW = layout.editorW;
        int editorH = bodyH;

        // 1. Handle Unified Context Menu left click / dismiss
        if (model.isContextMenuOpen()) {
            if (button == 0) {
                int menuW = 80;
                int optionH = 16;
                int menuH = model.getActiveMenuOptions().size() * optionH + 4;
                if (mouseX >= model.getContextMenuX() && mouseX < model.getContextMenuX() + menuW && mouseY >= model.getContextMenuY() && mouseY < model.getContextMenuY() + menuH) {
                    int clickedOptIndex = (int) ((mouseY - model.getContextMenuY() - 2) / optionH);
                    if (clickedOptIndex >= 0 && clickedOptIndex < model.getActiveMenuOptions().size()) {
                        playClickSound();
                        MasteryEditorModel.MenuOption opt = model.getActiveMenuOptions().get(clickedOptIndex);
                        model.closeContextMenu();
                        opt.action.run();
                        return true;
                    }
                }
            }
            model.closeContextMenu();
            if (button != 0) {
                return true; // Consume other click types when dismissing
            }
        }

        // 2. Handle Right Click to open Context Menu on card or sidebar
        if (button == 1) {
            // Right Click on Sidebar Branch Item
            int sidebarX = containerX + 2;
            int listX = sidebarX + 10;
            int listY = bodyY + 25;
            int itemW = sidebarW - 20;
            int itemH = 18;
            int addPathBtnY = bodyY + bodyH - 25;
            int listH = addPathBtnY - listY - 5;
            for (int i = 0; i < model.getPaths().size(); i++) {
                int itemY = listY + i * 20 - (int) model.getPathScrollY();
                if (mouseX >= listX && mouseX < listX + itemW && mouseY >= itemY && mouseY < itemY + itemH && mouseY >= listY && mouseY < listY + listH) {
                    playClickSound();
                    List<MasteryEditorModel.MenuOption> options = new ArrayList<>();
                    int finalI = i;
                    if (i > 0) {
                        options.add(new MasteryEditorModel.MenuOption("▲", () -> {
                            PathInfo path1 = model.getPaths().get(finalI);
                            PathInfo path2 = model.getPaths().get(finalI - 1);
                            model.getPaths().set(finalI - 1, path1);
                            model.getPaths().set(finalI, path2);
                            model.setSelectedPathIndex(finalI - 1);
                            updateEditors();
                        }));
                    }
                    if (i < model.getPaths().size() - 1) {
                        options.add(new MasteryEditorModel.MenuOption("▼", () -> {
                            PathInfo path1 = model.getPaths().get(finalI);
                            PathInfo path2 = model.getPaths().get(finalI + 1);
                            model.getPaths().set(finalI + 1, path1);
                            model.getPaths().set(finalI, path2);
                            model.setSelectedPathIndex(finalI + 1);
                            updateEditors();
                        }));
                    }
                    options.add(new MasteryEditorModel.MenuOption("Borrar", () -> {
                        PathInfo target = model.getPaths().get(finalI);
                        Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(this, () -> {
                            model.getPaths().remove(finalI);
                            if (model.getSelectedPathIndex() >= finalI) {
                                model.setSelectedPathIndex(model.getPaths().isEmpty() ? -1 : 0);
                            }
                            updateEditors();
                        }, target.getName()));
                    }, true));
                    model.openContextMenu(i, true, (int) mouseX, (int) mouseY, options);
                    return true;
                }
            }

            if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
                PathInfo p = model.getSelectedPath();
                int startCardY = layout.reqTitleY + 16;
                int reqListH = editorH - (layout.reqTitleY - bodyY + 16) - 10;
                int cardW = editorW - 40;
                int cardH = 40;

                if (mouseX >= editorX + 20 && mouseX < editorX + editorW - 20 && mouseY >= startCardY && mouseY < startCardY + reqListH) {
                    double clickedY = mouseY + model.getScrollY() - startCardY;
                    int cardIndex = (int) (clickedY / 46);
                    double relativeY = clickedY % 46;

                    if (cardIndex >= 0 && cardIndex < p.getRequirements().size() && relativeY <= cardH) {
                        playClickSound();
                        List<MasteryEditorModel.MenuOption> options = new ArrayList<>();
                        options.add(new MasteryEditorModel.MenuOption("Editar", () -> {
                            Requirement req = p.getRequirements().get(cardIndex);
                            Minecraft.getInstance().setScreen(new RequirementEditScreen(this, p.getId(), req));
                        }));

                        if (cardIndex > 0) {
                            options.add(new MasteryEditorModel.MenuOption("▲", () -> {
                                Requirement req1 = p.getRequirements().get(cardIndex);
                                Requirement req2 = p.getRequirements().get(cardIndex - 1);
                                p.getRequirements().set(cardIndex - 1, req1);
                                p.getRequirements().set(cardIndex, req2);
                                updateEditors();
                            }));
                        }

                        if (cardIndex < p.getRequirements().size() - 1) {
                            options.add(new MasteryEditorModel.MenuOption("▼", () -> {
                                Requirement req1 = p.getRequirements().get(cardIndex);
                                Requirement req2 = p.getRequirements().get(cardIndex + 1);
                                p.getRequirements().set(cardIndex + 1, req1);
                                p.getRequirements().set(cardIndex, req2);
                                updateEditors();
                            }));
                        }
                        model.openContextMenu(cardIndex, false, (int) mouseX, (int) mouseY, options);
                        return true;
                    }
                }
            }
            return false;
        }

        if (button == 0) {
            if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
                PathInfo p = model.getSelectedPath();
                
                // Icon button click
                int iconW = 20;
                if (mouseX >= layout.iconX && mouseX < layout.iconX + iconW && mouseY >= layout.iconY && mouseY < layout.iconY + iconW) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(new IconSelectionScreen(this, item -> {
                        net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                        if (rl != null) {
                            p.setIcon(rl.toString());
                        }
                        Minecraft.getInstance().setScreen(this);
                    }));
                    return true;
                }

                // Mod ID "..." button click
                if (mouseX >= layout.browseX && mouseX < layout.browseX + 20 && mouseY >= layout.modY && mouseY < layout.modY + 20) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(new ModSelectionScreen(this, modId -> {
                        this.pathModIdEdit.setValue(modId);
                        p.setModId(modId);
                        Minecraft.getInstance().setScreen(this);
                    }));
                    return true;
                }

                // Dependencias "..." picker button click
                if (mouseX >= layout.depsBtnX && mouseX < layout.depsBtnX + 20 && mouseY >= layout.secondY && mouseY < layout.secondY + 20) {
                    playClickSound();
                    this.pathDepsEdit.setResponder(null);
                    Minecraft.getInstance().setScreen(new DependencySelectionScreen(
                            this, p.getId(), model.getPaths(), new ArrayList<>(p.getDependencies()), deps -> {
                        p.clearDependencies();
                        p.getDependencies().addAll(deps);
                        pathDepsEdit.setValue(String.join(", ", deps));
                        pathDepsEdit.setResponder(val -> {
                            p.clearDependencies();
                            if (!val.trim().isEmpty()) {
                                for (String dep : val.split(",")) p.addDependency(dep.trim());
                            }
                        });
                        Minecraft.getInstance().setScreen(this);
                    }));
                    return true;
                }

                // Rule Button click
                if (mouseX >= layout.minX && mouseX < layout.minX + layout.minW && mouseY >= layout.minY && mouseY < layout.minY + 20) {
                    playClickSound();
                    cycleMinToSwitch(p);
                    return true;
                }
            }



            // Footer buttons (Descartar Todo, Guardar Estructura)
            int footBtnW = 120;
            int footBtnH = 20;
            int footStartX = containerX + containerW - 15 - (footBtnW * 2 + 10);
            int footBtnY = containerY + containerH - footerH + (footerH - footBtnH) / 2;

            if (mouseX >= footStartX && mouseX < footStartX + footBtnW && mouseY >= footBtnY && mouseY < footBtnY + footBtnH) {
                playClickSound();
                this.onClose();
                return true;
            }
            if (mouseX >= footStartX + footBtnW + 10 && mouseX < footStartX + footBtnW + 10 + footBtnW && mouseY >= footBtnY && mouseY < footBtnY + footBtnH) {
                playClickSound();
                saveConfig();
                return true;
            }

            // Sidebar: Add / Delete Branch Buttons click
            int sidebarX = containerX + 2;
            int listX = sidebarX + 10;
            int listY = bodyY + 25;
            int itemW = sidebarW - 20;
            int addPathBtnY = bodyY + bodyH - 25;
            int btnHalfW = itemW / 2 - 2;

            // Click "+ AÑADIR"
            if (mouseX >= listX && mouseX < listX + btnHalfW && mouseY >= addPathBtnY && mouseY < addPathBtnY + 18) {
                playClickSound();
                addPath();
                return true;
            }

            // Click "- BORRAR"
            int delBtnX = listX + btnHalfW + 4;
            if (mouseX >= delBtnX && mouseX < delBtnX + btnHalfW && mouseY >= addPathBtnY && mouseY < addPathBtnY + 18) {
                playClickSound();
                Minecraft.getInstance().setScreen(new DeleteMasteryScreen(this));
                return true;
            }

            // Sidebar: select branch
            int listH = addPathBtnY - listY - 5;
            for (int i = 0; i < model.getPaths().size(); i++) {
                int itemY = listY + i * 20 - (int) model.getPathScrollY();
                if (mouseX >= listX && mouseX < listX + itemW && mouseY >= itemY && mouseY < itemY + 18 && mouseY >= listY && mouseY < listY + listH) {
                    playClickSound();
                    model.setSelectedPathIndex(i);
                    model.setScrollY(0);
                    updateEditors();
                    return true;
                }
            }

            // Right side editor clicks
            if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
                PathInfo p = model.getSelectedPath();

                // Perks Button Click
                int perksBtnX = editorX + editorW - 215;
                int perksBtnY = layout.reqTitleY - 4;
                int perksBtnW = 90;
                int perksBtnH = 16;
                if (mouseX >= perksBtnX && mouseX < perksBtnX + perksBtnW && mouseY >= perksBtnY && mouseY < perksBtnY + perksBtnH) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(new PerksConfigScreen(this, p));
                    return true;
                }

                // Add Requirement Button
                int addReqBtnX = editorX + editorW - 120;
                int addReqBtnY = layout.reqTitleY - 4;
                int addReqBtnW = 100;
                int addReqBtnH = 16;
                if (mouseX >= addReqBtnX && mouseX < addReqBtnX + addReqBtnW && mouseY >= addReqBtnY && mouseY < addReqBtnY + addReqBtnH) {
                    playClickSound();
                    addRequirement();
                    return true;
                }

                int startCardY = layout.reqTitleY + 16;
                int reqListH = editorH - (layout.reqTitleY - bodyY + 16) - 10;
                int cardW = editorW - 40;
                int cardH = 40;

                if (mouseX >= editorX + 20 && mouseX < editorX + editorW - 20 && mouseY >= startCardY && mouseY < startCardY + reqListH) {
                    double clickedY = mouseY + model.getScrollY() - startCardY;
                    int cardIndex = (int) (clickedY / 46);
                    double relativeY = clickedY % 46;

                    if (cardIndex >= 0 && cardIndex < p.getRequirements().size() && relativeY <= cardH) {
                        Requirement req = p.getRequirements().get(cardIndex);
                        int cardX = editorX + 20;

                        if (mouseX >= cardX && mouseX < cardX + 80) {
                            playClickSound();
                            cycleRequirementType(req);
                            return true;
                        }
                        if (mouseX >= cardX + 80 && mouseX < cardX + cardW - 40) {
                            playClickSound();
                            openSelectorForRequirement(req);
                            return true;
                        }
                        if (mouseX >= cardX + cardW - 40 && mouseX < cardX + cardW) {
                            playClickSound();
                            String taskName = req.getName().isEmpty() ? req.getId() : req.getName();
                            Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(this, () -> {
                                p.removeRequirementAtIndex(cardIndex);
                                updateModIdFromRequirements(p);
                                updateEditors();
                                int totalReqsH1 = p.getRequirements().size() * 46;
                                int maxScroll1 = Math.max(0, totalReqsH1 - reqListH);
                                model.setScrollY(Math.max(0, Math.min(maxScroll1, model.getScrollY())));
                            }, taskName));
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String getUniqueBranchName() {
        String baseName = Component.translatable("xam.editor.default.new_branch_name").getString();
        boolean exists = false;
        for (PathInfo path : model.getPaths()) {
            if (path.getName().equalsIgnoreCase(baseName)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            return baseName;
        }
        int i = 1;
        while (true) {
            String candidate = baseName + " (" + i + ")";
            boolean candExists = false;
            for (PathInfo path : model.getPaths()) {
                if (path.getName().equalsIgnoreCase(candidate)) {
                    candExists = true;
                    break;
                }
            }
            if (!candExists) {
                return candidate;
            }
            i++;
        }
    }

    private void addPath() {
        PathInfo p = new PathInfo();
        String uniqueName = getUniqueBranchName();
        p.setName(uniqueName);
        String generatedId = uniqueName.toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("__+", "_").replaceAll("^_+|_+$", "");
        if (generatedId.isEmpty()) {
            generatedId = "path_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        p.setId(generatedId);
        p.setModId("modid");
        p.setIcon("minecraft:writable_book");
        p.setRequirements(new ArrayList<>());
        model.addPath(p);
        model.setScrollY(0);
        updateEditors();
    }

    private void addRequirement() {
        if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
            PathInfo p = model.getSelectedPath();
            Requirement req = new Requirement("craft", "", "", "");
            // ponytail: don't add to list yet — RequirementEditScreen will add on commit
            Minecraft.getInstance().setScreen(new RequirementEditScreen(this, p.getId(), req, () -> {
                p.addRequirement(req);
                updateModIdFromRequirements(p);
                updateEditors();
                // Scroll to show newly added item
                int editorH = bodyH;
                int reqListH = editorH - (layout.reqTitleY - bodyY + 16) - 10;
                int totalReqsH = p.getRequirements().size() * 46;
                model.setScrollY(Math.max(0, totalReqsH - reqListH));
            }));
        }
    }

    private void cycleRequirementType(Requirement req) {
        String currentId = req.getId();
        String currentName = req.getName();

        if (req.getType().equals("craft")) {
            req.setType("collect");
            if (isItem(currentId)) {
                req.setDescription("Recoge " + currentName);
            } else {
                req.setId("minecraft:dirt");
                req.setName("Recoger Tierra");
                req.setDescription("Recoge un bloque de tierra");
            }
        } else if (req.getType().equals("collect")) {
            req.setType("kill");
            req.setId("minecraft:zombie");
            req.setName("Zombie");
            req.setDescription("Derrota a Zombie");
        } else if (req.getType().equals("kill")) {
            req.setType("advancement");
            req.setId("minecraft:story/root");
            req.setName("Minecraft");
            req.setDescription("Completa el logro Minecraft");
        } else {
            req.setType("craft");
            if (isItem(currentId)) {
                req.setDescription("Craftea " + currentName);
            } else {
                req.setId("minecraft:dirt");
                req.setName("Craftear Tierra");
                req.setDescription("Craftea un bloque de tierra");
            }
        }
        if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
            updateModIdFromRequirements(model.getSelectedPath());
        }
    }

    private boolean isItem(String id) {
        if (id == null || id.isEmpty()) return false;
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(net.minecraft.resources.ResourceLocation.tryParse(id));
    }

    private void cycleMinToSwitch(PathInfo p) {
        int max = p.getRequirements().size();
        if (p.getMinToSwitch() < 0) {
            p.setMinToSwitch(0);
        } else if (p.getMinToSwitch() >= max) {
            p.setMinToSwitch(-1);
        } else {
            p.setMinToSwitch(p.getMinToSwitch() + 1);
        }
    }

    private void openSelectorForRequirement(Requirement req) {
        Minecraft mc = Minecraft.getInstance();
        if (req.getType().equals("advancement")) {
            mc.setScreen(new AdvancementSelectionScreen(this, adv -> {
                req.setId(adv.getId().toString());
                String titleText = adv.getDisplay() != null ? adv.getDisplay().getTitle().getString() : adv.getId().getPath();
                if (titleText.contains("/")) {
                    String[] split = titleText.split("/");
                    titleText = split[split.length - 1];
                }
                titleText = titleText.replace("_", " ");
                if (!titleText.isEmpty()) {
                    titleText = Character.toUpperCase(titleText.charAt(0)) + titleText.substring(1);
                }
                req.setName(titleText);
                req.setDescription(adv.getDisplay() != null ? adv.getDisplay().getDescription().getString() : Component.translatable("xam.editor.desc.advancement", titleText).getString());
                if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
                    updateModIdFromRequirements(model.getSelectedPath());
                }
                mc.setScreen(this);
            }));
        } else if (req.getType().equals("craft") || req.getType().equals("collect")) {
            mc.setScreen(new ItemSelectionScreen(this, item -> {
                net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                if (rl != null) {
                    req.setId(rl.toString());
                    String friendlyName = item.getDescription().getString();
                    req.setName(friendlyName);
                    if (req.getType().equals("craft")) {
                        req.setDescription(Component.translatable("xam.editor.desc.craft", friendlyName).getString());
                    } else {
                        req.setDescription(Component.translatable("xam.editor.desc.collect", friendlyName).getString());
                    }
                }
                if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
                    updateModIdFromRequirements(model.getSelectedPath());
                }
                mc.setScreen(this);
            }));
        } else if (req.getType().equals("kill")) {
            mc.setScreen(new EntitySelectionScreen(this, type -> {
                net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
                if (rl != null) {
                    req.setId(rl.toString());
                    String friendlyName = type.getDescription().getString();
                    req.setName(friendlyName);
                    req.setDescription(Component.translatable("xam.editor.desc.kill", friendlyName).getString());
                }
                if (model.getSelectedPathIndex() >= 0 && model.getSelectedPathIndex() < model.getPaths().size()) {
                    updateModIdFromRequirements(model.getSelectedPath());
                }
                mc.setScreen(this);
            }));
        }
    }

    private void saveConfig() {
        MasteryEditorValidator.ValidationResult result = validator.validateAll(model.getPaths());
        if (!result.ok) {
            showError(result.errorMessage);
            return;
        }

        String json = ConfigManager.serializePaths(model.getPaths());
        XamNetwork.CHANNEL.sendToServer(new UpdateConfigPacket(json));

        model.showNotification(Component.translatable("xam.screen.mastery_editor.save_success").getString(), false);
    }

    private void showError(String msg) {
        model.showNotification(msg, true);
    }


}
