package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/** Tokens by their indices in the token stream: from the first up to, not including, the end. */
public record TokenRange(int from, int to) implements Scalar {

    /** Whether the range holds no token. */
    public boolean isEmpty() {
        return from >= to;
    }

    /** Whether the token at the index is in the range. */
    public boolean contains(final int index) {
        return index >= from && index < to;
    }
}
