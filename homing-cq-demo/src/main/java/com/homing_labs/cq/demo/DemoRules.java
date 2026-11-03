package com.homing_labs.cq.demo;

import com.homing_labs.cq.demo.enforce.MethodsWithin50LinesEnforcer;
import com.homing_labs.cq.demo.enforce.NoPublicFieldsEnforcer;
import com.homing_labs.cq.demo.enforce.NoPublicFieldsInNestedClassEnforcer;
import com.homing_labs.cq.demo.enforce.NoStaticMethodsEnforcer;
import com.homing_labs.cq.demo.enforce.NoStaticMutableVariablesEnforcer;
import com.homing_labs.cq.demo.enforce.TopLevelClassesWithin250LinesEnforcer;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Ruleset;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * The demo's rules, as one ruleset that lists its enforcers itself: no static methods, no static mutable variables,
 * no public fields - in a unit, or in a class nested in it alone - and the sizes classes and methods are written in.
 * It declares itself, the one ruleset, to compose with the code it checks, such as {@link DemoCode}.
 */
public record DemoRules() implements Ruleset, Declaration, StatelessFunctionalObject {

    public static final DemoRules INSTANCE = new DemoRules();

    @Override
    public String name() {
        return "demo rules";
    }

    @Override
    public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
        return List.of(NoStaticMethodsEnforcer.INSTANCE, NoStaticMutableVariablesEnforcer.INSTANCE,
                NoPublicFieldsEnforcer.INSTANCE, NoPublicFieldsInNestedClassEnforcer.INSTANCE,
                TopLevelClassesWithin250LinesEnforcer.INSTANCE, MethodsWithin50LinesEnforcer.INSTANCE);
    }

    @Override
    public List<Ruleset> rulesets() {
        return List.of(this);
    }
}
