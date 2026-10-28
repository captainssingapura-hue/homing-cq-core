package com.homing_labs.cq.lang.java.def;

import hue.captains.singapura.tao.ontology.ValueObject;

import java.net.URI;

/**
 * A Java source file: where it is, and its text.
 */
public record JavaSource(URI file, String text) implements ValueObject {
}
