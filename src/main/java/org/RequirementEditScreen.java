package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.xdAbsoluteMastery.ConfigManager.Requirement;

public class RequirementEditScreen extends AbstractMasteryScreen {
    private final MasteryEditorScreen parent;
    private final Requirement requirement;

    private EditBox nameEdit;
    private EditBox descEdit;
    private EditBox idEdit;

    public RequirementEditScreen(MasteryEditorScreen parent, Requirement requirement) {
        super(Component.literal("EDITAR REQUISITO"));
        this.parent = parent;
        this.requirement = requirement;
    }

    @Override
    protected void init() {
        super.init();

        int panelW = (int) (containerW * 0.80);
        int panelX = containerX + (containerW - panelW) / 2;
        int startY = bodyY + 15;

        int nameW = panelW - 40;
        int nameX = panelX + 20;
        int nameY = startY + 15;

        // Nombre Input Box
        this.nameEdit = new EditBox(this.font, nameX + 4, nameY + 5, nameW - 8, 12, Component.literal("Nombre"));
        this.nameEdit.setBordered(false);
        this.nameEdit.setTextColor(TEXT_PRIMARY);
        this.nameEdit.setValue(requirement.name);
        this.addRenderableWidget(this.nameEdit);

        // Descripción Input Box
        int descY = nameY + 35;
        this.descEdit = new EditBox(this.font, nameX + 4, descY + 5, nameW - 8, 12, Component.literal("Descripción"));
        this.descEdit.setBordered(false);
        this.descEdit.setTextColor(TEXT_PRIMARY);
        this.descEdit.setValue(requirement.description);
        this.addRenderableWidget(this.descEdit);

        // ID/Objetivo Input Box (Editable!)
        int typeY = startY + 85;
        int idX = panelX + 150;
        int idW = panelW - 40 - 120 - 100 - 20;
        this.idEdit = new EditBox(this.font, idX + 4, typeY + 5, idW - 8, 12, Component.literal("ID"));
        this.idEdit.setBordered(false);
        this.idEdit.setTextColor(TEXT_PRIMARY);
        this.idEdit.setValue(requirement.id);
        this.addRenderableWidget(this.idEdit);
    }

    private void saveFields() {
        if (this.nameEdit != null) {
            this.requirement.name = this.nameEdit.getValue();
        }
        if (this.descEdit != null) {
            this.requirement.description = this.descEdit.getValue();
        }
        if (this.idEdit != null) {
            this.requirement.id = this.idEdit.getValue();
        }
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, "EDITAR REQUISITO", containerX + 15, titleY, TEXT_PRIMARY, false);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int btnW = 100;
        int btnH = 20;
        int startX = containerX + containerW - 15 - (btnW * 2 + 10);
        int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

        drawFlatButton(graphics, startX, btnY, btnW, btnH, "Cancelar", mouseX, mouseY, true);
        drawFlatButton(graphics, startX + btnW + 10, btnY, btnW, btnH, "Guardar", mouseX, mouseY, true, true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int panelW = (int) (containerW * 0.80);
        int panelX = containerX + (containerW - panelW) / 2;
        int startY = bodyY + 15;

        int nameW = panelW - 40;
        int nameX = panelX + 20;
        int nameY = startY + 15;
        int descY = nameY + 35;

        // Labels
        graphics.drawString(this.font, "Nombre", nameX, nameY - 11, TEXT_MUTED, false);
        graphics.drawString(this.font, "Descripción", nameX, descY - 11, TEXT_MUTED, false);

        // Input Background Panels
        drawFlatPanel(graphics, nameX, nameY, nameW, 20, INPUT_BACKGROUND, BORDER_STANDARD);
        drawFlatPanel(graphics, nameX, descY, nameW, 20, INPUT_BACKGROUND, BORDER_STANDARD);

        // Tipo & ID section
        int typeY = startY + 85;
        int idX = panelX + 150;
        int idW = panelW - 40 - 120 - 100 - 20;

        graphics.drawString(this.font, "Objetivo (ID)", idX, typeY - 11, TEXT_MUTED, false);

        // Draw Tipo Button
        String typeLabel = "Tipo: " + requirement.type.toUpperCase();
        drawFlatButton(graphics, nameX, typeY, 120, 20, typeLabel, mouseX, mouseY, true);

        // Draw ID Panel (Editable background panel with standard border)
        drawFlatPanel(graphics, idX, typeY, idW, 20, INPUT_BACKGROUND, BORDER_STANDARD);

        // Draw Cambiar Objetivo Button
        int changeBtnX = idX + idW + 10;
        drawFlatButton(graphics, changeBtnX, typeY, 100, 20, "Cambiar", mouseX, mouseY, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelW = (int) (containerW * 0.80);
            int panelX = containerX + (containerW - panelW) / 2;
            int startY = bodyY + 15;
            int nameX = panelX + 20;
            int typeY = startY + 85;
            int idX = panelX + 150;
            int idW = panelW - 40 - 120 - 100 - 20;
            int changeBtnX = idX + idW + 10;

            // 1. Click Tipo Button -> Cycle type
            if (mouseX >= nameX && mouseX < nameX + 120 && mouseY >= typeY && mouseY < typeY + 20) {
                playClickSound();
                saveFields();
                cycleRequirementType(requirement);
                return true;
            }

            // 2. Click Cambiar Button -> Open selector screen
            if (mouseX >= changeBtnX && mouseX < changeBtnX + 100 && mouseY >= typeY && mouseY < typeY + 20) {
                playClickSound();
                saveFields();
                openSelectorForRequirement(requirement);
                return true;
            }

            // Footer Buttons
            int btnW = 100;
            int btnH = 20;
            int startX = containerX + containerW - 15 - (btnW * 2 + 10);
            int btnY = containerY + containerH - footerH + (footerH - btnH) / 2;

            // Cancelar
            if (mouseX >= startX && mouseX < startX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                this.minecraft.setScreen(this.parent);
                return true;
            }

            // Guardar
            if (mouseX >= startX + btnW + 10 && mouseX < startX + btnW + 10 + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                saveFields();
                this.minecraft.setScreen(this.parent);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        if (idEdit != null) idEdit.setValue(req.id);
        if (nameEdit != null) nameEdit.setValue(req.name);
        if (descEdit != null) descEdit.setValue(req.description);
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
                mc.setScreen(this);
            }));
        }
    }
}
