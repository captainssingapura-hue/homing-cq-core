package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * How a class's static state is set up when the JVM initialises the class, once, before it is first used: not a part of
 * the class - no instruction can call it - but what is run for it. The class file says only whether code runs: javac
 * puts its static initialiser together from the static fields' initial values that are no constants, an enum's
 * constants and the static blocks, in the order written, and keeps no trace of where one ends and the next begins. The
 * steps are the source's.
 */
public sealed interface ClassInitialisation extends Stateless, Scalar {

    /**
     * No code runs: the class file has no static initialiser. Its static fields are constants, or keep their default
     * values.
     */
    record None() implements ClassInitialisation {
        public static final None INSTANCE = new None();
    }

    /** Code runs: the class file has a static initialiser. */
    record Code() implements ClassInitialisation {
        public static final Code INSTANCE = new Code();
    }
}
