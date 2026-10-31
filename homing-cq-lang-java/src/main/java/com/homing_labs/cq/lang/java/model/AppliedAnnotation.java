package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An annotation applied to a class, a member or a parameter, and where it is kept. An annotation nested in another's
 * value, or given as a default, is kept with what holds it, and has no retention of its own.
 */
public record AppliedAnnotation(Annotation annotation, Retention retention) implements Scalar {
}
