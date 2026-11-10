package com.homing_labs.cq.rules.basic.rule;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.principle.Encapsulation;

/**
 * {@link Encapsulation} in Java: a class of any kind declares no public instance field - nor does any class nested in
 * it.
 */
public record NoPublicFields() implements Rule<Encapsulation, JavaLang> {

    public static final NoPublicFields INSTANCE = new NoPublicFields();
}
