package com.homing_labs.cq.demo.principle;

import com.homing_labs.cq.core.principle.def.Principle;

/**
 * A piece of code - a class, a method - is small enough to be read, and held in mind, whole.
 */
public record SmallEnoughToReadWhole() implements Principle {

    public static final SmallEnoughToReadWhole INSTANCE = new SmallEnoughToReadWhole();
}
