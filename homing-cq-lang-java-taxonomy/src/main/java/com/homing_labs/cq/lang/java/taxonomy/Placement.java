package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An object a kind of class can have, and how many of it a class of that kind has.
 */
public record Placement(ObjectKind object, Cardinality cardinality) implements Scalar {
}
