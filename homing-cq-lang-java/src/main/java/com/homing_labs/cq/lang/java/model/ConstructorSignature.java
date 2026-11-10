package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A constructor's generic signature, as its class file keeps it when the constructor is generic or uses a generic type:
 * its own type parameters, and its parameter types as declared. javac leaves out the parameters it adds to the
 * constructor itself - an inner class's outer instance, an enum's name and ordinal - so these need not line up with
 * the constructor's parameters. The types it throws are here only when the compiler writes them, as javac does when
 * one of them is a type variable.
 */
public record ConstructorSignature(TypeParameters typeParameters, Parameters parameters, TypeUses thrown)
        implements Scalar {
}
