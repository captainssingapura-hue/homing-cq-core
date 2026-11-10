package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Whether a field can be assigned again once set: a final field cannot; a mutable one can; and a volatile one can, with
 * every write seen by every thread that reads it after. A field cannot be both final and volatile.
 */
public sealed interface Mutability extends Stateless, Scalar {

    record Final() implements Mutability {
        public static final Final INSTANCE = new Final();
    }

    record Mutable() implements Mutability {
        public static final Mutable INSTANCE = new Mutable();
    }

    record Volatile() implements Mutability {
        public static final Volatile INSTANCE = new Volatile();
    }
}
