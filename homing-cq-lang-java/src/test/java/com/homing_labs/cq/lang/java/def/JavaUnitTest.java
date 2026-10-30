package com.homing_labs.cq.lang.java.def;

import com.homing_labs.cq.lang.java.fixtures.ARecord;
import com.homing_labs.cq.lang.java.fixtures.AnAnnotation;
import com.homing_labs.cq.lang.java.fixtures.AnEnum;
import com.homing_labs.cq.lang.java.fixtures.AnInterface;
import com.homing_labs.cq.lang.java.fixtures.Nesting;
import com.homing_labs.cq.lang.java.fixtures.Plain;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.AbstractList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaUnitTest {

    @Test
    void aUnitIsOfItsClassKind() {
        assertEquals(List.of(JavaUnit.OfClass.class, JavaUnit.OfAbstractClass.class, JavaUnit.OfInterface.class,
                        JavaUnit.OfEnum.class, JavaUnit.OfRecord.class, JavaUnit.OfAnnotation.class),
                List.of(Plain.class, AbstractList.class, AnInterface.class, AnEnum.class, ARecord.class,
                        AnAnnotation.class).stream()
                        .map(type -> JavaUnits.INSTANCE.of(type, Optional.empty()).getClass())
                        .toList());
    }

    @Test
    void aUnitNamesTheKindOfItsClass() {
        assertEquals(List.of(ClassKind.Class.INSTANCE, ClassKind.Enum.INSTANCE, ClassKind.Annotation.INSTANCE),
                List.of(Plain.class, AnEnum.class, AnAnnotation.class).stream()
                        .map(type -> JavaUnits.INSTANCE.of(type, Optional.empty()).kind())
                        .toList());
    }

    @Test
    void aUnitRefusesAClassOfAnotherKind() {
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new JavaUnit.OfClass(AnEnum.class, Optional.empty()));
        assertEquals("com.homing_labs.cq.lang.java.fixtures.AnEnum is an enum, not a class", refused.getMessage());
    }

    @Test
    void aUnitRefusesANestedClass() {
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new JavaUnit.OfClass(Nesting.Member.class, Optional.empty()));
        assertEquals("A JavaUnit is a top-level class, but com.homing_labs.cq.lang.java.fixtures.Nesting$Member"
                + " is nested in com.homing_labs.cq.lang.java.fixtures.Nesting", refused.getMessage());
    }

    @Test
    void aUnitWithSourceRefusesANestedClass() {
        assertThrows(IllegalArgumentException.class, () -> new JavaUnit.OfClass(Nesting.StaticNested.class,
                Optional.of(new JavaSource(URI.create("file:///Nesting.java"), ""))));
    }
}
