package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.Language;
import com.homing_labs.cq.core.def.Rule;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * One thing a rule found on a unit: what breaks the rule, and where. Each rule declares its own findings; tied to the
 * rule by type, a finding of one rule cannot be put in the result of another.
 */
public interface Finding<L extends Language, R extends Rule<?, L>> extends Scalar {
}
