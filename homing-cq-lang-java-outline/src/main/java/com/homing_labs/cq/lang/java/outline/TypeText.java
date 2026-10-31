package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A type as written, by its tokens: {@code Map.Entry<String, T[]>}. What it is made of - the names in it, its type
 * arguments - is left to a later stage to read.
 */
public record TypeText(TokenRange range) implements Scalar {
}
