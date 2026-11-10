package com.homing_labs.cq.rules.basic.enforce;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * One line of a source file, in the effective lines {@link EffectiveLines} counts it as.
 */
public record LineCount(int effectiveLines) implements Scalar {
}
