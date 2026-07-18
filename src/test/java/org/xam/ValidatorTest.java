package org.xam;

import org.junit.jupiter.api.Test;
import org.xam.client.gui.MasteryEditorValidator;
import org.xam.config.PathInfo;
import org.xam.config.Requirement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    private static PathInfo validPath(String id, String name, String modId) {
        PathInfo p = new PathInfo();
        p.setId(id);
        p.setName(name);
        p.setModId(modId);
        return p;
    }

    private static Requirement validReq() {
        Requirement r = new Requirement();
        r.setType("advancement");
        r.setId("mekanism:achievement/elite");
        r.setName("Elite");
        r.setDescription("Reach elite tier.");
        return r;
    }

    @Test
    void emptyList_isOk() {
        assertTrue(new MasteryEditorValidator().validateAll(List.of()).ok);
    }

    @Test
    void validPath_noRequirements_isOk() {
        assertTrue(new MasteryEditorValidator().validateAll(List.of(validPath("mek", "Mekanism", "mekanism"))).ok);
    }

    @Test
    void missingName_isError() {
        assertFalse(new MasteryEditorValidator().validateAll(List.of(validPath("mek", "", "mekanism"))).ok);
    }

    @Test
    void missingModId_isError() {
        assertFalse(new MasteryEditorValidator().validateAll(List.of(validPath("mek", "Mekanism", ""))).ok);
    }

    @Test
    void placeholderModId_isError() {
        assertFalse(new MasteryEditorValidator().validateAll(List.of(validPath("mek", "Mekanism", "modid"))).ok);
    }

    @Test
    void requirementMissingId_isError() {
        PathInfo p = validPath("mek", "Mekanism", "mekanism");
        Requirement r = validReq(); r.setId(""); p.addRequirement(r);
        assertFalse(new MasteryEditorValidator().validateAll(List.of(p)).ok);
    }

    @Test
    void requirementMissingName_isError() {
        PathInfo p = validPath("mek", "Mekanism", "mekanism");
        Requirement r = validReq(); r.setName(""); p.addRequirement(r);
        assertFalse(new MasteryEditorValidator().validateAll(List.of(p)).ok);
    }

    @Test
    void requirementMissingDesc_isError() {
        PathInfo p = validPath("mek", "Mekanism", "mekanism");
        Requirement r = validReq(); r.setDescription(""); p.addRequirement(r);
        assertFalse(new MasteryEditorValidator().validateAll(List.of(p)).ok);
    }

    @Test
    void multiplePathsAllValid_isOk() {
        PathInfo p1 = validPath("botania", "Botania", "botania");
        PathInfo p2 = validPath("mek", "Mekanism", "mekanism");
        p2.addRequirement(validReq());
        assertTrue(new MasteryEditorValidator().validateAll(List.of(p1, p2)).ok);
    }
}