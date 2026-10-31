package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * What a record component holds: its name, its type as it runs, its generic type as declared, when it has one, and the
 * annotations applied to it. The field, the accessor and the constructor parameter javac derives from it are members
 * of the record, each with its own value.
 */
public record RecordComponentValue(String name, TypeUse type, Optional<TypeUse> signature, Annotations annotations)
        implements Scalar {
}
