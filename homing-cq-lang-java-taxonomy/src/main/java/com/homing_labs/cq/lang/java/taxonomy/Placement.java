package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An object's place in a kind of class, and how many of it a class of that kind has there.
 */
public record Placement(Class<? extends MemberOf> place, Class<? extends ObjectKind> object, Cardinality cardinality)
        implements Scalar {
}
