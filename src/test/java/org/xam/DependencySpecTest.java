package org.xam;

import org.junit.jupiter.api.Test;
import org.xam.progression.DependencySpec;

import static org.junit.jupiter.api.Assertions.*;

class DependencySpecTest {

    @Test
    void nullOrEmpty_returnsNull() {
        assertNull(DependencySpec.parse(null));
        assertNull(DependencySpec.parse(""));
    }

    @Test
    void noSuffix_isMastered() {
        DependencySpec s = DependencySpec.parse("mekanism");
        assertNotNull(s);
        assertEquals("mekanism", s.pathId);
        assertEquals(DependencySpec.Kind.MASTERED, s.kind);
    }

    @Test
    void suffixMastered_isMastered() {
        DependencySpec s = DependencySpec.parse("botania:mastered");
        assertNotNull(s);
        assertEquals(DependencySpec.Kind.MASTERED, s.kind);
    }

    @Test
    void suffixCount_isCount() {
        DependencySpec s = DependencySpec.parse("el_c:3");
        assertNotNull(s);
        assertEquals("el_c", s.pathId);
        assertEquals(DependencySpec.Kind.COUNT, s.kind);
        assertEquals(3, s.value);
    }

    @Test
    void suffixPercent_isPercent() {
        DependencySpec s = DependencySpec.parse("botania:50%");
        assertNotNull(s);
        assertEquals("botania", s.pathId);
        assertEquals(DependencySpec.Kind.PERCENT, s.kind);
        assertEquals(50, s.value);
    }

    @Test
    void suffixAll_isMastered() {
        DependencySpec s = DependencySpec.parse("rama:all");
        assertNotNull(s);
        assertEquals(DependencySpec.Kind.MASTERED, s.kind);
    }

    @Test
    void unknownSuffix_defaultsToMastered() {
        DependencySpec s = DependencySpec.parse("rama:???");
        assertNotNull(s);
        assertEquals(DependencySpec.Kind.MASTERED, s.kind);
    }
}