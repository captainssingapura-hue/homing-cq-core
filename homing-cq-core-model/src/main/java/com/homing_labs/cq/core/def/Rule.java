package com.homing_labs.cq.core.def;

import com.homing_labs.cq.core.principle.def.Principle;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * A rule to be applied to a set of Languages
 */
public interface Rule<P extends Principle, L extends Language> extends Stateless {

}
