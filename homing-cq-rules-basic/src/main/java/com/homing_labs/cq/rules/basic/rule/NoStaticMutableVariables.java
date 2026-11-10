package com.homing_labs.cq.rules.basic.rule;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.principle.NoGlobalMutableState;

/**
 * {@link NoGlobalMutableState} in Java: every static field is final, and of a type known to be immutable.
 */
public record NoStaticMutableVariables() implements Rule<NoGlobalMutableState, JavaLang> {

    public static final NoStaticMutableVariables INSTANCE = new NoStaticMutableVariables();
}
