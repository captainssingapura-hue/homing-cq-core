package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/** How bad a diagnostic is: an error javac would refuse the source for, or a warning it would only point out. */
public sealed interface Severity extends Stateless, Scalar {

    record Error() implements Severity {
        public static final Error INSTANCE = new Error();
    }

    record Warning() implements Severity {
        public static final Warning INSTANCE = new Warning();
    }
}
