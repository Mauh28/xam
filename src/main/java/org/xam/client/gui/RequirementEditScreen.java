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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private EditBox effectEdit;

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
    private int effectX, effectY, effectEditW, effectBtnX;

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

            if (requirement.getType().equals("kill")) {
                this.effectY = typeY + gapY + 10;
                this.effectX = nameX;
                this.effectEditW = nameW - 25;
                this.effectBtnX = nameX + effectEditW + 5;

                this.depsY = effectY + gapY + 10;
            } else {
                this.depsY = typeY + gapY + 10;
            }
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

            if (requirement.getType().equals("kill")) {
                this.effectY = idY + gapY + 5;
                this.effectX = nameX;
                this.effectEditW = nameW - 25;
                this.effectBtnX = nameX + effectEditW + 5;

                this.depsY = effectY + gapY + 5;
            } else {
                this.depsY = idY + gapY + 5;
            }
            this.depsEditW = nameW - 25;
            this.depsX = nameX;
            this.depsBtnX = nameX + depsEditW + 5;
        }

        // Nombre Input Box
        this.nameEdit = new EditBox(this.font, nameX + 4, nameY + 5, nameW - 8, 12, Component.literal("Nombre"));
        this.nameEdit.setBordered(false);
        this.nameEdit.setTextColor(TEXT_PRIMARY);
        this.nameEdit.setValue(requirement.getName());
        this.addRenderableWidget(this.nameEdit);

        // Descripción Input Box
        this.descEdit = new EditBox(this.font, nameX + 4, descY + 5, nameW - 8, 12, Component.literal("Descripción"));
        this.descEdit.setBordered(false);
        this.descEdit.setTextColor(TEXT_PRIMARY);
        this.descEdit.setValue(requirement.getDescription());
        this.descEdit.setMaxLength(52);
        this.addRenderableWidget(this.descEdit);

        // ID/Objetivo Input Box (Editable!)
        this.idEdit = new EditBox(this.font, idX + 4, idY + 5, idW - 8, 12, Component.literal("ID"));
        this.idEdit.setBordered(false);
        this.idEdit.setTextColor(TEXT_PRIMARY);
        this.idEdit.setValue(requirement.getId());
        this.addRenderableWidget(this.idEdit);

        // Effect Input Box (if type is kill)
        if (requirement.getType().equals("kill")) {
            this.effectEdit = new EditBox(this.font, effectX + 4, effectY + 5, effectEditW - 8, 12, Component.literal("Efecto"));
            this.effectEdit.setBordered(false);
            this.effectEdit.setTextColor(TEXT_PRIMARY);
            this.effectEdit.setValue(requirement.getEffect());
            this.addRenderableWidget(this.effectEdit);
        } else {
            this.effectEdit = null;
        }

        // Dependencias Input Box
        this.dependenciesEdit = new EditBox(this.font, depsX + 4, depsY + 5, depsEditW - 8, 12, Component.literal("Dependencias"));
        this.dependenciesEdit.setBordered(false);
        this.dependenciesEdit.setTextColor(TEXT_PRIMARY);
        this.dependenciesEdit.setValue(String.join(", ", requirement.getDependencies()));
        this.addRenderableWidget(this.dependenciesEdit);
    }

    private void saveFields() {
        if (this.nameEdit != null) {
            this.requirement.setName(this.nameEdit.getValue());
        }
        if (this.descEdit != null) {
            this.requirement.setDescription(this.descEdit.getValue());
        }
        if (this.idEdit != null) {
            this.requirement.setId(this.idEdit.getValue());
        }
        if (this.dependenciesEdit != null) {
            this.requirement.clearDependencies();
            String val = this.dependenciesEdit.getValue();
            if (!val.trim().isEmpty()) {
                for (String dep : val.split(",")) {
                    this.requirement.addDependency(dep.trim());
                }
            }
        }
        if (this.effectEdit != null && this.requirement.getType().equals("kill")) {
            this.requirement.setEffect(this.effectEdit.getValue());
        } else if (!this.requirement.getType().equals("kill")) {
            this.requirement.setEffect("");
        }
    }

    public boolean isCommitted() {
        return committed;
    }

    @Override
    protected void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleY = containerY + (headerH - 8) / 2;
        graphics.drawString(this.font, Component.translatable("xam.screen.requirement_edit.title").getString(), containerX + 15, titleY, TEXT_PRIMARY, false);
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

        drawFlatButton(graphics, startX, btnY, btnW, btnH, Component.translatable("xam.editor.cancel").getString(), mouseX, mouseY, true);
        drawFlatButton(graphics, startX + btnW + 10, btnY, btnW, btnH, Component.translatable("xam.editor.save").getString(), mouseX, mouseY, true, true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Labels
        graphics.drawString(this.font, Component.translatable("xam.screen.requirement_edit.name").getString(), nameX, nameY - 11, TEXT_MUTED, false);
        graphics.drawString(this.font, Component.translatable("xam.screen.requirement_edit.description").getString(), nameX, descY - 11, TEXT_MUTED, false);

        // Input Background Panels — highlight red if validation failed and field is empty
        boolean nameEmpty = nameEdit != null && nameEdit.getValue().trim().isEmpty();
        boolean descEmpty = descEdit != null && descEdit.getValue().trim().isEmpty();
        int nameBorder = (errorMsg != null && nameEmpty) ? 0xFFFF5555 : BORDER_STANDARD;
        int descBorder = (errorMsg != null && descEmpty) ? 0xFFFF5555 : BORDER_STANDARD;
        drawFlatPanel(graphics, nameX, nameY, nameW, 20, INPUT_BACKGROUND, nameBorder);
        drawFlatPanel(graphics, nameX, descY, nameW, 20, INPUT_BACKGROUND, descBorder);

        // Tipo & ID section
        graphics.drawString(this.font, Component.translatable("xam.screen.requirement_edit.target_id").getString(), idX, idY - 11, TEXT_MUTED, false);

        // Draw Tipo Button
        String typeLabel = Component.translatable("xam.screen.requirement_edit.type_format", requirement.getType().toUpperCase()).getString();
        drawFlatButton(graphics, typeX, typeY, typeW, 20, typeLabel, mouseX, mouseY, true);

        // Draw ID Panel — highlight red if empty
        boolean idEmpty = idEdit != null && idEdit.getValue().trim().isEmpty();
        int idBorder = (errorMsg != null && idEmpty) ? 0xFFFF5555 : BORDER_STANDARD;
        drawFlatPanel(graphics, idX, idY, idW, 20, INPUT_BACKGROUND, idBorder);

        // Draw Cambiar Objetivo Button
        drawFlatButton(graphics, changeBtnX, changeBtnY, changeBtnW, 20, Component.translatable("xam.screen.requirement_edit.btn.change").getString(), mouseX, mouseY, true);

        // Dependencias section
        graphics.drawString(this.font, Component.translatable("xam.screen.requirement_edit.dependencies").getString(), depsX, depsY - 11, TEXT_MUTED, false);
        drawFlatPanel(graphics, depsX, depsY, depsEditW, 20, INPUT_BACKGROUND, BORDER_STANDARD);

        boolean depsBtnHovered = mouseX >= depsBtnX && mouseX < depsBtnX + 20 && mouseY >= depsY && mouseY < depsY + 20;
        int depsBtnBg = depsBtnHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
        int depsBtnBorder = depsBtnHovered ? COLOR_BRASS : 0xFF2C221D;
        drawFlatPanel(graphics, depsBtnX, depsY, 20, 20, depsBtnBg, depsBtnBorder);
        graphics.drawCenteredString(this.font, "...", depsBtnX + 10, depsY + 6, TEXT_PRIMARY);

        // Effect section (if type is kill)
        if (requirement.getType().equals("kill")) {
            graphics.drawString(this.font, "Efecto (Poción)", effectX, effectY - 11, TEXT_MUTED, false);
            drawFlatPanel(graphics, effectX, effectY, effectEditW, 20, INPUT_BACKGROUND, BORDER_STANDARD);

            boolean effectBtnHovered = mouseX >= effectBtnX && mouseX < effectBtnX + 20 && mouseY >= effectY && mouseY < effectY + 20;
            int effectBtnBg = effectBtnHovered ? COLOR_COPPER_HOVER : COLOR_COPPER;
            int effectBtnBorder = effectBtnHovered ? COLOR_BRASS : 0xFF2C221D;
            drawFlatPanel(graphics, effectBtnX, effectY, 20, 20, effectBtnBg, effectBtnBorder);
            graphics.drawCenteredString(this.font, "...", effectBtnX + 10, effectY + 6, TEXT_PRIMARY);
        }
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
                        this, parentPathId, ConfigManager.PATHS,
                        new ArrayList<>(requirement.getDependencies()), deps -> {
                    requirement.clearDependencies();
                    requirement.getDependencies().addAll(deps);
                    if (dependenciesEdit != null) dependenciesEdit.setValue(String.join(", ", deps));
                    Minecraft.getInstance().setScreen(this);
                }));
                return true;
            }

            // 4. Click Efecto "..." -> Open effect selection screen
            if (requirement.getType().equals("kill") && mouseX >= effectBtnX && mouseX < effectBtnX + 20 && mouseY >= effectY && mouseY < effectY + 20) {
                playClickSound();
                saveFields();
                Minecraft.getInstance().setScreen(new EffectSelectionScreen(this, effect -> {
                    ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect);
                    if (rl != null) {
                        String currentVal = effectEdit != null ? effectEdit.getValue() : "";
                        String currentLevel = "1";
                        if (currentVal.contains(" ")) {
                            String[] split = currentVal.split(" ");
                            currentLevel = split[split.length - 1];
                        }
                        requirement.setEffect(rl.toString() + " " + currentLevel);
                        if (effectEdit != null) {
                            effectEdit.setValue(requirement.getEffect());
                        }
                    }
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
                boolean nameOk = !requirement.getName().trim().isEmpty();
                boolean descOk = !requirement.getDescription().trim().isEmpty();
                boolean idOk = !requirement.getId().trim().isEmpty();
                if (!nameOk || !descOk || !idOk) {
                    errorMsg = Component.translatable("xam.screen.requirement_edit.error_empty_fields").getString();
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
        String currentId = req.getId();
        String currentName = req.getName();

        if (req.getType().equals("craft")) {
            req.setType("collect");
            if (isItem(currentId)) {
                req.setDescription(Component.translatable("xam.editor.desc.collect", currentName).getString());
            } else {
                req.setId("minecraft:dirt");
                req.setName(Component.translatable("xam.editor.default.collect_dirt_name").getString());
                req.setDescription(Component.translatable("xam.editor.default.collect_dirt_desc").getString());
            }
        } else if (req.getType().equals("collect")) {
            req.setType("kill");
            req.setId("minecraft:zombie");
            req.setName("Zombie");
            req.setDescription(Component.translatable("xam.editor.desc.kill", req.getName()).getString());
        } else if (req.getType().equals("kill")) {
            req.setType("advancement");
            req.setId("minecraft:story/root");
            req.setName("Minecraft");
            req.setDescription(Component.translatable("xam.editor.desc.advancement", req.getName()).getString());
        } else {
            req.setType("craft");
            if (isItem(currentId)) {
                req.setDescription(Component.translatable("xam.editor.desc.craft", currentName).getString());
            } else {
                req.setId("minecraft:dirt");
                req.setName(Component.translatable("xam.editor.default.craft_dirt_name").getString());
                req.setDescription(Component.translatable("xam.editor.default.craft_dirt_desc").getString());
            }
        }
        this.clearWidgets();
        this.init(this.minecraft, this.width, this.height);
    }

    private boolean isItem(String id) {
        if (id == null || id.isEmpty()) return false;
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(net.minecraft.resources.ResourceLocation.tryParse(id));
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
                mc.setScreen(this);
            }));
        }
    }
}
