package com.homing_labs.cq.lang.java.extract;

import hue.captains.singapura.tao.ontology.Scalar;

import java.net.URI;

/**
 * A directory of Java sources, laid out by package: a class of package {@code a.b} is declared in a file under
 * {@code a/b} there.
 */
public record SourceDirectory(URI location) implements Scalar {
}
