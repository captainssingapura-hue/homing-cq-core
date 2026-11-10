package com.homing_labs.cq.lang.java.def;

import hue.captains.singapura.tao.ontology.Scalar;

import java.lang.reflect.Field;

/**
 * A field as a value: the class that declares it, its name, and the type it is declared with. Unlike a reflected
 * {@link Field}, it cannot be changed.
 */
public record JavaField(Class<?> owner, String name, Class<?> type) implements Scalar {

    /** The field a reflected one is. */
    public JavaField(final Field field) {
        this(field.getDeclaringClass(), field.getName(), field.getType());
    }
}
