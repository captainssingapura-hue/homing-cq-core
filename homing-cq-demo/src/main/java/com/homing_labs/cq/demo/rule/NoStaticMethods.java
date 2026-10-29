package com.homing_labs.cq.demo.rule;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.principle.BehaviourLivesOnObjects;

/**
 * {@link BehaviourLivesOnObjects} in Java: a class declares no static method.
 */
public record NoStaticMethods() implements Rule<BehaviourLivesOnObjects, JavaLang> {

    public static final NoStaticMethods INSTANCE = new NoStaticMethods();
}
