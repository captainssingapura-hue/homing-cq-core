package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.def.JavaSource;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * A value of the model with its source, when the source is at hand: the part of the file that declares it, once the
 * parser can cut it out. A rule that needs the source and finds none says so in its result, as a notice.
 *
 * @param <T> the object
 */
public record WithSource<T extends Scalar>(T object, Optional<JavaSource> source) implements Scalar {
}
