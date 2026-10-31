package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Whether a subclass can override a method - or, for a static one, hide it: a final method cannot be. A private method
 * cannot be overridden either, final or not; its visibility says so.
 */
public sealed interface Overridability extends Stateless, Scalar {

    record Overridable() implements Overridability {
        public static final Overridable INSTANCE = new Overridable();
    }

    record Final() implements Overridability {
        public static final Final INSTANCE = new Final();
    }
}
