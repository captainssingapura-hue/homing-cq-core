package com.homing_labs.cq.rules.basic;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.rules.basic.enforce.NoStaticMutableVariablesEnforcer;
import com.homing_labs.cq.rules.basic.principle.NoGlobalMutableState;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Ruleset;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * The rules of {@link NoGlobalMutableState}: no static mutable variables. It declares itself, the one ruleset, for a
 * project to opt in to on its own.
 */
public record NoGlobalMutableStateRules() implements Ruleset, Declaration, StatelessFunctionalObject {

    public static final NoGlobalMutableStateRules INSTANCE = new NoGlobalMutableStateRules();

    @Override
    public String name() {
        return "no global mutable state";
    }

    @Override
    public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
        return List.of(NoStaticMutableVariablesEnforcer.INSTANCE);
    }

    @Override
    public List<Ruleset> rulesets() {
        return List.of(this);
    }
}
