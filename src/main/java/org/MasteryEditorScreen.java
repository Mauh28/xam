package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.xdAbsoluteMastery.ConfigManager.PathInfo;
import org.xdAbsoluteMastery.ConfigManager.Requirement;

import java.util.ArrayList;
import java.util.List;

public class MasteryEditorScreen extends Screen {
    private final Screen parent;
    private final List<PathInfo> localPaths = new ArrayList<>();
    private int selectedPathIndex = -1;
    private int selectedReqIndex = -1;

    // Edit fields
    private EditBox pathIdEdit;
    private EditBox pathNameEdit;
    private EditBox pathModIdEdit;

    private EditBox reqIdEdit;
    private EditBox reqNameEdit;
    private EditBox reqDescEdit;
    
    private EditBox universalNsEdit;

    private Button typeBtn;
    private Button selectBtn;

    public MasteryEditorScreen(Screen parent) {
        super(Component.literal("Editor de Maestría (XAM)"));
        this.parent = parent;

        // Copy paths to local list so changes are transactional
        for (PathInfo path : xdAbsoluteMastery.ConfigManager.PATHS) {
            PathInfo p = new PathInfo();
            p.id = path.id;
            p.name = path.name;
            p.mod_id = path.mod_id;
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

        int panelWidth = 380;
        int panelHeight = 215;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelY = this.height / 2 - panelHeight / 2;

        // --- LEFT COLUMN ---
        Button addPathBtn = Button.builder(Component.literal("+ Rama"), b -> {
            PathInfo p = new PathInfo();
            p.id = "nueva_rama_" + (System.currentTimeMillis() % 1000);
            p.name = "Nueva Rama";
            p.mod_id = "modid";
            localPaths.add(p);
            selectedPathIndex = localPaths.size() - 1;
            selectedReqIndex = -1;
            updateEditors();
        }).bounds(panelX + 15, panelY + 155, 48, 16).build();
        this.addRenderableWidget(addPathBtn);

        Button delPathBtn = Button.builder(Component.literal("- Rama"), b -> {
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                localPaths.remove(selectedPathIndex);
                selectedPathIndex = Math.max(0, selectedPathIndex - 1);
                if (localPaths.isEmpty()) selectedPathIndex = -1;
                selectedReqIndex = -1;
                updateEditors();
            }
        }).bounds(panelX + 68, panelY + 155, 48, 16).build();
        this.addRenderableWidget(delPathBtn);

        // --- RIGHT COLUMN ---
        pathIdEdit = new EditBox(this.font, panelX + 172, panelY + 12, 65, 14, Component.literal("ID de Rama"));
        this.addRenderableWidget(pathIdEdit);

        pathNameEdit = new EditBox(this.font, panelX + 295, panelY + 12, 75, 14, Component.literal("Nombre"));
        this.addRenderableWidget(pathNameEdit);

        pathModIdEdit = new EditBox(this.font, panelX + 172, panelY + 30, 60, 14, Component.literal("Mod ID"));
        this.addRenderableWidget(pathModIdEdit);

        universalNsEdit = new EditBox(this.font, panelX + 295, panelY + 30, 75, 14, Component.literal("Mods Libres"));
        universalNsEdit.setValue(String.join(",", xdAbsoluteMastery.ConfigManager.UNIVERSAL_NAMESPACES));
        universalNsEdit.setResponder(val -> {
            xdAbsoluteMastery.ConfigManager.UNIVERSAL_NAMESPACES.clear();
            String[] split = val.split(",");
            for (String s : split) {
                String clean = s.trim();
                if (!clean.isEmpty()) {
                    xdAbsoluteMastery.ConfigManager.UNIVERSAL_NAMESPACES.add(clean);
                }
            }
        });
        this.addRenderableWidget(universalNsEdit);

        Button addReqBtn = Button.builder(Component.literal("+ Req"), b -> {
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                Requirement req = new Requirement("craft", "minecraft:dirt", "Craftear Tierra", "Obtén un bloque de tierra");
                p.requirements.add(req);
                selectedReqIndex = p.requirements.size() - 1;
                updateEditors();
            }
        }).bounds(panelX + 140, panelY + 130, 42, 16).build();
        this.addRenderableWidget(addReqBtn);

        Button delReqBtn = Button.builder(Component.literal("- Req"), b -> {
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                if (selectedReqIndex >= 0 && selectedReqIndex < p.requirements.size()) {
                    p.requirements.remove(selectedReqIndex);
                    selectedReqIndex = Math.max(0, selectedReqIndex - 1);
                    if (p.requirements.isEmpty()) selectedReqIndex = -1;
                    updateEditors();
                }
            }
        }).bounds(panelX + 185, panelY + 130, 42, 16).build();
        this.addRenderableWidget(delReqBtn);

        typeBtn = Button.builder(Component.literal("Tipo: Craft"), b -> {
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                if (selectedReqIndex >= 0 && selectedReqIndex < p.requirements.size()) {
                    Requirement req = p.requirements.get(selectedReqIndex);
                    if (req.type.equals("advancement")) {
                        req.type = "craft";
                    } else if (req.type.equals("craft")) {
                        req.type = "collect";
                    } else if (req.type.equals("collect")) {
                        req.type = "kill";
                    } else {
                        req.type = "advancement";
                    }
                    b.setMessage(Component.literal("Tipo: " + getTypeName(req.type)));
                }
            }
        }).bounds(panelX + 232, panelY + 130, 68, 16).build();
        this.addRenderableWidget(typeBtn);

        reqIdEdit = new EditBox(this.font, panelX + 140, panelY + 155, 75, 14, Component.literal("ID"));
        this.addRenderableWidget(reqIdEdit);

        selectBtn = Button.builder(Component.literal("..."), b -> {
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                if (selectedReqIndex >= 0 && selectedReqIndex < p.requirements.size()) {
                    Requirement req = p.requirements.get(selectedReqIndex);
                    Minecraft mc = Minecraft.getInstance();
                    if (req.type.equals("advancement")) {
                        mc.setScreen(new AdvancementSelectionScreen(this, adv -> {
                            req.id = adv.getId().toString();
                            if (req.name.isEmpty() || req.name.startsWith("Nueva") || req.name.startsWith("Completa") || req.name.startsWith("Logro")) {
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
                            }
                            reqIdEdit.setValue(req.id);
                            reqNameEdit.setValue(req.name);
                            reqDescEdit.setValue(req.description);
                            mc.setScreen(this);
                        }));
                    } else if (req.type.equals("craft") || req.type.equals("collect")) {
                        mc.setScreen(new ItemSelectionScreen(this, item -> {
                            net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                            if (rl != null) {
                                req.id = rl.toString();
                                String friendlyName = item.getDescription().getString();
                                if (req.name.isEmpty() || req.name.startsWith("Nueva") || req.name.startsWith("Craft") || req.name.startsWith("Obtener") || req.name.startsWith("Recoger")) {
                                    req.name = friendlyName;
                                    if (req.type.equals("craft")) {
                                        req.description = "Craftea " + friendlyName;
                                    } else {
                                        req.description = "Recoge " + friendlyName;
                                    }
                                }
                                reqIdEdit.setValue(req.id);
                                reqNameEdit.setValue(req.name);
                                reqDescEdit.setValue(req.description);
                            }
                            mc.setScreen(this);
                        }));
                    } else if (req.type.equals("kill")) {
                        mc.setScreen(new EntitySelectionScreen(this, type -> {
                            net.minecraft.resources.ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
                            if (rl != null) {
                                req.id = rl.toString();
                                String friendlyName = type.getDescription().getString();
                                if (req.name.isEmpty() || req.name.startsWith("Nueva") || req.name.startsWith("Derrotar") || req.name.startsWith("Matar")) {
                                    req.name = friendlyName;
                                    req.description = "Derrota a " + friendlyName;
                                }
                                reqIdEdit.setValue(req.id);
                                reqNameEdit.setValue(req.name);
                                reqDescEdit.setValue(req.description);
                            }
                            mc.setScreen(this);
                        }));
                    }
                }
            }
        }).bounds(panelX + 220, panelY + 155, 30, 14).build();
        this.addRenderableWidget(selectBtn);

        reqNameEdit = new EditBox(this.font, panelX + 255, panelY + 155, 115, 14, Component.literal("Nombre"));
        this.addRenderableWidget(reqNameEdit);

        reqDescEdit = new EditBox(this.font, panelX + 140, panelY + 173, 230, 14, Component.literal("Descripción"));
        this.addRenderableWidget(reqDescEdit);

        Button saveBtn = Button.builder(Component.literal("Guardar"), b -> {
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
                Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("Error: Los IDs y Nombres no pueden estar vacíos.").withStyle(net.minecraft.ChatFormatting.RED)
                );
                return;
            }

            String json = xdAbsoluteMastery.ConfigManager.serializePaths(localPaths);
            xdAbsoluteMastery.CHANNEL.sendToServer(new xdAbsoluteMastery.UpdateConfigPacket(json));
            
            this.onClose();
        }).bounds(panelX + panelWidth / 2 - 85, panelY + 195, 80, 16).build();
        this.addRenderableWidget(saveBtn);

        Button cancelBtn = Button.builder(Component.literal("Cancelar"), b -> this.onClose())
                .bounds(panelX + panelWidth / 2 + 5, panelY + 195, 80, 16).build();
        this.addRenderableWidget(cancelBtn);

        updateEditors();
    }

    private void updateEditors() {
        boolean pathSelected = selectedPathIndex >= 0 && selectedPathIndex < localPaths.size();
        pathIdEdit.visible = pathSelected;
        pathNameEdit.visible = pathSelected;
        pathModIdEdit.visible = pathSelected;

        if (pathSelected) {
            PathInfo p = localPaths.get(selectedPathIndex);
            pathIdEdit.setResponder(null);
            pathIdEdit.setValue(p.id);
            pathIdEdit.setResponder(val -> p.id = val);

            pathNameEdit.setResponder(null);
            pathNameEdit.setValue(p.name);
            pathNameEdit.setResponder(val -> p.name = val);

            pathModIdEdit.setResponder(null);
            pathModIdEdit.setValue(p.mod_id);
            pathModIdEdit.setResponder(val -> p.mod_id = val);

            boolean reqSelected = selectedReqIndex >= 0 && selectedReqIndex < p.requirements.size();
            reqIdEdit.visible = reqSelected;
            selectBtn.visible = reqSelected;
            reqNameEdit.visible = reqSelected;
            reqDescEdit.visible = reqSelected;
            typeBtn.visible = reqSelected;

            if (reqSelected) {
                Requirement req = p.requirements.get(selectedReqIndex);
                reqIdEdit.setResponder(null);
                reqIdEdit.setValue(req.id);
                reqIdEdit.setResponder(val -> req.id = val);

                reqNameEdit.setResponder(null);
                reqNameEdit.setValue(req.name);
                reqNameEdit.setResponder(val -> req.name = val);

                reqDescEdit.setResponder(null);
                reqDescEdit.setValue(req.description);
                reqDescEdit.setResponder(val -> req.description = val);

                typeBtn.setMessage(Component.literal("Tipo: " + getTypeName(req.type)));
            }
        } else {
            reqIdEdit.visible = false;
            selectBtn.visible = false;
            reqNameEdit.visible = false;
            reqDescEdit.visible = false;
            typeBtn.visible = false;
        }
    }

    private String getTypeName(String type) {
        if (type.equals("advancement")) return "Logro";
        if (type.equals("craft")) return "Craft";
        if (type.equals("collect")) return "Collect";
        if (type.equals("kill")) return "Kill";
        return type;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int panelWidth = 380;
        int panelHeight = 215;
        int panelX = this.width / 2 - panelWidth / 2;
        int panelY = this.height / 2 - panelHeight / 2;

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xDD0F0F12);
        guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFFFFD700);

        guiGraphics.drawString(this.font, "RAMAS", panelX + 15, panelY + 15, 0xFFFFD700, false);
        guiGraphics.fill(panelX + 130, panelY + 10, panelX + 131, panelY + panelHeight - 30, 0x33FFFFFF);

        // Render Paths List
        int listX = panelX + 15;
        int listY = panelY + 30;
        for (int i = 0; i < localPaths.size(); i++) {
            PathInfo p = localPaths.get(i);
            int color = (i == selectedPathIndex) ? 0xFFFFD700 : 0xFFFFFF;
            String text = p.name;
            if (this.font.width(text) > 105) {
                text = this.font.plainSubstrByWidth(text, 95) + "...";
            }
            guiGraphics.drawString(this.font, text, listX, listY + i * 12, color, false);
        }

        if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
            PathInfo p = localPaths.get(selectedPathIndex);
            
            guiGraphics.drawString(this.font, "ID:", panelX + 140, panelY + 15, 0x888888, false);
            guiGraphics.drawString(this.font, "Nom:", panelX + 255, panelY + 15, 0x888888, false);
            guiGraphics.drawString(this.font, "Mod:", panelX + 140, panelY + 33, 0x888888, false);
            guiGraphics.drawString(this.font, "Libres:", panelX + 248, panelY + 33, 0x888888, false);

            guiGraphics.drawString(this.font, "Requisitos:", panelX + 140, panelY + 50, 0xFFFFD700, false);

            // Render Requirements List
            int reqListX = panelX + 140;
            int reqListY = panelY + 62;
            for (int i = 0; i < p.requirements.size(); i++) {
                Requirement req = p.requirements.get(i);
                int color = (i == selectedReqIndex) ? 0xFFFFD700 : 0xAAAAAA;
                String label = req.name.isEmpty() ? req.id : req.name;
                label = "[" + getTypeName(req.type) + "] " + label;
                if (this.font.width(label) > 220) {
                    label = this.font.plainSubstrByWidth(label, 210) + "...";
                }
                guiGraphics.drawString(this.font, label, reqListX, reqListY + i * 12, color, false);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelWidth = 380;
            int panelHeight = 215;
            int panelX = this.width / 2 - panelWidth / 2;
            int panelY = this.height / 2 - panelHeight / 2;

            // Click left column paths
            int listX = panelX + 15;
            int listY = panelY + 30;
            if (mouseX >= listX && mouseX < listX + 110 && mouseY >= listY && mouseY < listY + localPaths.size() * 12) {
                int clickedIndex = (int) ((mouseY - listY) / 12);
                if (clickedIndex >= 0 && clickedIndex < localPaths.size()) {
                    selectedPathIndex = clickedIndex;
                    selectedReqIndex = -1;
                    updateEditors();
                    return true;
                }
            }

            // Click right column requirements
            if (selectedPathIndex >= 0 && selectedPathIndex < localPaths.size()) {
                PathInfo p = localPaths.get(selectedPathIndex);
                int reqListX = panelX + 140;
                int reqListY = panelY + 62;
                if (mouseX >= reqListX && mouseX < reqListX + 230 && mouseY >= reqListY && mouseY < reqListY + p.requirements.size() * 12) {
                    int clickedIndex = (int) ((mouseY - reqListY) / 12);
                    if (clickedIndex >= 0 && clickedIndex < p.requirements.size()) {
                        selectedReqIndex = clickedIndex;
                        updateEditors();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
