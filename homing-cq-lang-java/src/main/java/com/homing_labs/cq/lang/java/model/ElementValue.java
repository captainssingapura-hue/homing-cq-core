package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * The value of an element of an annotation, as the class file holds it: a constant of a primitive type, a string, an
 * enum constant, a class, another annotation, or an array of these.
 */
public sealed interface ElementValue extends Scalar {

    /** A constant of a primitive type, as Java writes it: {@code 42}, {@code true}, {@code x} for a char. */
    record OfPrimitive(PrimitiveType type, String value) implements ElementValue {
    }

    /** A string. */
    record OfString(String value) implements ElementValue {
    }

    /** An enum constant, by its enum and its name. */
    record OfEnum(ClassName type, String constant) implements ElementValue {
    }

    /** A class literal: {@code String[].class}; no type for {@code void.class}. */
    record OfClass(Optional<TypeUse> type) implements ElementValue {
    }

    /** Another annotation. */
    record OfAnnotation(Annotation annotation) implements ElementValue {
    }

    /** An array of values, in order. */
    record OfArray(ElementValues values) implements ElementValue {
    }
}
