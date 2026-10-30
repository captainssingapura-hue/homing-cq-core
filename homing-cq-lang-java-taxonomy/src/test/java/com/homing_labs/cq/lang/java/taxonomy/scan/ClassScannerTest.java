package com.homing_labs.cq.lang.java.taxonomy.scan;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassScannerTest {

    @Test
    void objectIsAClassWithNoSuperclass() throws IOException {
        final List<Occurrence> found = ClassScanner.INSTANCE.scan(classFile(Object.class));
        assertEquals(new Occurrence.OfClass("java/lang/Object", ClassKind.Class.INSTANCE), found.getFirst());
        assertTrue(found.stream().noneMatch(occurrence -> occurrence.kind() instanceof Superclass));
    }

    @Test
    void occurrencesAndTheScannerHoldToTheOntology() {
        for (final Class<?> type : List.of(Occurrence.class, Occurrence.OfClass.class, Occurrence.OfMember.class,
                ClassScanner.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }

    private byte[] classFile(final Class<?> type) throws IOException {
        try (InputStream in = ClassLoader.getSystemResourceAsStream(type.getName().replace('.', '/') + ".class")) {
            return in.readAllBytes();
        }
    }
}
