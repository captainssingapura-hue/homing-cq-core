package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Who can extend a class, or implement it: no one, only the classes it permits, or anyone. A class that extends a
 * sealed one and is neither final nor sealed is declared non-sealed, but its class file does not say so: it is open.
 */
public sealed interface Extensibility extends Stateless, Scalar {

    /** No one: a final class. */
    record Final() implements Extensibility {
        public static final Final INSTANCE = new Final();
    }

    /** Only the classes it permits: a sealed class or interface. */
    record Sealed() implements Extensibility {
        public static final Sealed INSTANCE = new Sealed();
    }

    /** Anyone it is visible to. */
    record Open() implements Extensibility {
        public static final Open INSTANCE = new Open();
    }
}
