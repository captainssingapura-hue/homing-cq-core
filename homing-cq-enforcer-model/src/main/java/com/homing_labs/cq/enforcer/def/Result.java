package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.Language;
import com.homing_labs.cq.core.def.Rule;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What checking one rule on one unit came to: the rule's findings, and what is worked out from them. One type for
 * every rule, so that what is worked out from findings is worked out once, here.
 */
public record Result<L extends Language, R extends Rule<?, L>>(Findings<L, R> findings) implements Scalar {

    /** Whether the rule found anything on the unit. */
    public boolean hasFindings() {
        return !findings.items().isEmpty();
    }
}
