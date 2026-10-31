package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A class of a unit, by its name, with the attributes the unit holds for it: the top-level class's own, or those of a
 * class nested in it.
 */
public record ClassAttributes(ClassName name, Attributes attributes) implements Scalar {
}
