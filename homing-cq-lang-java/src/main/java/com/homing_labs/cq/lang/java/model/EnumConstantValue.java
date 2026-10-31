package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What an enum constant holds: its name, and the annotations applied to it. Its type is its enum, and it is always
 * public, static and final, so none of that is here; its ordinal is its place among its enum's constants.
 */
public record EnumConstantValue(String name, Annotations annotations) implements Scalar {
}
