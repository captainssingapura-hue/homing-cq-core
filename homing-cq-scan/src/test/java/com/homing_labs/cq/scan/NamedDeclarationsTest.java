package com.homing_labs.cq.scan;

import com.homing_labs.cq.scan.declarations.SeenRules;
import com.homing_labs.cq.scan.declarations.TaxonomyCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NamedDeclarationsTest {

    private final ClassLoader loader = getClass().getClassLoader();

    @Test
    void aDeclarationWithAnInstanceIsThatInstance() {
        assertSame(TaxonomyCode.INSTANCE, NamedDeclarations.INSTANCE.of(List.of(TaxonomyCode.class.getName()), loader));
    }

    @Test
    void aDeclarationWithNoInstanceIsMade() {
        assertEquals(new SeenRules(), NamedDeclarations.INSTANCE.of(List.of(SeenRules.class.getName()), loader));
    }

    @Test
    void declarationsNamedTogetherAreComposedInOrder() {
        assertEquals(TaxonomyCode.INSTANCE.and(new SeenRules()), NamedDeclarations.INSTANCE.of(
                List.of(TaxonomyCode.class.getName(), SeenRules.class.getName()), loader));
    }

    @Test
    void noNameNoClassAndNoDeclarationAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> NamedDeclarations.INSTANCE.of(List.of(), loader));
        assertThrows(IllegalArgumentException.class,
                () -> NamedDeclarations.INSTANCE.of(List.of("no.such.Declaration"), loader));
        assertThrows(IllegalArgumentException.class,
                () -> NamedDeclarations.INSTANCE.of(List.of(String.class.getName()), loader));
    }
}
