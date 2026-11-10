package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * A type as a declaration uses it: a primitive, a class by name, a class given type arguments, a type variable, or an
 * array of any of them. A class file runs on erased types - a primitive, a class by name, or an array of either - and
 * keeps the generic ones as declared in its signatures.
 */
public sealed interface TypeUse extends Scalar {

    /** A primitive type. */
    record Primitive(PrimitiveType type) implements TypeUse {
    }

    /**
     * A class, interface, enum, record or annotation, by name, with no type arguments: one that is not generic, a
     * generic one used raw, or the erasure of one that is given type arguments.
     */
    record Named(ClassName name) implements TypeUse {
    }

    /**
     * A generic class given type arguments: {@code List<String>}. A class nested in a generic class it is an inner
     * class of is owned by that class with its own type arguments, {@code Outer<T>.Inner<U>}, and then may have none
     * of its own.
     */
    record Parameterized(ClassName name, TypeArguments arguments, Optional<Parameterized> owner)
            implements TypeUse {
    }

    /**
     * A type variable, by the name of the type parameter it stands for. That is declared by the class, by a class it is
     * nested in, or by the method or constructor that declares it or whose code declares its class.
     */
    record Variable(String name) implements TypeUse {
    }

    /** An array, of its component type. */
    record Array(TypeUse component) implements TypeUse {
    }
}
