package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What is selected on the bench: a unit, or one of the classes nested in it.
 */
public record Selection(JavaUnit unit, Class<?> type) implements Scalar {
}
