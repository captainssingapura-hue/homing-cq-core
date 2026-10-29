package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.Language;
import com.homing_labs.cq.core.def.Rule;
import hue.captains.singapura.tao.ontology.Vector;

import java.util.Set;

/**
 * The findings of one rule on one unit, as a set: equal by its findings in any order, and copied when made, so it
 * never changes. An order, where one is wanted - to list them, say - is worked out from the findings themselves.
 */
public record Findings<L extends Language, R extends Rule<?, L>>(Set<? extends Finding<L, R>> items)
        implements Vector<Finding<L, R>> {

    public Findings {
        items = Set.copyOf(items);
    }
}
