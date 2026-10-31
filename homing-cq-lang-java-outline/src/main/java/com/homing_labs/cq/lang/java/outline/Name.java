package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A name as written - simple, or qualified with dots: {@code java.util.List} - and the tokens it is written with. A
 * name the source leaves out is empty, at the place it should have been.
 */
public record Name(String text, TokenRange range) implements Scalar {
}
