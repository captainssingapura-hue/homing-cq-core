package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/** The type whose body is read: the word it is declared with, and its name. */
record OwnType(TypeKeyword keyword, String name) implements Scalar {

    /** Whether it is an interface, an annotation interface included. */
    boolean interfaceLike() {
        return keyword instanceof TypeKeyword.Interface || keyword instanceof TypeKeyword.Annotation;
    }
}
