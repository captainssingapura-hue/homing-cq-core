package com.homing_labs.cq.rules.basic.rule;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.principle.BehaviourLivesOnObjects;

/**
 * {@link BehaviourLivesOnObjects} in Java: a class declares no static method, but for the
 * {@code public static void main(String[])} a program starts from.
 */
public record NoStaticMethods() implements Rule<BehaviourLivesOnObjects, JavaLang> {

    public static final NoStaticMethods INSTANCE = new NoStaticMethods();
}
