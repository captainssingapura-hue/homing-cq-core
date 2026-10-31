package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/** The documentation comment of a declaration, by the index of its token. */
public record DocComment(int token) implements Scalar {
}
