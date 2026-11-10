package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A type parameter, of a class or of a method or constructor: its name, and its bounds in the order they are declared.
 * One declared with no bound is bounded by {@code Object}, as the class file writes it. Its erasure is its first bound.
 */
public record TypeParameterValue(String name, TypeUses bounds) implements Scalar {
}
