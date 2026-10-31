package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What a local or anonymous class holds: its name, where in its class's code it is declared, and its own attributes -
 * of its own kind, and covering only itself. Like a member class, it has no identity of its own.
 */
public record EnclosedClass(ClassName name, Enclosure enclosure, Attributes attributes) implements Scalar {
}
