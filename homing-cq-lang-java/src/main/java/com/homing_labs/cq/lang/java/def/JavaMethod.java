package com.homing_labs.cq.lang.java.def;

import hue.captains.singapura.tao.ontology.Scalar;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A method as a value: the class that declares it, its name, and its parameter types, which tell overloads apart.
 * Unlike a reflected {@link Method}, it cannot be changed.
 */
public record JavaMethod(Class<?> owner, String name, JavaTypes parameters) implements Scalar {

    /** The method of this class and name that takes these parameter types, in order. */
    public JavaMethod(final Class<?> owner, final String name, final Class<?>... parameters) {
        this(owner, name, new JavaTypes(Arrays.stream(parameters).map(JavaType::new).toList()));
    }

    /** The method a reflected one is. */
    public JavaMethod(final Method method) {
        this(method.getDeclaringClass(), method.getName(), method.getParameterTypes());
    }
}
