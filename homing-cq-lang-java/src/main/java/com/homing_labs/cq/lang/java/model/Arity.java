package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * How many arguments a method or a constructor takes: as many as its parameters, or, when its last parameter is
 * variable, that many or more.
 */
public sealed interface Arity extends Stateless, Scalar {

    record Fixed() implements Arity {
        public static final Fixed INSTANCE = new Fixed();
    }

    record Variable() implements Arity {
        public static final Variable INSTANCE = new Variable();
    }
}
