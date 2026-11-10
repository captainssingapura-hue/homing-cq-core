package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What a member class holds: its name, and its own attributes - of its own kind, and covering only itself - with how it
 * is initialised beside them. It has no identity of its own; only the top-level class of a unit has one.
 */
public record NestedClass(ClassName name, Attributes attributes, WithSource<ClassInitialisation> initialisation)
        implements Scalar {
}
