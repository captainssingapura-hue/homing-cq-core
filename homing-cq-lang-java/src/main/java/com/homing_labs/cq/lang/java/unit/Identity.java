package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * Which class a unit is: its top-level class, by name, and of which kind. Nothing else - what the class has is in the
 * unit's attributes.
 */
public record Identity(ClassName name, ClassKind kind) implements Scalar {
}
