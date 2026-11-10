package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Where an annotation is kept: in the class file alone, out of reflection's sight, or at runtime too. An annotation
 * kept in the source alone is not in the class file, and so is not here.
 */
public sealed interface Retention extends Stateless, Scalar {

    /** In the class file alone: {@code RetentionPolicy.CLASS}, the default. */
    record InClassFile() implements Retention {
        public static final InClassFile INSTANCE = new InClassFile();
    }

    /** At runtime too, where reflection reads it: {@code RetentionPolicy.RUNTIME}. */
    record AtRuntime() implements Retention {
        public static final AtRuntime INSTANCE = new AtRuntime();
    }
}
