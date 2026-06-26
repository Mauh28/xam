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

    // Context menu state
    private int contextMenuCardIndex = -1;
    private int contextMenuX = 0;
    private int contextMenuY = 0;

    // Custom text input boxes
    private EditBox pathNameEdit;
    private EditBox pathModIdEdit;

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
            p.dependencies = new ArrayList<>(path.dependencies);
            p.requirements = new ArrayList<>();
            for (Requirement req : path.requirements) {
                Requirement r = new Requirement();
                r.type = req.type;
                r.id = req.id;
                r.name = req.name;
                r.description = req.description;
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

        int sidebarW = (int) (containerW * 0.25);
        int editorX = containerX + sidebarW + 2;
        int editorW = containerW - sidebarW - 4;
        int editorH = bodyH;

        int inputH = (int) (editorH * 0.10);

        int iconW = 20;
        int iconX = editorX + 20;
        int iconY = bodyY + 15;

        int titleW = (int) ((editorW - 80) * 0.60);
        int titleX = iconX + iconW + 10;
        int titleY = bodyY + 15;

        int modW = (int) ((editorW - 80) * 0.40);
        int modX = titleX + titleW + 10;
        int modY = bodyY + 15;

        // Edit box for Title
        this.pathNameEdit = new EditBox(this.font, titleX + 4, titleY + 5, titleW - 8, 12, Component.literal("Título"));
        this.pathNameEdit.setBordered(false);
        this.pathNameEdit.setTextColor(TEXT_PRIMARY);
        this.addRenderableWidget(this.pathNameEdit);

        // Edit box for Mod ID (Editable)
        int modEditW = modW - 25;
        this.pathModIdEdit = new EditBox(this.font, modX + 4, modY + 5, modEditW - 8, 12, Component.literal("Namespace MOD"));
        this.pathModIdEdit.setBordered(false);
        this.pathModIdEdit.setTextColor(TEXT_PRIMARY);
        this.pathModIdEdit.setEditable(true);
        this.addRenderableWidget(this.pathModIdEdit);

        updateEditors();
        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
            updateModIdFromRequirements(localPaths.get(selectedPathIndex));
        }
    }

    private void updateEditors() {
        boolean pathSelected = selectedPathIndex >= 0 && selectedPathIndex < localPaths.size();
        pathNameEdit.visible = pathSelected;
        pathModIdEdit.visible = pathSelected;

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
            graphics.drawString(this.font, saveNotificationMsg, containerX + 15, msgY, 0xFF55FF55, false);
        }

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

        // Sidebar dimensions (25% of body width)
        int sidebarW = (int) (containerW * 0.25);
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

        // Sidebar Add Branch Button
        int addPathBtnY = sidebarY + sidebarH - 25;
        boolean addHovered = mouseX >= listX && mouseX < listX + itemW && mouseY >= addPathBtnY && mouseY < addPathBtnY + 18;
        int addBg = addHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int addBorder = addHovered ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, listX, addPathBtnY, itemW, 18, addBg, addBorder);
        graphics.drawCenteredString(this.font, "+ AÑADIR RAMA", listX + itemW / 2, addPathBtnY + 5, TEXT_PRIMARY);

        // --- RIGHT COLUMN (Editor - 75% of body width) ---
        int editorX = containerX + sidebarW + 2;
        int editorW = containerW - sidebarW - 4;
        int editorH = bodyH;

        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
            PathInfo p = localPaths.get(selectedPathIndex);

            int inputH = (int) (editorH * 0.10);
            
            int iconW = 20;
            int iconX = editorX + 20;
            int iconY = bodyY + 15;

            int titleW = (int) ((editorW - 80) * 0.60);
            int titleX = iconX + iconW + 10;
            int titleY = bodyY + 15;

            int modW = (int) ((editorW - 80) * 0.40);
            int modX = titleX + titleW + 10;
            int modY = bodyY + 15;

            // Enclosing metadata frame
            drawFlatPanel(graphics, editorX + 10, bodyY + 5, editorW - 20, inputH + 18, PANEL_INNER_BG, 0xFF2A201C);

            // Inputs Labels
            graphics.drawString(this.font, "Icono", iconX, iconY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, "Título", titleX, titleY - 11, COLOR_BRASS, false);
            graphics.drawString(this.font, "Namespace MOD", modX, modY - 11, COLOR_BRASS, false);

            // Icon background panel
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
            int modEditW = modW - 25;
            drawFlatPanel(graphics, titleX, titleY, titleW, 20, INPUT_BACKGROUND, COLOR_COPPER);
            drawFlatPanel(graphics, modX, modY, modEditW, 20, INPUT_BACKGROUND, COLOR_COPPER);

            // Copper "..." selection button
            int browseX = modX + modEditW + 5;
            boolean browseHovered = mouseX >= browseX && mouseX < browseX + 20 && mouseY >= modY && mouseY < modY + 20;
            int browseBg = browseHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int browseBorder = browseHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, browseX, modY, 20, 20, browseBg, browseBorder);
            graphics.drawCenteredString(this.font, "...", browseX + 10, modY + 6, TEXT_PRIMARY);

            // Requisitos Section
            int reqTitleY = bodyY + 15 + inputH + 15;
            graphics.drawString(this.font, "REQUISITOS", editorX + 20, reqTitleY, COLOR_BRASS, false);
            int reqTitleW = this.font.width("REQUISITOS");
            graphics.fill(editorX + 20, reqTitleY + 10, editorX + 20 + reqTitleW, reqTitleY + 11, COLOR_COPPER);

            // Button "+ AÑADIR ESCENA" with Cobre Ponder style
            int addReqBtnX = editorX + editorW - 120;
            int addReqBtnY = reqTitleY - 4;
            int addReqBtnW = 100;
            int addReqBtnH = 16;
            boolean addReqHovered = mouseX >= addReqBtnX && mouseX < addReqBtnX + addReqBtnW && mouseY >= addReqBtnY && mouseY < addReqBtnY + addReqBtnH;
            int addReqBg = addReqHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int addReqBorder = addReqHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, addReqBtnX, addReqBtnY, addReqBtnW, addReqBtnH, addReqBg, addReqBorder);
            graphics.drawCenteredString(this.font, "+ AÑADIR ESCENA", addReqBtnX + addReqBtnW / 2, addReqBtnY + 4, TEXT_PRIMARY);

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
                int delBg = delHovered ? 0xFF3A1111 : 0xFF140F0D;
                int delFg = delHovered ? 0xFFFF5555 : TEXT_MUTED;
                graphics.fill(cardX + cardW - 40, cardY + 2, cardX + cardW - 2, cardY + cardH - 2, delBg);
                graphics.fill(cardX + cardW - 40, cardY + 2, cardX + cardW - 38, cardY + cardH - 2, 0xFF2A201C); // Separator
                graphics.drawCenteredString(this.font, "✕", cardX + cardW - 20, cardY + 16, delFg);
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

            // Render context menu if active
            if (contextMenuCardIndex != -1) {
                int menuW = 80;
                int menuH = 22;
                boolean optionHovered = mouseX >= contextMenuX && mouseX < contextMenuX + menuW && mouseY >= contextMenuY && mouseY < contextMenuY + menuH;

                int menuBg = optionHovered ? BUTTON_HOVER_BG : WIDGET_BACKGROUND;
                int menuBorder = optionHovered ? BUTTON_HOVER_BORDER : BORDER_INNER;

                drawFlatPanel(graphics, contextMenuX, contextMenuY, menuW, menuH, menuBg, menuBorder);

                int textCol = optionHovered ? TEXT_PRIMARY : TEXT_SECONDARY;
                graphics.drawCenteredString(this.font, "Editar", contextMenuX + menuW / 2, contextMenuY + (menuH - 8) / 2, textCol);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int sidebarW = (int) (containerW * 0.25);
        int editorX = containerX + sidebarW + 2;
        int editorW = containerW - sidebarW - 4;
        int editorH = bodyH;

        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size() && mouseX >= editorX + 20) {
            PathInfo p = localPaths.get(selectedPathIndex);
            int inputH = (int) (editorH * 0.10);
            int reqTitleY = bodyY + 15 + inputH + 15;
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
        int sidebarW = (int) (containerW * 0.25);
        int editorX = containerX + sidebarW + 2;
        int editorW = containerW - sidebarW - 4;
        int editorH = bodyH;

        // 1. Handle Context Menu left click / dismiss
        if (contextMenuCardIndex != -1) {
            if (button == 0) {
                int menuW = 80;
                int menuH = 22;
                if (mouseX >= contextMenuX && mouseX < contextMenuX + menuW && mouseY >= contextMenuY && mouseY < contextMenuY + menuH) {
                    playClickSound();
                    int index = contextMenuCardIndex;
                    contextMenuCardIndex = -1;
                    if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                        PathInfo p = localPaths.get(selectedPathIndex);
                        if (index >= 0 && index < p.requirements.size()) {
                            Requirement req = p.requirements.get(index);
                            this.minecraft.setScreen(new RequirementEditScreen(this, req));
                        }
                    }
                    return true;
                }
            }
            contextMenuCardIndex = -1;
            if (button != 0) {
                return true; // Consume other click types when dismissing
            }
        }

        // 2. Handle Right Click to open Context Menu on card
        if (button == 1) {
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                int inputH = (int) (editorH * 0.10);
                int reqTitleY = bodyY + 15 + inputH + 15;
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
                        contextMenuCardIndex = cardIndex;
                        contextMenuX = (int) mouseX;
                        contextMenuY = (int) mouseY;
                        return true;
                    }
                }
            }
            return false;
        }

        if (button == 0) {
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                int inputH = (int) (editorH * 0.10);
                
                int iconW = 20;
                int iconX = editorX + 20;
                int iconY = bodyY + 15;

                int titleW = (int) ((editorW - 80) * 0.60);
                int titleX = iconX + iconW + 10;

                int modW = (int) ((editorW - 80) * 0.40);
                int modX = titleX + titleW + 10;
                int modY = bodyY + 15;
                int modEditW = modW - 25;
                
                // Icon button click
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
                int btnX = modX + modEditW + 5;
                if (mouseX >= btnX && mouseX < btnX + 20 && mouseY >= modY && mouseY < modY + 20) {
                    playClickSound();
                    Minecraft.getInstance().setScreen(new ModSelectionScreen(this, modId -> {
                        this.pathModIdEdit.setValue(modId);
                        p.mod_id = modId;
                        Minecraft.getInstance().setScreen(this);
                    }));
                    return true;
                }
            }
            // Footer buttons (Descartar Todo, Guardar Estructura)
            int footBtnW = 120;
            int footBtnH = 20;
            int footStartX = containerX + containerW - 15 - (footBtnW * 2 + 10);
            int footBtnY = containerY + containerH - footerH + (footerH - footBtnH) / 2;

            // Descartar Todo
            if (mouseX >= footStartX && mouseX < footStartX + footBtnW && mouseY >= footBtnY && mouseY < footBtnY + footBtnH) {
                playClickSound();
                this.onClose();
                return true;
            }
            // Guardar Estructura
            if (mouseX >= footStartX + footBtnW + 10 && mouseX < footStartX + footBtnW + 10 + footBtnW && mouseY >= footBtnY && mouseY < footBtnY + footBtnH) {
                playClickSound();
                saveConfig();
                return true;
            }

            // Sidebar: Add Branch
            int sidebarH = bodyH;
            int sidebarX = containerX + 2;
            int sidebarY = bodyY;
            int listX = sidebarX + 10;
            int listY = sidebarY + 25;
            int itemW = sidebarW - 20;
            int itemH = 18;

            int addPathBtnY = sidebarY + sidebarH - 25;
            if (mouseX >= listX && mouseX < listX + itemW && mouseY >= addPathBtnY && mouseY < addPathBtnY + 18) {
                playClickSound();
                addPath();
                return true;
            }

            // Sidebar: select branch
            for (int i = 0; i < localPaths.size(); i++) {
                int itemY = listY + i * 20;
                if (mouseX >= listX && mouseX < listX + itemW && mouseY >= itemY && mouseY < itemY + itemH) {
                    playClickSound();
                    selectedPathIndex = i;
                    scrollY = 0; // Reset scroll on path change
                    updateEditors();
                    return true;
                }
            }

            // Right side editor clicks
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);

                int inputH = (int) (editorH * 0.10);
                int reqTitleY = bodyY + 15 + inputH + 15;

                // Add Scene Button
                int addReqBtnX = editorX + editorW - 120;
                int addReqBtnY = reqTitleY - 4;
                int addReqBtnW = 100;
                int addReqBtnH = 16;
                if (mouseX >= addReqBtnX && mouseX < addReqBtnX + addReqBtnW && mouseY >= addReqBtnY && mouseY < addReqBtnY + addReqBtnH) {
                    playClickSound();
                    addRequirement();
                    return true;
                }

                // Requirements Cards Click (taking scrollY into account)
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
                        int cardY = startCardY + (cardIndex * 46) - (int) scrollY;

                        // 1. Click Type Box (left 80px)
                        if (mouseX >= cardX && mouseX < cardX + 80) {
                            playClickSound();
                            cycleRequirementType(req);
                            return true;
                        }

                        // 2. Click Info Box (middle)
                        if (mouseX >= cardX + 80 && mouseX < cardX + cardW - 40) {
                            playClickSound();
                            openSelectorForRequirement(req);
                            return true;
                        }

                        // 3. Click Delete Box (right 40px)
                        if (mouseX >= cardX + cardW - 40 && mouseX < cardX + cardW) {
                            playClickSound();
                            p.requirements.remove(cardIndex);
                            updateModIdFromRequirements(p);
                            
                            // Adjust scroll if item deletion reduces height below viewport
                            int totalReqsH = p.requirements.size() * 46;
                            int maxScroll = Math.max(0, totalReqsH - reqListH);
                            scrollY = Math.max(0, Math.min(maxScroll, scrollY));
                            
                            updateEditors();
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
            Requirement req = new Requirement("craft", "minecraft:dirt", "Craftear Tierra", "Obtén un bloque de tierra");
            p.requirements.add(req);
            updateModIdFromRequirements(p);
            updateEditors();
            
            // Adjust scroll to make newly added item visible
            int sidebarW = (int) (containerW * 0.25);
            int editorH = bodyH;
            int reqTitleY = bodyY + 15 + (int)(editorH * 0.10) + 15;
            int reqListH = editorH - (reqTitleY - bodyY + 16) - 10;
            int totalReqsH = p.requirements.size() * 46;
            scrollY = Math.max(0, totalReqsH - reqListH);
            
            openSelectorForRequirement(req);
        }
    }

    private void cycleRequirementType(Requirement req) {
        if (req.type.equals("craft")) {
            req.type = "collect";
            req.id = "minecraft:dirt";
            req.name = "Recoger Tierra";
            req.description = "Recoge un bloque de tierra";
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
            req.id = "minecraft:dirt";
            req.name = "Craftear Tierra";
            req.description = "Craftea un bloque de tierra";
        }
        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
            updateModIdFromRequirements(localPaths.get(selectedPathIndex));
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
        boolean valid = true;
        for (PathInfo p : localPaths) {
            if (p.id.trim().isEmpty() || p.name.trim().isEmpty() || p.mod_id.trim().isEmpty()) {
                valid = false;
                break;
            }
            for (Requirement req : p.requirements) {
                if (req.id.trim().isEmpty() || req.name.trim().isEmpty()) {
                    valid = false;
                    break;
                }
            }
        }
        if (!valid) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("Error: Los IDs y Nombres no pueden estar vacíos.").withStyle(net.minecraft.ChatFormatting.RED)
                );
            }
            return;
        }

        String json = xdAbsoluteMastery.ConfigManager.serializePaths(localPaths);
        xdAbsoluteMastery.CHANNEL.sendToServer(new xdAbsoluteMastery.UpdateConfigPacket(json));

        this.saveNotificationMsg = "✔ ¡Maestría guardada correctamente!";
        this.saveNotificationTime = System.currentTimeMillis();
    }
}
