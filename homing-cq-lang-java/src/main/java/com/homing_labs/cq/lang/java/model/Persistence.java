package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Whether a field is part of the persistent state of its object: a transient field is not, and serialisation leaves
 * it out.
 */
public sealed interface Persistence extends Stateless, Scalar {

    record Persistent() implements Persistence {
        public static final Persistent INSTANCE = new Persistent();
    }

    record Transient() implements Persistence {
        public static final Transient INSTANCE = new Transient();
    }
}
