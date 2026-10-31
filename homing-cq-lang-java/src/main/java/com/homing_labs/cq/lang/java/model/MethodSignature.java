package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * A method's generic signature, as its class file keeps it when the method is generic or uses a generic type: its own
 * type parameters, and its parameter types and result as declared - none for {@code void}. The types it throws are
 * here only when the compiler writes them, as javac does when one of them is a type variable; otherwise they are the
 * method's thrown classes.
 */
public record MethodSignature(TypeParameters typeParameters, Parameters parameters, Optional<TypeUse> result,
                              TypeUses thrown) implements Scalar {
}
