package com.homing_labs.cq.demo;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.extract.JavaUnitReader;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.scan.ClassScanner;
import com.homing_labs.cq.lang.java.taxonomy.scan.Occurrence;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The kind each unit of this module's tests is made of, read from its loaded class, against the kind the taxonomy's
 * scanner reads from its class file. The samples give units of every kind.
 */
class UnitKindsTest {

    private final List<JavaUnit> units = JavaUnitReader.INSTANCE.read(UnitKindsTest.class);

    @Test
    void everyUnitIsOfTheKindItsClassFileSays() {
        assertEquals(List.of(), units.stream()
                .filter(unit -> !unit.kind().equals(kindInClassFile(unit.type())))
                .map(unit -> unit.type().getName() + " is " + unit.kind() + " but its class file says "
                        + kindInClassFile(unit.type()))
                .toList());
    }

    @Test
    void theUnitsAreOfEveryKind() {
        assertEquals(Set.of(ClassKind.Class.INSTANCE, ClassKind.AbstractClass.INSTANCE, ClassKind.Interface.INSTANCE,
                        ClassKind.Enum.INSTANCE, ClassKind.Record.INSTANCE, ClassKind.Annotation.INSTANCE),
                units.stream().map(JavaUnit::kind).collect(Collectors.toSet()));
    }

    private ClassKind kindInClassFile(final Class<?> type) {
        return ((Occurrence.OfClass) ClassScanner.INSTANCE.scan(classFile(type)).getFirst()).kind();
    }

    private byte[] classFile(final Class<?> type) {
        try (InputStream in = type.getClassLoader().getResourceAsStream(type.getName().replace('.', '/') + ".class")) {
            return in.readAllBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
