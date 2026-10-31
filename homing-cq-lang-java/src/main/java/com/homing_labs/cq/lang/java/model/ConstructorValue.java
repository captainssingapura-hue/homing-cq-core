package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * What a constructor holds: its parameters as it runs, and its generic signature as declared, when it has one; who can
 * reach it, how many arguments it takes, where it came from, and the annotations applied to it and to its parameters.
 * An inner class's constructor takes its outer instance first, as the class file has it.
 */
public record ConstructorValue(Parameters parameters, Optional<ConstructorSignature> signature, Visibility visibility,
                               Arity arity, Origin origin, Annotations annotations,
                               ParameterAnnotations parameterAnnotations) implements Scalar {
}
