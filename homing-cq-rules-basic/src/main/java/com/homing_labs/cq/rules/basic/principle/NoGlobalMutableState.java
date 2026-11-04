package com.homing_labs.cq.rules.basic.principle;

import com.homing_labs.cq.core.principle.def.Principle;

/**
 * No state is held where every part of a program can change it: state that changes belongs to an object, and is
 * handed to what uses it.
 */
public record NoGlobalMutableState() implements Principle {

    public static final NoGlobalMutableState INSTANCE = new NoGlobalMutableState();
}
