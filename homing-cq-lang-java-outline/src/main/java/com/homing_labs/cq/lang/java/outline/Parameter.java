package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/** A parameter of a method or a constructor as written: a formal one, or the receiver. */
public sealed interface Parameter extends Scalar {

    /** The tokens it is written with. */
    TokenRange range();

    /**
     * A formal parameter: its modifiers - {@code final} and annotations - its type, whether it takes any number of
     * arguments - written {@code ...} - its name, and brackets written after the name, an old form (JLS 8.4.1).
     */
    record Formal(Modifiers modifiers, TypeText type, boolean variable, Name name, TokenRange dimensions,
                  TokenRange range) implements Parameter {
    }

    /**
     * The receiver parameter, written first if at all, so that the type of the object a method is called on can be
     * annotated (JLS 8.4). Its name is {@code this}, or, in an inner class's constructor, the enclosing class's name
     * and {@code .this}.
     */
    record Receiver(Modifiers annotations, TypeText type, Name name, TokenRange range) implements Parameter {
    }
}
