package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What a static initialiser holds, at declaration level: only where it came from.
 */
public record InitialiserValue(Origin origin) implements Scalar {
}
