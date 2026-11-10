package com.homing_labs.cq.scan;

import com.homing_labs.cq.scan.declarations.DefinitionCode;
import com.homing_labs.cq.scan.declarations.InterfaceRules;
import com.homing_labs.cq.scan.declarations.SeenRules;
import com.homing_labs.cq.scan.declarations.TaxonomyCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateTest {

    private final ClassLoader loader = getClass().getClassLoader();

    @Test
    void codeWithItsSourcesThatBreaksNoRulePasses() {
        assertEquals(List.of(), Gate.INSTANCE.failures(DefinitionCode.INSTANCE.and(InterfaceRules.INSTANCE), loader));
    }

    @Test
    void eachRuleAUnitBreaksFailsIt() {
        final List<String> failures = Gate.INSTANCE.failures(TaxonomyCode.INSTANCE.and(new SeenRules()), loader);
        assertTrue(failures.contains("com.homing_labs.cq.lang.java.taxonomy.scan.ClassScanner EveryUnitIsSeen:"
                + " Seen[unit=ClassScanner]"), failures.toString());
        assertTrue(failures.contains("com.homing_labs.cq.lang.java.taxonomy.ObjectKind InterfacesAreSeen:"
                + " Seen[unit=ObjectKind]"), failures.toString());
    }

    @Test
    void aUnitReadWithoutItsSourceFails() {
        final List<String> failures = Gate.INSTANCE.failures(new Declaration() {
            @Override
            public List<String> packages() {
                return List.of("hue.captains.singapura.tao.ontology");
            }
        }.and(InterfaceRules.INSTANCE), loader);
        assertTrue(failures.contains("hue.captains.singapura.tao.ontology.Scalar: read without its source"),
                failures.toString());
    }

    @Test
    void aScanThatChecksNothingFails() {
        assertEquals(List.of("no unit read: nothing was checked"), Gate.INSTANCE.failures(new Declaration() {
            @Override
            public List<String> packages() {
                return List.of("no.such.code");
            }
        }.and(InterfaceRules.INSTANCE), loader));
        assertEquals(List.of("no rule to check by: nothing was checked"),
                Gate.INSTANCE.failures(DefinitionCode.INSTANCE, loader));
    }

    @Test
    void asAProgramTheGateFailsByThrowingAndPassesQuietly() {
        assertDoesNotThrow(() -> Gate.INSTANCE.main(new String[]{DefinitionCode.class.getName(),
                InterfaceRules.class.getName()}));
        final IllegalStateException failed = assertThrows(IllegalStateException.class, () -> Gate.INSTANCE.main(
                new String[]{TaxonomyCode.class.getName(), SeenRules.class.getName()}));
        assertFalse(failed.getMessage().isEmpty());
    }
}
