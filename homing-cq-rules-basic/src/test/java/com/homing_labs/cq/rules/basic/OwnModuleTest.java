package com.homing_labs.cq.rules.basic;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.extract.JavaUnitReader;
import hue.captains.singapura.tao.ontology.FunctionalObject;
import hue.captains.singapura.tao.ontology.Immutable;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Stateless;
import hue.captains.singapura.tao.ontology.ValueObject;
import hue.captains.singapura.tao.ontology.enforcer.ContractViolation;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every public type in this module held to what it says it is.
 */
class OwnModuleTest {

    @Test
    void everyPublicTypeSaysWhatItIsAndIsHeldToIt() {
        final List<String> violations = new ArrayList<>();
        for (final JavaUnit unit : JavaUnitReader.INSTANCE.read(BasicRules.class)) {
            for (final Class<?> type : unit.type().getNestMembers()) {
                if (!Modifier.isPublic(type.getModifiers())) {
                    continue;
                }
                if (type.isInterface()) {
                    if (!saysWhatItIs(type)) {
                        violations.add(type.getName() + " extends no jOntology marker");
                    }
                } else {
                    new OntologyEnforcer().enforce(type).stream().map(ContractViolation::message).forEach(violations::add);
                }
            }
        }
        assertEquals(List.of(), violations);
    }

    private boolean saysWhatItIs(final Class<?> type) {
        return Stream.of(Stateless.class, FunctionalObject.class, ValueObject.class, Immutable.class, Mutable.class)
                .anyMatch(marker -> marker.isAssignableFrom(type));
    }
}
