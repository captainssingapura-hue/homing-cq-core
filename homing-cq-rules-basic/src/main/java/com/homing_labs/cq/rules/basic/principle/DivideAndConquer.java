package com.homing_labs.cq.rules.basic.principle;

import com.homing_labs.cq.core.principle.def.Principle;

/**
 * Divide and conquer: work too large to be done at once is divided into parts, each small enough to be done on its
 * own, and the whole is put together from them.
 */
public record DivideAndConquer() implements Principle {

    public static final DivideAndConquer INSTANCE = new DivideAndConquer();
}
