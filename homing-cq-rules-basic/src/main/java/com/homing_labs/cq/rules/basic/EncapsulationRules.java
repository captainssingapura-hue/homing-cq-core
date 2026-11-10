package com.homing_labs.cq.rules.basic;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.rules.basic.enforce.NoPublicFieldsEnforcer;
import com.homing_labs.cq.rules.basic.enforce.NoPublicFieldsInNestedClassEnforcer;
import com.homing_labs.cq.rules.basic.principle.Encapsulation;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Ruleset;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * The rules of {@link Encapsulation}: no public fields, in a unit or in a class nested in it alone. It declares itself,
 * the one ruleset, for a project to opt in to on its own.
 */
public record EncapsulationRules() implements Ruleset, Declaration, StatelessFunctionalObject {

    public static final EncapsulationRules INSTANCE = new EncapsulationRules();

    @Override
    public String name() {
        return "encapsulation";
    }

    @Override
    public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
        return List.of(NoPublicFieldsEnforcer.INSTANCE, NoPublicFieldsInNestedClassEnforcer.INSTANCE);
    }

    @Override
    public List<Ruleset> rulesets() {
        return List.of(this);
    }
}
