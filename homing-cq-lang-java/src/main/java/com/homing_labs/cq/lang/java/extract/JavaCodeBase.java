package com.homing_labs.cq.lang.java.extract;

import hue.captains.singapura.tao.ontology.Scalar;

import java.net.URI;
import java.util.Optional;

/**
 * Where a module's Java code is: its compiled classes - a directory or a jar - and the directory of its sources, if
 * it has one. Only what is under {@code classes} is read, never a dependency's classes.
 */
public record JavaCodeBase(URI classes, Optional<URI> sources) implements Scalar {
}
