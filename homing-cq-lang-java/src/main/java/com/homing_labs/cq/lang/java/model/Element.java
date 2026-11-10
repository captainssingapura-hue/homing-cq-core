package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An element of an annotation given a value: {@code value = "class"}.
 */
public record Element(String name, ElementValue value) implements Scalar {
}
