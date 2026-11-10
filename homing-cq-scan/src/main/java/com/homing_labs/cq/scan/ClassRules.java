package com.homing_labs.cq.scan;

import com.homing_labs.cq.lang.java.model.ClassName;
import hue.captains.singapura.tao.ontology.Scalar;

/** A member class nested in a unit, by its name, and the rules applied to it alone, as sub-rules are. */
public record ClassRules(ClassName name, AppliedRules applied) implements Scalar {
}
