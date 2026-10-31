package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.lang.java.fixtures.Nesting;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaNestsTest {

    private final ClassUnit unit = (ClassUnit) JavaUnits.INSTANCE.of(Nesting.class, Optional.empty());

    @Test
    void aUnitsNestedUnitsAreItsStaticThenItsInnerClasses() {
        final List<JavaNestedUnit> nested = JavaNests.INSTANCE.of(unit);
        assertEquals(List.of(JavaNestedUnit.Static.class, JavaNestedUnit.Inner.class),
                nested.stream().map(Object::getClass).toList());
        assertEquals(List.of(Nesting.class.getName() + "$StaticNested", Nesting.class.getName() + "$Member"),
                nested.stream().map(each -> each.nested().object().name().binaryName()).toList());
        assertEquals(List.of(unit, unit), nested.stream().map(JavaNestedUnit::top).toList());
    }

    @Test
    void nestedUnitsHoldToTheOntology() {
        for (final Class<?> type : List.of(JavaNestedUnit.Static.class, JavaNestedUnit.Inner.class, JavaNests.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }
}
