package com.homing_labs.cq.enforcer.def;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What a rule has to say about checking a unit beside its findings: that it could not check all it meant to, say,
 * because what it needed was not there. A notice is not a finding - the unit may well conform - but the result is not
 * complete without it.
 */
public sealed interface Notice extends Scalar {

    /** The rule needed the source of something, and it was not at hand: the class file alone was checked. */
    record SourceUnavailable(String of) implements Notice {
    }
}
