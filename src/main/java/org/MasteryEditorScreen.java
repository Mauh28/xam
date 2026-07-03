package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.xdAbsoluteMastery.ConfigManager.PathInfo;
import org.xdAbsoluteMastery.ConfigManager.Requirement;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;

public class MasteryEditorScreen extends AbstractMasteryScreen {
    private final Screen parent;
    private final List<PathInfo> localPaths = new ArrayList<>();
    private int selectedPathIndex = -1;

    // Scroll state for requirements list
    private double scrollY = 0;

    // Unified Context menu state
    private int contextMenuIndex = -1;
    private boolean contextMenuIsBranch = false;
    private int contextMenuX = 0;
    private int contextMenuY = 0;

    private static class MenuOption {
        final String label;
        final Runnable action;
        final boolean isDanger;

        MenuOption(String label, Runnable action) {
            this(label, action, false);
        }

        MenuOption(String label, Runnable action, boolean isDanger) {
            this.label = label;
            this.action = action;
            this.isDanger = isDanger;
        }
    }
    private final List<MenuOption> activeMenuOptions = new ArrayList<>();

    // Custom text input boxes
    private EditBox pathNameEdit;
    private EditBox pathModIdEdit;
    private EditBox pathDepsEdit;

    // Layout fields
    private boolean isNarrow;
    private int iconX, iconY;
    private int titleX, titleY, titleW;
    private int modX, modY, modEditW, browseX;
    private int depsX, secondY, depsW, depsBtnX;
    private int minX, minY, minW;
    private int metadataFrameH;
    private int reqTitleY;

    // Notification state
    private long saveNotificationTime = 0;
    private String saveNotificationMsg = "";

    public MasteryEditorScreen(Screen parent) {
        super(Component.literal("EDITOR DE MAESTRÍAS"));
        this.parent = parent;

        for (PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
            PathInfo p = new PathInfo();
            p.id = path.id;
            p.name = path.name;
            p.mod_id = path.mod_id;
            p.icon = path.icon;
            p.min_to_switch = path.min_to_switch;
            p.perkEffect = path.perkEffect != null ? path.perkEffect : "";
            p.perkAmplifier = path.perkAmplifier;
            p.dependencies = new ArrayList<>(path.dependencies);
            p.requirements = new ArrayList<>();
            for (Requirement req : path.requirements) {
                Requirement r = new Requirement();
                r.type = req.type;
                r.id = req.id;
                r.name = req.name;
                r.description = req.description;
                r.dependencies = new ArrayList<>(req.dependencies);
                p.requirements.add(r);
            }
            this.localPaths.add(p);
        }

        if (!this.localPaths.isEmpty()) {
            this.selectedPathIndex = 0;
        }
    }

    @Override
    protected void init() {
        super.init();

        int sidebarW = containerW < 450 ? 95 : (int) (containerW * 0.25);
        int editorX = containerX + sidebarW + 2;
        int editorW = containerW - sidebarW - 4;
        int editorH = bodyH;

        this.isNarrow = editorW < 320;

        int iconW = 20;
        if (!isNarrow) {
            this.iconX = editorX + 20;
            this.iconY = bodyY + 15;

            this.titleX = iconX + iconW + 10;
            this.titleW = (int) ((editorW - 80) * 0.60);
            this.titleY = bodyY + 15;

            int modW = (int) ((editorW - 80) * 0.40);
            this.modX = titleX + titleW + 10;
            this.modEditW = modW - 25;
            this.modY = bodyY + 15;
            this.browseX = modX + modEditW + 5;

            // Increased vertical spacing to prevent vertical overlaps with upper fields
            this.secondY = bodyY + 52;
            int secondW = editorW - 40;
            this.minW = 120;
            int depsAreaW = secondW - minW - 10;
            this.depsW = depsAreaW - 25;
            this.depsX = editorX + 20;
            this.depsBtnX = depsX + depsW + 5;
            this.minX = depsBtnX + 20 + 10;
            this.minY = secondY;

            this.metadataFrameH = 80;
        } else {
            // Narrow stacked layout with comfortable row gaps
            this.iconX = editorX + 15;
            this.iconY = bodyY + 12;

            this.titleX = iconX + iconW + 10;
            this.titleW = editorW - 60;
            this.titleY = bodyY + 12;

            this.modX = editorX + 15;
            this.modEditW = editorW - 55;
            this.modY = bodyY + 47;
            this.browseX = modX + modEditW + 5;

            this.depsX = editorX + 15;
            this.depsW = editorW - 55;
            this.secondY = bodyY + 82;
            this.depsBtnX = depsX + depsW + 5;

            this.minX = editorX + 15;
            this.minW = editorW - 30;
            this.minY = bodyY + 117;

            this.metadataFrameH = 145;
        }

        this.reqTitleY = bodyY + 10 + metadataFrameH + 10;

        // Edit box for Title
        this.pathNameEdit = new EditBox(this.font, titleX + 4, titleY + 5, titleW - 8, 12, Component.literal("Título"));
        this.pathNameEdit.setBordered(false);
        this.pathNameEdit.setTextColor(TEXT_PRIMARY);
        this.addRenderableWidget(this.pathNameEdit);

        // Edit box for Mod ID (Editable)
        this.pathModIdEdit = new EditBox(this.font, modX + 4, modY + 5, modEditW - 8, 12, Component.literal("Namespace MOD"));
        this.pathModIdEdit.setBordered(false);
        this.pathModIdEdit.setTextColor(TEXT_PRIMARY);
        this.pathModIdEdit.setEditable(true);
        this.addRenderableWidget(this.pathModIdEdit);

        // Second line editor inputs
        this.pathDepsEdit = new EditBox(this.font, depsX + 4, secondY + 5, depsW - 8, 12, Component.literal("Dependencias"));
        this.pathDepsEdit.setBordered(false);
        this.pathDepsEdit.setTextColor(TEXT_PRIMARY);
        this.addRenderableWidget(this.pathDepsEdit);

        updateEditors();
        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
            updateModIdFromRequirements(localPaths.get(selectedPathIndex));
        }
    }

    private void updateEditors() {
        boolean pathSelected = selectedPathIndex >= 0 && selectedPathIndex < localPaths.size();
        pathNameEdit.visible = pathSelected;
        pathModIdEdit.visible = pathSelected;
        pathDepsEdit.visible = pathSelected;

        if (pathSelected) {
            PathInfo p = localPaths.get(selectedPathIndex);
            pathNameEdit.setResponder(null);
            pathNameEdit.setValue(p.name);
            pathNameEdit.setResponder(val -> {
                p.name = val;
                p.id = val.toLowerCase().replaceAll("[^a-z0-9_]", "_");
            });

            pathModIdEdit.setResponder(null);
            pathModIdEdit.setValue(p.mod_id);
            pathModIdEdit.setResponder(val -> p.mod_id = val);

            pathDepsEdit.setResponder(null);
            pathDepsEdit.setValue(String.join(", ", p.dependencies));
            pathDepsEdit.setResponder(val -> {
                p.dependencies.clear();
                if (!val.trim().isEmpty()) {
                    for (String dep : val.split(",")) {
                        p.dependencies.add(dep.trim());
                    }
                }
            });
        }
    }

    private void updateModIdFromRequirements(PathInfo p) {
        if (p.requirements.isEmpty()) return;
        for (Requirement r : p.requirements) {
            if (r.id != null && r.id.contains(":")) {
                String ns = r.id.split(":")[0];
                if (!ns.equals("minecraft")) {
                    p.mod_id = ns;
                    if (pathModIdEdit != null) {
                        pathModIdEdit.setValue(ns);
                    }
                    return;
                }
            }
        }
        String firstId = p.requirements.get(0).id;
        if (firstId != null && firstId.contains(":")) {
            String ns = firstId.split(":")[0];
            p.mod_id = ns;
            if (pathModIdEdit != null) {
                pathModIdEdit.setValue(ns);
            }
        }
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, "EDITOR DE MAESTRÍAS", containerX + 15, titleY, COLOR_BRASS, false);
        drawBackButton(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnW = 120;
        int btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        // Render Save Notification if active
        if (System.currentTimeMillis() - saveNotificationTime < 3000) {
            int msgY = containerY + containerH - footerH + (footerH - 8) / 2;
            int notifColor = saveNotificationMsg.startsWith("✕") ? 0xFFFF5555 : 0xFF55FF55;
            graphics.drawString(this.font, saveNotificationMsg, containerX + 15, msgY, notifColor, false);
        }

        // Custom Prestige toggle checkbox
        int prestX = containerX + 140;
        int prestY = containerY + containerH - footerH + (footerH - 12) / 2;
        boolean prestHovered = mouseX >= prestX && mouseX < prestX + 110 && mouseY >= prestY && mouseY < prestY + 12;
        drawFlatPanel(graphics, prestX, prestY, 12, 12, xdAbsoluteMastery.ConfigManager.prestigeModeEnabled ? 0xFF2A593E : 0xFF14100E, prestHovered ? COLOR_BRASS : COLOR_COPPER);
        if (xdAbsoluteMastery.ConfigManager.prestigeModeEnabled) {
            graphics.drawString(this.font, "✔", prestX + 2, prestY + 2, 0xFF55FF55, false);
        }
        graphics.drawString(this.font, "Modo Prestigio", prestX + 18, prestY + 2, prestHovered ? TEXT_PRIMARY : TEXT_SECONDARY, false);

        // Custom Discard button (dark red/grey)
        boolean discHovered = mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int discBg = discHovered ? 0xFF2A1C1A : 0xFF1C1312;
        int discBorder = discHovered ? 0xFFFF5555 : 0xFF3E2D2B;
        drawFlatPanel(graphics, startX, btnY, btnW, btnH, discBg, discBorder);
        graphics.drawCenteredString(this.font, "Descartar Todo", startX + btnW / 2, btnY + 6, discHovered ? TEXT_PRIMARY : TEXT_SECONDARY);

        // Custom Save button (copper/brass)
        int saveX = startX + btnW + 10;
        boolean saveHovered = mouseX >= saveX && mouseX < saveX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int saveBg = saveHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int saveBorder = saveHovered ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, saveX, btnY, btnW, btnH, saveBg, saveBorder);
        graphics.drawCenteredString(this.font, "Guardar Estructura", saveX + btnW / 2, btnY + 6, TEXT_PRIMARY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Sidebar dimensions (25% of body width or fixed width if narrow)
        int sidebarW = containerW < 450 ? 95 : (int) (containerW * 0.25);
        int sidebarH = bodyH;
        int sidebarX = containerX + 2;
        int sidebarY = bodyY;

        // Draw Sidebar Background & Border right
        graphics.fill(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, PANEL_INNER_BG);
        graphics.fill(sidebarX + sidebarW, sidebarY, sidebarX + sidebarW + 2, sidebarY + sidebarH, COLOR_COPPER);

        // Sidebar title
        graphics.drawString(this.font, "RAMAS", sidebarX + 15, sidebarY + 10, COLOR_BRASS, false);

        // Render sidebar branch items
        int listX = sidebarX + 10;
        int listY = sidebarY + 25;
        int itemW = sidebarW - 20;
        int itemH = 18;

        for (int i = 0; i < localPaths.size(); i++) {
            PathInfo p = localPaths.get(i);
            int itemY = listY + i * 20;

            boolean itemHovered = mouseX >= listX && mouseX < listX + itemW && mouseY >= itemY && mouseY < itemY + itemH;
            boolean isActive = (i == selectedPathIndex);

            int bg = isActive ? 0xFF2C221D : (itemHovered ? 0xFF1C1613 : 0xFF14100E);
            int border = isActive ? COLOR_BRASS : (itemHovered ? COLOR_COPPER : 0xFF332D29);

            drawFlatPanel(graphics, listX, itemY, itemW, itemH, bg, border);

            String name = p.name;
            int textX = listX + 8;

            net.minecraft.world.item.ItemStack iconStack = net.minecraft.world.item.ItemStack.EMPTY;
            if (p.icon != null) {
                net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(p.icon));
                if (item != null) {
                    iconStack = new net.minecraft.world.item.ItemStack(item);
                }
            }
            if (iconStack.isEmpty()) {
                if (p.id.equals("botania")) {
                    iconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POPPY);
                } else if (p.id.equals("mekanism")) {
                    iconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE);
                } else {
                    iconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                }
            }

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

        // Sidebar Add and Delete Buttons (split itemW horizontally)
        int addPathBtnY = sidebarY + sidebarH - 25;
        int btnHalfW = itemW / 2 - 2;

        // Button "+ AÑADIR"
        boolean addHovered = mouseX >= listX && mouseX < listX + btnHalfW && mouseY >= addPathBtnY && mouseY < addPathBtnY + 18;
        int addBg = addHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int addBorder = addHovered ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, listX, addPathBtnY, btnHalfW, 18, addBg, addBorder);
        graphics.drawCenteredString(this.font, "+ AÑADIR", listX + btnHalfW / 2, addPathBtnY + 5, TEXT_PRIMARY);

        // Button "- BORRAR"
        int delBtnX = listX + btnHalfW + 4;
        boolean delBtnHovered = mouseX >= delBtnX && mouseX < delBtnX + btnHalfW && mouseY >= addPathBtnY && mouseY < addPathBtnY + 18;
        int delBg = delBtnHovered ? 0xFF3A1111 : 0xFF140F0D;
        int delBorder = delBtnHovered ? 0xFFFF5555 : 0xFF2C221D;
        drawFlatPanel(graphics, delBtnX, addPathBtnY, btnHalfW, 18, delBg, delBorder);
        graphics.drawCenteredString(this.font, "- BORRAR", delBtnX + btnHalfW / 2, addPathBtnY + 5, delBtnHovered ? TEXT_PRIMARY : TEXT_SECONDARY);

        // --- RIGHT COLUMN (Editor - 75% of body width or more if narrow) ---
        int editorX = containerX + sidebarW + 2;
        int editorW = containerW - sidebarW - 4;
        int editorH = bodyH;

        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
            PathInfo p = localPaths.get(selectedPathIndex);

            // Enclosing metadata frame
            drawFlatPanel(graphics, editorX + 10, bodyY + 5, editorW - 20, metadataFrameH, PANEL_INNER_BG, 0xFF2A201C);

            // Inputs Labels
            graphics.drawString(this.font, "Icono", iconX, iconY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, "Título", titleX, titleY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, "Namespace MOD", modX, modY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, "Dependencias de Rama (ej. botania:1)", depsX, secondY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, "Regla de Cambio", minX, minY - 11, COLOR_BRASS, false);

            // Icon background panel
            int iconW = 20;
            boolean iconHovered = mouseX >= iconX && mouseX < iconX + iconW && mouseY >= iconY && mouseY < iconY + iconW;
            int iconBg = iconHovered ? 0xFF2C221D : INPUT_BACKGROUND;
            int iconBorder = iconHovered ? COLOR_BRASS : COLOR_COPPER;
            drawFlatPanel(graphics, iconX, iconY, iconW, iconW, iconBg, iconBorder);

            // Render current path icon in slot
            net.minecraft.world.item.ItemStack branchIconStack = net.minecraft.world.item.ItemStack.EMPTY;
            if (p.icon != null) {
                net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(p.icon));
                if (item != null) {
                    branchIconStack = new net.minecraft.world.item.ItemStack(item);
                }
            }
            if (branchIconStack.isEmpty()) {
                if (p.id.equals("botania")) {
                    branchIconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POPPY);
                } else if (p.id.equals("mekanism")) {
                    branchIconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REDSTONE);
                } else {
                    branchIconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                }
            }
            graphics.renderFakeItem(branchIconStack, iconX + 2, iconY + 2);

            // Inputs Background Panels
            drawFlatPanel(graphics, titleX, titleY, titleW, 20, INPUT_BACKGROUND, COLOR_COPPER);
            drawFlatPanel(graphics, modX, modY, modEditW, 20, INPUT_BACKGROUND, COLOR_COPPER);
            drawFlatPanel(graphics, depsX, secondY, depsW, 20, INPUT_BACKGROUND, COLOR_COPPER);
            // Draw Switch Rule Button instead of edit box
            boolean ruleHovered = mouseX >= minX && mouseX < minX + minW && mouseY >= minY && mouseY < minY + 20;
            int ruleBg = ruleHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int ruleBorder = ruleHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, minX, minY, minW, 20, ruleBg, ruleBorder);

            String ruleText = "";
            if (p.min_to_switch < 0) {
                ruleText = "Dominar completa";
            } else if (p.min_to_switch == 0) {
                ruleText = "Cambio libre";
            } else {
                ruleText = p.min_to_switch + " tarea" + (p.min_to_switch > 1 ? "s" : "");
            }
            graphics.drawCenteredString(this.font, ruleText, minX + minW / 2, minY + 6, TEXT_PRIMARY);

            // Copper "..." dependency picker button (junto al panel de Dependencias de Rama)
            boolean depsBtnHovered = mouseX >= depsBtnX && mouseX < depsBtnX + 20 && mouseY >= secondY && mouseY < secondY + 20;
            int depsBtnBg = depsBtnHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int depsBtnBorder = depsBtnHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, depsBtnX, secondY, 20, 20, depsBtnBg, depsBtnBorder);
            graphics.drawCenteredString(this.font, "...", depsBtnX + 10, secondY + 6, TEXT_PRIMARY);

            // Copper "..." selection button
            boolean browseHovered = mouseX >= browseX && mouseX < browseX + 20 && mouseY >= modY && mouseY < modY + 20;
            int browseBg = browseHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int browseBorder = browseHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, browseX, modY, 20, 20, browseBg, browseBorder);
            graphics.drawCenteredString(this.font, "...", browseX + 10, modY + 6, TEXT_PRIMARY);

            // Requisitos Section
            graphics.drawString(this.font, "REQUISITOS", editorX + 20, reqTitleY, COLOR_BRASS, false);
            int reqTitleW = this.font.width("REQUISITOS");
            graphics.fill(editorX + 20, reqTitleY + 10, editorX + 20 + reqTitleW, reqTitleY + 11, COLOR_COPPER);

            // Button "CONFIGURAR PERKS" with Cobre Ponder style
            int perksBtnX = editorX + editorW - 215;
            int perksBtnY = reqTitleY - 4;
            int perksBtnW = 90;
            int perksBtnH = 16;
            boolean perksHovered = mouseX >= perksBtnX && mouseX < perksBtnX + perksBtnW && mouseY >= perksBtnY && mouseY < perksBtnY + perksBtnH;
            int perksBg = perksHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int perksBorder = perksHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, perksBtnX, perksBtnY, perksBtnW, perksBtnH, perksBg, perksBorder);
            graphics.drawCenteredString(this.font, "Perks", perksBtnX + perksBtnW / 2, perksBtnY + 4, TEXT_PRIMARY);

            // Button "+ AÑADIR TAREA" with Cobre Ponder style
            int addReqBtnX = editorX + editorW - 120;
            int addReqBtnY = reqTitleY - 4;
            int addReqBtnW = 100;
            int addReqBtnH = 16;
            boolean addReqHovered = mouseX >= addReqBtnX && mouseX < addReqBtnX + addReqBtnW && mouseY >= addReqBtnY && mouseY < addReqBtnY + addReqBtnH;
            int addReqBg = addReqHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int addReqBorder = addReqHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, addReqBtnX, addReqBtnY, addReqBtnW, addReqBtnH, addReqBg, addReqBorder);
            graphics.drawCenteredString(this.font, "+ AÑADIR TAREA", addReqBtnX + addReqBtnW / 2, addReqBtnY + 4, TEXT_PRIMARY);

            // Requirements Scissor Region & Scrollbar logic
            int startCardY = reqTitleY + 16;
            int reqListH = editorH - (reqTitleY - bodyY + 16) - 10;

            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            int scissorX = (int) ((editorX + 20) * scale);
            int scissorY = (int) ((this.height - (startCardY + reqListH)) * scale);
            int scissorW = (int) ((editorW - 40) * scale);
            int scissorH = (int) (reqListH * scale);

            RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

            int cardW = editorW - 40;
            int cardH = 40;

            for (int j = 0; j < p.requirements.size(); j++) {
                Requirement req = p.requirements.get(j);
                int cardX = editorX + 20;
                int cardY = startCardY + (j * 46) - (int) scrollY;

                // Render requirement card
                boolean delHovered = mouseX >= cardX + cardW - 40 && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + cardH;

                drawFlatPanel(graphics, cardX, cardY, cardW, cardH, PANEL_INNER_BG, 0xFF2A201C);

                // 1. Tag Pill & Dynamic Icon
                net.minecraft.world.item.ItemStack renderStack = net.minecraft.world.item.ItemStack.EMPTY;
                int typeBg = 0xFF2C221A;
                int typeBorder = COLOR_COPPER;
                int typeFg = 0xFFFFAA00;
                String typeLabel = "CRAFT";

                if (req.type.equals("craft")) {
                    typeBg = 0xFF2C221A;
                    typeBorder = COLOR_COPPER;
                    typeFg = 0xFFFFAA00;
                    typeLabel = "CRAFT";
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(req.id));
                    if (item != null) renderStack = new net.minecraft.world.item.ItemStack(item);
                } else if (req.type.equals("collect")) {
                    typeBg = 0xFF152615;
                    typeBorder = 0xFF3F8F3F;
                    typeFg = 0xFF55FF55;
                    typeLabel = "COLLECT";
                    net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(req.id));
                    if (item != null) renderStack = new net.minecraft.world.item.ItemStack(item);
                } else if (req.type.equals("kill")) {
                    typeBg = 0xFF2A1515;
                    typeBorder = 0xFF9E2A2A;
                    typeFg = 0xFFFF5555;
                    typeLabel = "KILL";
                    renderStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
                } else if (req.type.equals("advancement")) {
                    typeBg = 0xFF152A2A;
                    typeBorder = 0xFF2A9E9E;
                    typeFg = 0xFF55FFFF;
                    typeLabel = "LOGRO";
                    renderStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                }

                drawFlatPanel(graphics, cardX + 4, cardY + 4, 76, cardH - 8, typeBg, typeBorder);
                if (!renderStack.isEmpty()) {
                    graphics.renderFakeItem(renderStack, cardX + 8, cardY + 12);
                    graphics.drawString(this.font, typeLabel, cardX + 28, cardY + 16, typeFg, false);
                } else {
                    graphics.drawCenteredString(this.font, typeLabel, cardX + 42, cardY + 16, typeFg);
                }

                // 2. Info (Flex width: cardW - 120)
                String nameText = req.name;
                if (nameText.isEmpty()) nameText = req.id;
                int infoMaxW = cardW - 130;
                if (this.font.width(nameText) > infoMaxW) {
                    nameText = this.font.plainSubstrByWidth(nameText, infoMaxW - 10) + "...";
                }
                graphics.drawString(this.font, nameText, cardX + 86, cardY + 8, COLOR_BRASS, false);

                String descText = req.description;
                if (descText.isEmpty()) descText = req.id;
                if (this.font.width(descText) > infoMaxW) {
                    descText = this.font.plainSubstrByWidth(descText, infoMaxW - 10) + "...";
                }
                graphics.drawString(this.font, descText, cardX + 86, cardY + 24, TEXT_SECONDARY, false);

                // 3. Botón Eliminar (40px wide) with Ponder copper/red style
                int cardDelBg = delHovered ? 0xFF3A1111 : 0xFF140F0D;
                int cardDelFg = delHovered ? 0xFFFF5555 : TEXT_MUTED;
                graphics.fill(cardX + cardW - 40, cardY + 2, cardX + cardW - 2, cardY + cardH - 2, cardDelBg);
                graphics.fill(cardX + cardW - 40, cardY + 2, cardX + cardW - 38, cardY + cardH - 2, 0xFF2A201C); // Separator
                graphics.drawCenteredString(this.font, "✕", cardX + cardW - 20, cardY + 16, cardDelFg);
            }

            RenderSystem.disableScissor();

            // Render scrollbar if visible cards overflow with Cobre style
            int totalReqsH = p.requirements.size() * 46;
            if (totalReqsH > reqListH) {
                int scrollbarX = editorX + editorW - 15;
                int scrollbarY = startCardY;
                graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + reqListH, 0xFF2A201C);

                float fraction = (float) scrollY / (totalReqsH - reqListH);
                int thumbH = Math.max(12, (int) (((float) reqListH / totalReqsH) * reqListH));
                int thumbY = scrollbarY + (int) (fraction * (reqListH - thumbH));
                graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbH, COLOR_COPPER);
            }

            // Render unified context menu if active
            if (contextMenuIndex != -1 && !activeMenuOptions.isEmpty()) {
                int menuW = 80;
                int optionH = 16;
                int menuH = activeMenuOptions.size() * optionH + 4; // 2px padding top/bottom

                drawFlatPanel(graphics, contextMenuX, contextMenuY, menuW, menuH, WIDGET_BACKGROUND, BORDER_INNER);

                for (int o = 0; o < activeMenuOptions.size(); o++) {
                    MenuOption opt = activeMenuOptions.get(o);
                    int optY = contextMenuY + 2 + o * optionH;
                    boolean optHovered = mouseX >= contextMenuX && mouseX < contextMenuX + menuW && mouseY >= optY && mouseY < optY + optionH;

                    if (optHovered) {
                        int hoverBg = opt.isDanger ? 0xFF3A1111 : BUTTON_HOVER_BG;
                        graphics.fill(contextMenuX + 2, optY, contextMenuX + menuW - 2, optY + optionH, hoverBg);
                    }

                    int textCol = optHovered ? (opt.isDanger ? 0xFFFF5555 : TEXT_PRIMARY) : TEXT_SECONDARY;
                    graphics.drawCenteredString(this.font, opt.label, contextMenuX + menuW / 2, optY + (optionH - 8) / 2, textCol);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int sidebarW = containerW < 450 ? 95 : (int) (containerW * 0.25);
        int editorX = containerX + sidebarW + 2;
        int editorW = containerW - sidebarW - 4;
        int editorH = bodyH;

        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size() && mouseX >= editorX + 20) {
            PathInfo p = localPaths.get(selectedPathIndex);
            int reqListH = editorH - (reqTitleY - bodyY + 16) - 10;
            int totalReqsH = p.requirements.size() * 46;
            int maxScroll = Math.max(0, totalReqsH - reqListH);

            if (maxScroll > 0) {
                scrollY = Math.max(0, Math.min(maxScroll, scrollY - delta * 15));
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
        int sidebarW = containerW < 450 ? 95 : (int) (containerW * 0.25);
        int editorX = containerX + sidebarW + 2;
        int editorW = containerW - sidebarW - 4;
        int editorH = bodyH;

        // 1. Handle Unified Context Menu left click / dismiss
        if (contextMenuIndex != -1) {
            if (button == 0) {
                int menuW = 80;
                int optionH = 16;
                int menuH = activeMenuOptions.size() * optionH + 4;
                if (mouseX >= contextMenuX && mouseX < contextMenuX + menuW && mouseY >= contextMenuY && mouseY < contextMenuY + menuH) {
                    int clickedOptIndex = (int) ((mouseY - contextMenuY - 2) / optionH);
                    if (clickedOptIndex >= 0 && clickedOptIndex < activeMenuOptions.size()) {
                        playClickSound();
                        MenuOption opt = activeMenuOptions.get(clickedOptIndex);
                        contextMenuIndex = -1;
                        activeMenuOptions.clear();
                        opt.action.run();
                        return true;
                    }
                }
            }
            contextMenuIndex = -1;
            activeMenuOptions.clear();
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
            for (int i = 0; i < localPaths.size(); i++) {
                int itemY = listY + i * 20;
                if (mouseX >= listX && mouseX < listX + itemW && mouseY >= itemY && mouseY < itemY + itemH) {
                    playClickSound();
                    contextMenuIndex = i;
                    contextMenuIsBranch = true;
                    contextMenuX = (int) mouseX;
                    contextMenuY = (int) mouseY;

                    activeMenuOptions.clear();
                    int finalI = i;
                    if (i > 0) {
                        activeMenuOptions.add(new MenuOption("▲", () -> {
                            PathInfo path1 = localPaths.get(finalI);
                            PathInfo path2 = localPaths.get(finalI - 1);
                            localPaths.set(finalI - 1, path1);
                            localPaths.set(finalI, path2);
                            selectedPathIndex = finalI - 1;
                            updateEditors();
                        }));
                    }
                    if (i < localPaths.size() - 1) {
                        activeMenuOptions.add(new MenuOption("▼", () -> {
                            PathInfo path1 = localPaths.get(finalI);
                            PathInfo path2 = localPaths.get(finalI + 1);
                            localPaths.set(finalI + 1, path1);
                            localPaths.set(finalI, path2);
                            selectedPathIndex = finalI + 1;
                            updateEditors();
                        }));
                    }
                    activeMenuOptions.add(new MenuOption("Borrar", () -> {
                        PathInfo target = localPaths.get(finalI);
                        Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(this, () -> {
                            localPaths.remove(finalI);
                            if (selectedPathIndex >= finalI) {
                                selectedPathIndex = localPaths.isEmpty() ? -1 : 0;
                            }
                            updateEditors();
                        }, target.name));
                    }, true));
                    return true;
                }
            }

            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                int startCardY = reqTitleY + 16;
                int reqListH = editorH - (reqTitleY - bodyY + 16) - 10;
                int cardW = editorW - 40;
                int cardH = 40;

                if (mouseX >= editorX + 20 && mouseX < editorX + editorW - 20 && mouseY >= startCardY && mouseY < startCardY + reqListH) {
                    double clickedY = mouseY + scrollY - startCardY;
                    int cardIndex = (int) (clickedY / 46);
                    double relativeY = clickedY % 46;

                    if (cardIndex >= 0 && cardIndex < p.requirements.size() && relativeY <= cardH) {
                        playClickSound();
                        contextMenuIndex = cardIndex;
                        contextMenuIsBranch = false;
                        contextMenuX = (int) mouseX;
                        contextMenuY = (int) mouseY;

                        activeMenuOptions.clear();
                        activeMenuOptions.add(new MenuOption("Editar", () -> {
                            Requirement req = p.requirements.get(cardIndex);
                            Minecraft.getInstance().setScreen(new RequirementEditScreen(this, p.id, req));
                        }));

                        if (cardIndex > 0) {
                            activeMenuOptions.add(new MenuOption("▲", () -> {
                                Requirement req1 = p.requirements.get(cardIndex);
                                Requirement req2 = p.requirements.get(cardIndex - 1);
                                p.requirements.set(cardIndex - 1, req1);
                                p.requirements.set(cardIndex, req2);
                                updateEditors();
                            }));
                        }

                        if (cardIndex < p.requirements.size() - 1) {
                            activeMenuOptions.add(new MenuOption("▼", () -> {
                                Requirement req1 = p.requirements.get(cardIndex);
                                Requirement req2 = p.requirements.get(cardIndex + 1);
                                p.requirements.set(cardIndex + 1, req1);
                                p.requirements.set(cardIndex, req2);
                                updateEditors();
                            }));
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        if (button == 0) {
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                
                // Icon button click
                int iconW = 20;
                if (mouseX >= iconX && mouseX < iconX + iconW && mouseY >= iconY && mouseY < iconY + iconW) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(new IconSelectionScreen(this, item -> {
                        net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                        if (rl != null) {
                            p.icon = rl.toString();
                        }
                        Minecraft.getInstance().setScreen(this);
                    }));
                    return true;
                }

                // Mod ID "..." button click
                if (mouseX >= browseX && mouseX < browseX + 20 && mouseY >= modY && mouseY < modY + 20) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(new ModSelectionScreen(this, modId -> {
                        this.pathModIdEdit.setValue(modId);
                        p.mod_id = modId;
                        Minecraft.getInstance().setScreen(this);
                    }));
                    return true;
                }

                // Dependencias "..." picker button click
                if (mouseX >= depsBtnX && mouseX < depsBtnX + 20 && mouseY >= secondY && mouseY < secondY + 20) {
                    playClickSound();
                    this.pathDepsEdit.setResponder(null);
                    Minecraft.getInstance().setScreen(new DependencySelectionScreen(
                            this, p.id, localPaths, new ArrayList<>(p.dependencies), deps -> {
                        p.dependencies.clear();
                        p.dependencies.addAll(deps);
                        pathDepsEdit.setValue(String.join(", ", deps));
                        pathDepsEdit.setResponder(val -> {
                            p.dependencies.clear();
                            if (!val.trim().isEmpty()) {
                                for (String dep : val.split(",")) p.dependencies.add(dep.trim());
                            }
                        });
                        Minecraft.getInstance().setScreen(this);
                    }));
                    return true;
                }

                // Rule Button click
                if (mouseX >= minX && mouseX < minX + minW && mouseY >= minY && mouseY < minY + 20) {
                    playClickSound();
                    cycleMinToSwitch(p);
                    return true;
                }
            }

            // Click Prestige Switch
            int prestX = containerX + 140;
            int prestY = containerY + containerH - footerH + (footerH - 12) / 2;
            if (mouseX >= prestX && mouseX < prestX + 110 && mouseY >= prestY && mouseY < prestY + 12) {
                playClickSound();
                xdAbsoluteMastery.ConfigManager.prestigeModeEnabled = !xdAbsoluteMastery.ConfigManager.prestigeModeEnabled;
                return true;
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
            for (int i = 0; i < localPaths.size(); i++) {
                int itemY = listY + i * 20;
                if (mouseX >= listX && mouseX < listX + itemW && mouseY >= itemY && mouseY < itemY + 18) {
                    playClickSound();
                    selectedPathIndex = i;
                    scrollY = 0;
                    updateEditors();
                    return true;
                }
            }

            // Right side editor clicks
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);

                // Perks Button Click
                int perksBtnX = editorX + editorW - 215;
                int perksBtnY = reqTitleY - 4;
                int perksBtnW = 90;
                int perksBtnH = 16;
                if (mouseX >= perksBtnX && mouseX < perksBtnX + perksBtnW && mouseY >= perksBtnY && mouseY < perksBtnY + perksBtnH) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(new PerksConfigScreen(this, p));
                    return true;
                }

                // Add Requirement Button
                int addReqBtnX = editorX + editorW - 120;
                int addReqBtnY = reqTitleY - 4;
                int addReqBtnW = 100;
                int addReqBtnH = 16;
                if (mouseX >= addReqBtnX && mouseX < addReqBtnX + addReqBtnW && mouseY >= addReqBtnY && mouseY < addReqBtnY + addReqBtnH) {
                    playClickSound();
                    addRequirement();
                    return true;
                }

                int startCardY = reqTitleY + 16;
                int reqListH = editorH - (reqTitleY - bodyY + 16) - 10;
                int cardW = editorW - 40;
                int cardH = 40;

                if (mouseX >= editorX + 20 && mouseX < editorX + editorW - 20 && mouseY >= startCardY && mouseY < startCardY + reqListH) {
                    double clickedY = mouseY + scrollY - startCardY;
                    int cardIndex = (int) (clickedY / 46);
                    double relativeY = clickedY % 46;

                    if (cardIndex >= 0 && cardIndex < p.requirements.size() && relativeY <= cardH) {
                        Requirement req = p.requirements.get(cardIndex);
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
                            String taskName = req.name.isEmpty() ? req.id : req.name;
                            Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(this, () -> {
                                p.requirements.remove(cardIndex);
                                updateModIdFromRequirements(p);
                                updateEditors();
                                int totalReqsH1 = p.requirements.size() * 46;
                                int maxScroll1 = Math.max(0, totalReqsH1 - reqListH);
                                scrollY = Math.max(0, Math.min(maxScroll1, scrollY));
                            }, taskName));
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void addPath() {
        PathInfo p = new PathInfo();
        p.id = "nueva_rama_" + (System.currentTimeMillis() % 1000);
        p.name = "Nueva Rama";
        p.mod_id = "modid";
        p.icon = "minecraft:writable_book";
        p.requirements = new ArrayList<>();
        localPaths.add(p);
        selectedPathIndex = localPaths.size() - 1;
        scrollY = 0;
        updateEditors();
    }

    private void addRequirement() {
        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
            PathInfo p = localPaths.get(selectedPathIndex);
            Requirement req = new Requirement("craft", "", "", "");
            // ponytail: don't add to list yet — RequirementEditScreen will add on commit
            Minecraft.getInstance().setScreen(new RequirementEditScreen(this, p.id, req, () -> {
                p.requirements.add(req);
                updateModIdFromRequirements(p);
                updateEditors();
                // Scroll to show newly added item
                int editorH = bodyH;
                int reqListH = editorH - (reqTitleY - bodyY + 16) - 10;
                int totalReqsH = p.requirements.size() * 46;
                scrollY = Math.max(0, totalReqsH - reqListH);
            }));
        }
    }

    private void cycleRequirementType(Requirement req) {
        String currentId = req.id;
        String currentName = req.name;

        if (req.type.equals("craft")) {
            req.type = "collect";
            if (isItem(currentId)) {
                req.description = "Recoge " + currentName;
            } else {
                req.id = "minecraft:dirt";
                req.name = "Recoger Tierra";
                req.description = "Recoge un bloque de tierra";
            }
        } else if (req.type.equals("collect")) {
            req.type = "kill";
            req.id = "minecraft:zombie";
            req.name = "Zombie";
            req.description = "Derrota a Zombie";
        } else if (req.type.equals("kill")) {
            req.type = "advancement";
            req.id = "minecraft:story/root";
            req.name = "Minecraft";
            req.description = "Completa el logro Minecraft";
        } else {
            req.type = "craft";
            if (isItem(currentId)) {
                req.description = "Craftea " + currentName;
            } else {
                req.id = "minecraft:dirt";
                req.name = "Craftear Tierra";
                req.description = "Craftea un bloque de tierra";
            }
        }
        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
            updateModIdFromRequirements(localPaths.get(selectedPathIndex));
        }
    }

    private boolean isItem(String id) {
        if (id == null || id.isEmpty()) return false;
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(net.minecraft.resources.ResourceLocation.tryParse(id));
    }

    private void cycleMinToSwitch(PathInfo p) {
        int max = p.requirements.size();
        if (p.min_to_switch < 0) {
            p.min_to_switch = 0;
        } else if (p.min_to_switch >= max) {
            p.min_to_switch = -1;
        } else {
            p.min_to_switch++;
        }
    }

    private void openSelectorForRequirement(Requirement req) {
        Minecraft mc = Minecraft.getInstance();
        if (req.type.equals("advancement")) {
            mc.setScreen(new AdvancementSelectionScreen(this, adv -> {
                req.id = adv.getId().toString();
                String titleText = adv.getDisplay() != null ? adv.getDisplay().getTitle().getString() : adv.getId().getPath();
                if (titleText.contains("/")) {
                    String[] split = titleText.split("/");
                    titleText = split[split.length - 1];
                }
                titleText = titleText.replace("_", " ");
                if (!titleText.isEmpty()) {
                    titleText = Character.toUpperCase(titleText.charAt(0)) + titleText.substring(1);
                }
                req.name = titleText;
                req.description = adv.getDisplay() != null ? adv.getDisplay().getDescription().getString() : "Completa el logro " + titleText;
                if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                    updateModIdFromRequirements(localPaths.get(selectedPathIndex));
                }
                mc.setScreen(this);
            }));
        } else if (req.type.equals("craft") || req.type.equals("collect")) {
            mc.setScreen(new ItemSelectionScreen(this, item -> {
                net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                if (rl != null) {
                    req.id = rl.toString();
                    String friendlyName = item.getDescription().getString();
                    req.name = friendlyName;
                    if (req.type.equals("craft")) {
                        req.description = "Craftea " + friendlyName;
                    } else {
                        req.description = "Recoge " + friendlyName;
                    }
                }
                if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                    updateModIdFromRequirements(localPaths.get(selectedPathIndex));
                }
                mc.setScreen(this);
            }));
        } else if (req.type.equals("kill")) {
            mc.setScreen(new EntitySelectionScreen(this, type -> {
                net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
                if (rl != null) {
                    req.id = rl.toString();
                    String friendlyName = type.getDescription().getString();
                    req.name = friendlyName;
                    req.description = "Derrota a " + friendlyName;
                }
                if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                    updateModIdFromRequirements(localPaths.get(selectedPathIndex));
                }
                mc.setScreen(this);
            }));
        }
    }

    private void saveConfig() {
        // ponytail: validate all fields with descriptive errors in footer
        for (int i = 0; i < localPaths.size(); i++) {
            PathInfo p = localPaths.get(i);
            if (p.name.trim().isEmpty()) {
                showError("La rama #" + (i + 1) + " no tiene nombre");
                return;
            }
            if (p.mod_id.trim().isEmpty() || p.mod_id.equals("modid")) {
                showError("'" + p.name + "' necesita un Namespace MOD válido");
                return;
            }
            for (int j = 0; j < p.requirements.size(); j++) {
                Requirement req = p.requirements.get(j);
                if (req.id.trim().isEmpty()) {
                    showError("Tarea #" + (j + 1) + " de '" + p.name + "' sin objetivo (ID)");
                    return;
                }
                if (req.name.trim().isEmpty()) {
                    showError("Tarea #" + (j + 1) + " de '" + p.name + "' sin nombre");
                    return;
                }
                if (req.description.trim().isEmpty()) {
                    showError("Tarea #" + (j + 1) + " de '" + p.name + "' sin descripción");
                    return;
                }
            }
        }

        String json = xdAbsoluteMastery.ConfigManager.serializePaths(localPaths);
        xdAbsoluteMastery.CHANNEL.sendToServer(new xdAbsoluteMastery.UpdateConfigPacket(json));

        this.saveNotificationMsg = "✔ ¡Maestría guardada correctamente!";
        this.saveNotificationTime = System.currentTimeMillis();
    }

    private void showError(String msg) {
        this.saveNotificationMsg = "✕ " + msg;
        this.saveNotificationTime = System.currentTimeMillis();
    }

    // --- NESTED CLASSES FOR MASTERY DELETION SYSTEM ---

    public static class ConfirmDeleteScreen extends AbstractMasteryScreen {
        private final Screen parent;
        private final Runnable onConfirm;
        private final String targetName;
        private final Screen returnScreen;

        public ConfirmDeleteScreen(Screen parent, Runnable onConfirm, String targetName, Screen returnScreen) {
            super(Component.literal("¿CONFIRMAR ELIMINACIÓN?"));
            this.parent = parent;
            this.onConfirm = onConfirm;
            this.targetName = targetName;
            this.returnScreen = returnScreen;
        }

        public ConfirmDeleteScreen(Screen parent, Runnable onConfirm, String targetName) {
            this(parent, onConfirm, targetName, parent);
        }

        @Override
        protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
            int titleY = containerY + (headerH - 8) / 2;
            graphics.drawString(this.font, "CONFIRMAR ELIMINACIÓN", containerX + 15, titleY, TEXT_PRIMARY, false);
            drawBackButton(graphics, mouseX, mouseY);
        }

        @Override
        protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
            int btnW = 100;
            int btnH = 20;
            int startX = containerX + containerW - 15 - (btnW * 2 + 10);
            int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

            // Confirmar button (danger/red hover)
            boolean confirmHovered = mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
            int confirmBg = confirmHovered ? 0xFF3A1111 : 0xFF140F0D;
            int confirmBorder = confirmHovered ? 0xFFFF5555 : 0xFF2C221D;
            drawFlatPanel(graphics, startX, btnY, btnW, btnH, confirmBg, confirmBorder);
            graphics.drawCenteredString(this.font, "Confirmar", startX + btnW / 2, btnY + 6, confirmHovered ? TEXT_PRIMARY : TEXT_SECONDARY);

            // Regresar button (standard copper)
            drawFlatButton(graphics, startX + btnW + 10, btnY, btnW, btnH, "Regresar", mouseX, mouseY, true);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);

            int panelW = (int) (containerW * 0.80);
            int panelH = (int) (bodyH * 0.50);
            int panelX = containerX + (containerW - panelW) / 2;
            int panelY = bodyY + (bodyH - panelH) / 2;

            drawFlatPanel(graphics, panelX, panelY, panelW, panelH, PANEL_INNER_BG, WARM_BORDER);

            String text1 = "¿Estás seguro de que deseas borrar?";
            String text2 = "\"" + targetName + "\"?";
            String text3 = "Esta acción no se puede deshacer.";

            graphics.drawCenteredString(this.font, text1, panelX + panelW / 2, panelY + 25, TEXT_PRIMARY);
            graphics.drawCenteredString(this.font, text2, panelX + panelW / 2, panelY + 45, COLOR_BRASS);
            graphics.drawCenteredString(this.font, text3, panelX + panelW / 2, panelY + 65, 0xFFFF5555);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                if (isBackButtonClicked(mouseX, mouseY)) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(this.parent);
                    return true;
                }

                int btnW = 100;
                int btnH = 20;
                int startX = containerX + containerW - 15 - (btnW * 2 + 10);
                int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

                // Confirmar
                if (mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    onConfirm.run();
                    Minecraft.getInstance().setScreen(this.returnScreen);
                    return true;
                }

                // Regresar
                if (mouseX >= startX + btnW + 10 && mouseX < startX + btnW + 10 + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(this.parent);
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public static class DeleteMasteryScreen extends AbstractPickerScreen<PathInfo> {
        private final MasteryEditorScreen editor;
        private final List<String> selectedIds = new ArrayList<>();

        public DeleteMasteryScreen(MasteryEditorScreen editor) {
            super(editor, Component.literal("SELECCIONAR MAESTRÍAS A BORRAR"), null);
            this.editor = editor;
        }

        @Override
        protected boolean shouldShowNamespaceFilter() { return false; }

        @Override
        protected void populateEntries() {
            this.allEntries.addAll(editor.localPaths);
        }

        @Override
        protected void filterEntries(String query) {
            this.filteredEntries.clear();
            String q = query.toLowerCase();
            for (PathInfo p : this.allEntries) {
                if (p.name.toLowerCase().contains(q) || p.id.toLowerCase().contains(q)) {
                    this.filteredEntries.add(p);
                }
            }
        }

        @Override
        protected void renderEntry(GuiGraphics g, PathInfo p, int x, int y, int index, boolean hovered) {
            // Draw Checkbox
            boolean selected = selectedIds.contains(p.id);
            g.drawString(this.font, selected ? "☑" : "☐", x + 5, y + 6, selected ? COLOR_BRASS : TEXT_MUTED, false);

            // Icon
            net.minecraft.world.item.ItemStack iconStack = net.minecraft.world.item.ItemStack.EMPTY;
            if (p.icon != null) {
                net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(p.icon));
                if (item != null) {
                    iconStack = new net.minecraft.world.item.ItemStack(item);
                }
            }
            if (iconStack.isEmpty()) {
                iconStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
            }
            g.renderFakeItem(iconStack, x + 24, y + 1);

            // Name
            g.drawString(this.font, p.name + " (" + p.id + ")", x + 46, y + 6, selected ? COLOR_BRASS : (hovered ? TEXT_PRIMARY : TEXT_SECONDARY), false);
        }

        @Override
        protected void onClickEntry(PathInfo p) {
            if (selectedIds.contains(p.id)) {
                selectedIds.remove(p.id);
            } else {
                selectedIds.add(p.id);
            }
        }

        @Override
        protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
            int btnW = 100, btnH = 20;
            int startX = containerX + containerW - 15 - (btnW * 2 + 10);
            int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

            drawFlatButton(graphics, startX, btnY, btnW, btnH, "Cancelar", mouseX, mouseY, true);

            boolean hasSelection = !selectedIds.isEmpty();
            String deleteText = "Borrar (" + selectedIds.size() + ")";
            // Borrar button uses danger hover style
            boolean delHovered = hasSelection && mouseX >= startX + btnW + 10 && mouseX < startX + btnW + 10 + btnW && mouseY >= btnY && mouseY < btnY + btnH;
            int delBg = delHovered ? 0xFF3A1111 : (hasSelection ? COLOR_COPPER : 0xFF181818);
            int delBorder = delHovered ? 0xFFFF5555 : (hasSelection ? COLOR_BRASS : 0xFF282828);
            drawFlatPanel(graphics, startX + btnW + 10, btnY, btnW, btnH, delBg, delBorder);
            graphics.drawCenteredString(this.font, deleteText, startX + btnW + 10 + btnW / 2, btnY + 6, hasSelection ? TEXT_PRIMARY : TEXT_MUTED);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                int btnW = 100, btnH = 20;
                int startX = containerX + containerW - 15 - (btnW * 2 + 10);
                int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

                // Cancelar
                if (mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(this.editor);
                    return true;
                }

                // Borrar (N)
                if (!selectedIds.isEmpty() && mouseX >= startX + btnW + 10 && mouseX < startX + btnW + 10 + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    playClickSound();
                    String targetsLabel = selectedIds.size() == 1 ? selectedIds.get(0) : selectedIds.size() + " maestrías";
                    Minecraft.getInstance().setScreen(new ConfirmDeleteScreen(this, () -> {
                        editor.localPaths.removeIf(p -> selectedIds.contains(p.id));
                        if (editor.selectedPathIndex >= editor.localPaths.size()) {
                            editor.selectedPathIndex = editor.localPaths.isEmpty() ? -1 : 0;
                        }
                        editor.updateEditors();
                    }, targetsLabel, editor));
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
