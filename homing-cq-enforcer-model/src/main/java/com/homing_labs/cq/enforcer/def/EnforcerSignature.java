package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.QcUnit;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What an enforcer is for, as its class names it: the language, the principle and the rule it enforces, and the type of
 * unit it checks.
 */
public record EnforcerSignature(Class<?> language, Class<?> principle, Class<?> rule, Class<?> unit)
        implements Scalar {

    /** Whether it checks a unit like this one: one of its unit type, or of a type below it. */
    public boolean checks(final QcUnit<?> candidate) {
        return unit.isInstance(candidate);
    }
}
