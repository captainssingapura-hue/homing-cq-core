package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Who can reach a class or a member: anyone, its package and subclasses, its package, or its top-level class alone.
 */
public sealed interface Visibility extends Stateless, Scalar {

    record Public() implements Visibility {
        public static final Public INSTANCE = new Public();
    }

    record Protected() implements Visibility {
        public static final Protected INSTANCE = new Protected();
    }

    record Package() implements Visibility {
        public static final Package INSTANCE = new Package();
    }

    record Private() implements Visibility {
        public static final Private INSTANCE = new Private();
    }
}
