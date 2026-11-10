package com.homing_labs.cq.lang.java.extract;

import hue.captains.singapura.tao.ontology.Scalar;

import java.net.URI;

/**
 * Where a module's Java code is: its compiled classes - a directory or a jar - and the directories of its sources,
 * searched in order: none, when it has none. Only what is under {@code classes} is read, never a dependency's
 * classes.
 */
public record JavaCodeBase(URI classes, SourceDirectories sources) implements Scalar {
}
