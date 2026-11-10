package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A value of the model with its source: where its unit's source file writes it, and what the file says of it there,
 * by the kind of value it is - or why the file writes no block of it. A rule that needs the source and finds none
 * says so in its result, as a notice.
 *
 * @param <T> the object
 */
public record WithSource<T extends Scalar>(T object, ProcessedSource<T> source) implements Scalar {
}
