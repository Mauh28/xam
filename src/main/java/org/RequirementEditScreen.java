package org;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.xdAbsoluteMastery.ConfigManager.Requirement;

import java.util.ArrayList;

public class RequirementEditScreen extends AbstractMasteryScreen {
    private final MasteryEditorScreen parent;
    private final String parentPathId;
    private final Requirement requirement;
    private final Runnable onCommit;

    private EditBox nameEdit;
    private EditBox descEdit;
    private EditBox idEdit;
    private EditBox dependenciesEdit;

    private String errorMsg = null;
    private boolean committed = false;

    // Layout fields
    private int panelW, panelX;
    private int nameX, nameY, nameW;
    private int descX, descY;
    private int typeX, typeY, typeW;
    private int idX, idY, idW;
    private int changeBtnX, changeBtnY, changeBtnW;
    private int depsX, depsY, depsEditW, depsBtnX;

    public RequirementEditScreen(MasteryEditorScreen parent, String parentPathId, Requirement requirement) {
        this(parent, parentPathId, requirement, null);
    }

    public RequirementEditScreen(MasteryEditorScreen parent, String parentPathId, Requirement requirement, Runnable onCommit) {
        super(Component.literal("EDITAR REQUISITO"));
        this.parent = parent;
        this.parentPathId = parentPathId;
        this.requirement = requirement;
        this.onCommit = onCommit;
    }

    @Override
    protected void init() {
        super.init();

        this.panelW = (int) (containerW * 0.80);
        this.panelX = containerX + (containerW - panelW) / 2;
        
        int gapY = containerH < 220 ? 25 : 35;
        int startY = bodyY + (containerH < 220 ? 5 : 15);

        this.nameW = panelW - 40;
        this.nameX = panelX + 20;
        this.nameY = startY + 12;

        this.descX = nameX;
        this.descY = nameY + gapY;

        this.typeY = descY + gapY + 10;
        this.typeX = nameX;
        this.typeW = 120;

        boolean isNarrow = panelW < 380;
        if (!isNarrow) {
            this.idX = panelX + 150;
            this.idW = panelW - 40 - 120 - 100 - 20;
            this.idY = typeY;

            this.changeBtnX = idX + idW + 10;
            this.changeBtnY = typeY;
            this.changeBtnW = 100;

            this.depsY = typeY + gapY + 10;
            this.depsEditW = nameW - 25;
            this.depsX = nameX;
            this.depsBtnX = nameX + depsEditW + 5;
        } else {
            // Stacked layout for small sizes
            this.idX = nameX;
            this.idW = nameW;
            this.idY = typeY + 30;

            this.changeBtnX = nameX + 130;
            this.changeBtnY = typeY;
            this.changeBtnW = nameW - 130;

            this.depsY = idY + gapY + 5;
            this.depsEditW = nameW - 25;
            this.depsX = nameX;
            this.depsBtnX = nameX + depsEditW + 5;
        }

        // Nombre Input Box
        this.nameEdit = new EditBox(this.font, nameX + 4, nameY + 5, nameW - 8, 12, Component.literal("Nombre"));
        this.nameEdit.setBordered(false);
        this.nameEdit.setTextColor(TEXT_PRIMARY);
        this.nameEdit.setValue(requirement.name);
        this.addRenderableWidget(this.nameEdit);

        // Descripción Input Box
        this.descEdit = new EditBox(this.font, nameX + 4, descY + 5, nameW - 8, 12, Component.literal("Descripción"));
        this.descEdit.setBordered(false);
        this.descEdit.setTextColor(TEXT_PRIMARY);
        this.descEdit.setValue(requirement.description);
        this.addRenderableWidget(this.descEdit);

        // ID/Objetivo Input Box (Editable!)
        this.idEdit = new EditBox(this.font, idX + 4, idY + 5, idW - 8, 12, Component.literal("ID"));
        this.idEdit.setBordered(false);
        this.idEdit.setTextColor(TEXT_PRIMARY);
        this.idEdit.setValue(requirement.id);
        this.addRenderableWidget(this.idEdit);

        // Dependencias Input Box
        this.dependenciesEdit = new EditBox(this.font, depsX + 4, depsY + 5, depsEditW - 8, 12, Component.literal("Dependencias"));
        this.dependenciesEdit.setBordered(false);
        this.dependenciesEdit.setTextColor(TEXT_PRIMARY);
        this.dependenciesEdit.setValue(String.join(", ", requirement.dependencies));
        this.addRenderableWidget(this.dependenciesEdit);
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
        if (this.dependenciesEdit != null) {
            this.requirement.dependencies.clear();
            String val = this.dependenciesEdit.getValue();
            if (!val.trim().isEmpty()) {
                for (String dep : val.split(",")) {
                    this.requirement.dependencies.add(dep.trim());
                }
            }
        }
    }

    public boolean isCommitted() {
        return committed;
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, "EDITAR REQUISITO", containerX + 15, titleY, TEXT_PRIMARY, false);
        drawBackButton(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        // Show validation error in footer if present
        if (errorMsg != null) {
            int msgY = containerY + containerH - footerH + (footerH - 8) / 2;
            graphics.drawString(this.font, errorMsg, containerX + 15, msgY, 0xFFFF5555, false);
        }

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

        // Labels
        graphics.drawString(this.font, "Nombre", nameX, nameY - 11, TEXT_MUTED, false);
        graphics.drawString(this.font, "Descripción", nameX, descY - 11, TEXT_MUTED, false);

        // Input Background Panels — highlight red if validation failed and field is empty
        boolean nameEmpty = nameEdit != null && nameEdit.getValue().trim().isEmpty();
        boolean descEmpty = descEdit != null && descEdit.getValue().trim().isEmpty();
        int nameBorder = (errorMsg != null && nameEmpty) ? 0xFFFF5555 : BORDER_STANDARD;
        int descBorder = (errorMsg != null && descEmpty) ? 0xFFFF5555 : BORDER_STANDARD;
        drawFlatPanel(graphics, nameX, nameY, nameW, 20, INPUT_BACKGROUND, nameBorder);
        drawFlatPanel(graphics, nameX, descY, nameW, 20, INPUT_BACKGROUND, descBorder);

        // Tipo & ID section
        graphics.drawString(this.font, "Objetivo (ID)", idX, idY - 11, TEXT_MUTED, false);

        // Draw Tipo Button
        String typeLabel = "Tipo: " + requirement.type.toUpperCase();
        drawFlatButton(graphics, typeX, typeY, typeW, 20, typeLabel, mouseX, mouseY, true);

        // Draw ID Panel — highlight red if empty
        boolean idEmpty = idEdit != null && idEdit.getValue().trim().isEmpty();
        int idBorder = (errorMsg != null && idEmpty) ? 0xFFFF5555 : BORDER_STANDARD;
        drawFlatPanel(graphics, idX, idY, idW, 20, INPUT_BACKGROUND, idBorder);

        // Draw Cambiar Objetivo Button
        drawFlatButton(graphics, changeBtnX, changeBtnY, changeBtnW, 20, "Cambiar", mouseX, mouseY, true);

        // Dependencias section
        graphics.drawString(this.font, "Dependencias del Requisito (ej. botania:1, mekanism:2)", depsX, depsY - 11, TEXT_MUTED, false);
        drawFlatPanel(graphics, depsX, depsY, depsEditW, 20, INPUT_BACKGROUND, BORDER_STANDARD);

        boolean depsBtnHovered = mouseX >= depsBtnX && mouseX < depsBtnX + 20 && mouseY >= depsY && mouseY < depsY + 20;
        int depsBtnBg = depsBtnHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int depsBtnBorder = depsBtnHovered ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, depsBtnX, depsY, 20, 20, depsBtnBg, depsBtnBorder);
        graphics.drawCenteredString(this.font, "...", depsBtnX + 10, depsY + 6, TEXT_PRIMARY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isBackButtonClicked(mouseX, mouseY)) {
            playClickSound();
            this.minecraft.setScreen(this.parent);
            return true;
        }
        if (button == 0) {
            // 1. Click Tipo Button -> Cycle type
            if (mouseX >= typeX && mouseX < typeX + typeW && mouseY >= typeY && mouseY < typeY + 20) {
                playClickSound();
                saveFields();
                cycleRequirementType(requirement);
                return true;
            }

            // 2. Click Cambiar Button -> Open selector screen
            if (mouseX >= changeBtnX && mouseX < changeBtnX + changeBtnW && mouseY >= changeBtnY && mouseY < changeBtnY + 20) {
                playClickSound();
                saveFields();
                openSelectorForRequirement(requirement);
                return true;
            }

            // 3. Click Dependencias "..." -> Open dependency picker
            if (mouseX >= depsBtnX && mouseX < depsBtnX + 20 && mouseY >= depsY && mouseY < depsY + 20) {
                playClickSound();
                saveFields();
                Minecraft.getInstance().setScreen(new DependencySelectionScreen(
                        this, parentPathId, org.xdAbsoluteMastery.ConfigManager.PATHS,
                        new ArrayList<>(requirement.dependencies), deps -> {
                    requirement.dependencies.clear();
                    requirement.dependencies.addAll(deps);
                    if (dependenciesEdit != null) dependenciesEdit.setValue(String.join(", ", deps));
                    Minecraft.getInstance().setScreen(this);
                }));
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

            // Guardar — validate before accepting
            if (mouseX >= startX + btnW + 10 && mouseX < startX + btnW + 10 + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                playClickSound();
                saveFields();
                // ponytail: inline validation — cheapest thing that blocks bad data
                boolean nameOk = !requirement.name.trim().isEmpty();
                boolean descOk = !requirement.description.trim().isEmpty();
                boolean idOk = !requirement.id.trim().isEmpty();
                if (!nameOk || !descOk || !idOk) {
                    errorMsg = "✕ Completa todos los campos antes de guardar";
                    return true;
                }
                errorMsg = null;
                committed = true;
                if (onCommit != null) onCommit.run();
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
