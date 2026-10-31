package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * What a method holds: its name, parameters, result - none for {@code void} - and the classes it throws, as it runs,
 * and its generic signature as declared, when it has one; who can reach it, whether it can be overridden, whether it
 * holds a lock while it runs, and how many arguments it takes; where it came from; the annotations applied to it and
 * to its parameters; and, for an element of an annotation type, its default value. Whether it is static, and whether
 * it is a declaration, an implementation, a default or a native one, is not here: the taxonomy's kinds of method say
 * so.
 */
public record MethodValue(String name, Parameters parameters, Optional<TypeUse> result, ClassNames thrown,
                          Optional<MethodSignature> signature, Visibility visibility, Overridability overridability,
                          Synchronisation synchronisation, Arity arity, Origin origin, Annotations annotations,
                          ParameterAnnotations parameterAnnotations, Optional<ElementValue> defaultValue)
        implements Scalar {
}
