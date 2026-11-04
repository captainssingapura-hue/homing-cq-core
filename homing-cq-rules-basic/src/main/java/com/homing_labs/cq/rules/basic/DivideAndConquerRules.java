package com.homing_labs.cq.rules.basic;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.rules.basic.enforce.MethodsWithin50LinesEnforcer;
import com.homing_labs.cq.rules.basic.enforce.TopLevelClassesWithin250LinesEnforcer;
import com.homing_labs.cq.rules.basic.principle.DivideAndConquer;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Ruleset;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * The rules of {@link DivideAndConquer}: top-level classes within 250 effective lines, methods and constructors within
 * 50. Both read sources. It declares itself, the one ruleset, for a project to opt in to on its own.
 */
public record DivideAndConquerRules() implements Ruleset, Declaration, StatelessFunctionalObject {

    public static final DivideAndConquerRules INSTANCE = new DivideAndConquerRules();

    @Override
    public String name() {
        return "divide and conquer";
    }

    @Override
    public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
        return List.of(TopLevelClassesWithin250LinesEnforcer.INSTANCE, MethodsWithin50LinesEnforcer.INSTANCE);
    }

    @Override
    public List<Ruleset> rulesets() {
        return List.of(this);
    }
}
