package com.homing_labs.cq.lang.java.def;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A Java type as a value of its own, so that types can be held in order - as {@link JavaTypes} holds a method's
 * parameters.
 */
public record JavaType(Class<?> type) implements Scalar {
}
