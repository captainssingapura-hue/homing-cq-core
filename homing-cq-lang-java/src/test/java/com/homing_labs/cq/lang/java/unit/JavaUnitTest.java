package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.extract.ClassFileAttributes;
import com.homing_labs.cq.lang.java.fixtures.ARecord;
import com.homing_labs.cq.lang.java.fixtures.AnAnnotation;
import com.homing_labs.cq.lang.java.fixtures.AnEnum;
import com.homing_labs.cq.lang.java.fixtures.AnInterface;
import com.homing_labs.cq.lang.java.fixtures.Nesting;
import com.homing_labs.cq.lang.java.fixtures.Plain;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassInitialisation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.AbstractList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaUnitTest {

    /** How a class is initialised that runs no code, its source not known. */
    private static final WithSource<ClassInitialisation> NOTHING =
            new WithSource<>(ClassInitialisation.None.INSTANCE, new ProcessedSource.Unavailable<>());

    @Test
    void aUnitIsOfItsClassKind() {
        assertEquals(List.of(ClassUnit.class, AbstractClassUnit.class, InterfaceUnit.class, EnumUnit.class,
                        RecordUnit.class, AnnotationUnit.class),
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
    void aUnitHoldsItsIdentityItsAttributesAndItsSource() {
        final JavaSource source = new JavaSource(URI.create("file:///Plain.java"), "class Plain {}");
        final JavaUnit unit = JavaUnits.INSTANCE.of(Plain.class, Optional.of(source));
        assertEquals(new Identity(new ClassName(Plain.class.getName()), ClassKind.Class.INSTANCE), unit.identity());
        assertEquals(objects(ClassFileAttributes.INSTANCE.of(Plain.class)), objects(unit.attributes()),
                "the values its class files hold");
        assertEquals(Optional.of(source), unit.source());
        assertEquals("class Plain", assertInstanceOf(ProcessedSource.Implied.class,
                unit.attributes().entry(Constructor.INSTANCE).all().getFirst().source()).by().text(),
                "each with its source: the default constructor is implied by the class's header");
        assertEquals(new WithSource<>(ClassInitialisation.None.INSTANCE, new ProcessedSource.Unwritten<>()),
                unit.initialisation(), "beside them, how it is initialised: no code runs, and none is written");
        assertEquals(ClassFileAttributes.INSTANCE.of(Plain.class), JavaUnits.INSTANCE.of(Plain.class,
                Optional.empty()).attributes(), "a unit read without its source keeps its attributes as they are");
        assertEquals(NOTHING, JavaUnits.INSTANCE.of(Plain.class, Optional.empty()).initialisation());
    }

    private List<List<Scalar>> objects(final Attributes attributes) {
        return attributes.entries().stream()
                .map(entry -> entry.all().stream().<Scalar>map(WithSource::object).toList())
                .toList();
    }

    @Test
    void aUnitRefusesAClassOfAnotherKind() {
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new EnumUnit(Plain.class, identity(Plain.class, ClassKind.Enum.INSTANCE),
                        ClassFileAttributes.INSTANCE.of(AnEnum.class), NOTHING, Optional.empty()));
        assertEquals("com.homing_labs.cq.lang.java.fixtures.Plain is a class, not an enum", refused.getMessage());
    }

    @Test
    void aUnitRefusesTheIdentityOfAnotherClass() {
        assertThrows(IllegalArgumentException.class, () -> new ClassUnit(Plain.class,
                identity(Nesting.class, ClassKind.Class.INSTANCE), ClassFileAttributes.INSTANCE.of(Plain.class),
                NOTHING, Optional.empty()));
    }

    @Test
    void aUnitRefusesTheAttributesOfAnotherKind() {
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new ClassUnit(Plain.class, identity(Plain.class, ClassKind.Class.INSTANCE),
                        ClassFileAttributes.INSTANCE.of(AnEnum.class), NOTHING, Optional.empty()));
        assertEquals("The attributes are those of an enum, but com.homing_labs.cq.lang.java.fixtures.Plain is a"
                + " class", refused.getMessage());
    }

    @Test
    void aUnitRefusesANestedClass() {
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new ClassUnit(Nesting.Member.class, identity(Nesting.Member.class, ClassKind.Class.INSTANCE),
                        ClassFileAttributes.INSTANCE.of(Nesting.Member.class), NOTHING, Optional.empty()));
        assertEquals("A JavaUnit is a top-level class, but com.homing_labs.cq.lang.java.fixtures.Nesting$Member"
                + " is nested in com.homing_labs.cq.lang.java.fixtures.Nesting", refused.getMessage());
    }

    @Test
    void unitsHoldToTheOntology() {
        for (final Class<?> type : List.of(Identity.class, ClassUnit.class, AbstractClassUnit.class,
                InterfaceUnit.class, EnumUnit.class, RecordUnit.class, AnnotationUnit.class, JavaUnits.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }

    private Identity identity(final Class<?> type, final ClassKind kind) {
        return new Identity(new ClassName(type.getName()), kind);
    }
}
