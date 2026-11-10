package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An annotation as it is written: its type, and the elements given a value. An element left to its default is not
 * here; the annotation type holds its default.
 */
public record Annotation(ClassName type, Elements elements) implements Scalar {
}
