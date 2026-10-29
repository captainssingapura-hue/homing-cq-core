package com.homing_labs.cq.demo.principle;

import com.homing_labs.cq.core.principle.def.Principle;

/**
 * Behaviour lives on objects: what a program does is done by an object's methods, so it can be passed, replaced and
 * held to a contract like any other object.
 */
public record BehaviourLivesOnObjects() implements Principle {

    public static final BehaviourLivesOnObjects INSTANCE = new BehaviourLivesOnObjects();
}
