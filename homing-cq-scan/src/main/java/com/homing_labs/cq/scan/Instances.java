package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * The instance of a class found by its name, where no code hands one over: its public static {@code INSTANCE}, or
 * else one made with its public constructor that takes nothing.
 */
record Instances() implements StatelessFunctionalObject {

    static final Instances INSTANCE = new Instances();

    /** The instance of {@code type}, as the {@code as} it is. */
    <T> T of(final Class<?> type, final Class<T> as) {
        try {
            final Field instance = type.getField("INSTANCE");
            if (Modifier.isStatic(instance.getModifiers()) && type.isAssignableFrom(instance.getType())) {
                return as.cast(instance.get(null));
            }
        } catch (final NoSuchFieldException | IllegalAccessException noInstance) {
            // made with its constructor instead
        }
        try {
            return as.cast(type.getConstructor().newInstance());
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(type.getName() + " has neither a public static INSTANCE nor a public"
                    + " constructor that takes nothing", e);
        }
    }
}
