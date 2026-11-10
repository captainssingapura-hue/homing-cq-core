package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Whether a method holds a lock while it runs: a synchronised one holds its object's monitor - or its class's, when
 * it is static. Synchronised blocks in a body are code, not this.
 */
public sealed interface Synchronisation extends Stateless, Scalar {

    record Unsynchronised() implements Synchronisation {
        public static final Unsynchronised INSTANCE = new Unsynchronised();
    }

    record Synchronised() implements Synchronisation {
        public static final Synchronised INSTANCE = new Synchronised();
    }
}
