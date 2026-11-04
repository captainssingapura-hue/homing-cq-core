package com.homing_labs.cq.rules.basic.principle;

import com.homing_labs.cq.core.principle.def.Principle;

/**
 * An object's state is reached through its behaviour, never directly.
 */
public record Encapsulation() implements Principle {

    public static final Encapsulation INSTANCE = new Encapsulation();
}
