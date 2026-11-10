package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * What a field holds: its name, its type as it runs, and its generic type as declared, when it has one; who can reach
 * it; whether it can be assigned again, and whether it is part of its object's persistent state; where it came from;
 * and the annotations applied to it. Whether it is static is not here: the taxonomy's {@code StaticField} and
 * {@code Field} say so.
 */
public record FieldValue(String name, TypeUse type, Optional<TypeUse> signature, Visibility visibility,
                         Mutability mutability, Persistence persistence, Origin origin, Annotations annotations)
        implements Scalar {
}
