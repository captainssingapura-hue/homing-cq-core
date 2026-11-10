package com.homing_labs.cq.rules.basic;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.rules.basic.enforce.NoStaticMethodsEnforcer;
import com.homing_labs.cq.rules.basic.principle.BehaviourLivesOnObjects;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Ruleset;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * The rules of {@link BehaviourLivesOnObjects}: no static methods. It declares itself, the one ruleset, for a project
 * to opt in to on its own.
 */
public record BehaviourLivesOnObjectsRules() implements Ruleset, Declaration, StatelessFunctionalObject {

    public static final BehaviourLivesOnObjectsRules INSTANCE = new BehaviourLivesOnObjectsRules();

    @Override
    public String name() {
        return "behaviour lives on objects";
    }

    @Override
    public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
        return List.of(NoStaticMethodsEnforcer.INSTANCE);
    }

    @Override
    public List<Ruleset> rulesets() {
        return List.of(this);
    }
}
