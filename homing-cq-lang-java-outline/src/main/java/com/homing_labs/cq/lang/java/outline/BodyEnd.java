package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/** How a type's body ends: closed by its brace, opened and never closed, or never opened at all. */
public sealed interface BodyEnd extends Stateless, Scalar {

    record Closed() implements BodyEnd {
        public static final Closed INSTANCE = new Closed();
    }

    /** A brace opens the body, and the file ends before the brace that would close it. */
    record Unterminated() implements BodyEnd {
        public static final Unterminated INSTANCE = new Unterminated();
    }

    /** No brace follows the header: the body is empty. */
    record Missing() implements BodyEnd {
        public static final Missing INSTANCE = new Missing();
    }
}
