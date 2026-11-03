package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * Declarations named by their classes, as a program's arguments name them, where no code can hand them over: each
 * class's public static {@code INSTANCE}, or else one made with its public constructor that takes nothing - composed,
 * in the order named, into one.
 */
public record NamedDeclarations() implements StatelessFunctionalObject {

    public static final NamedDeclarations INSTANCE = new NamedDeclarations();

    /** The declarations of the classes named, each loaded by {@code loader}, composed in order. */
    public Declaration of(final List<String> classNames, final ClassLoader loader) {
        return classNames.stream()
                .map(className -> declaration(className, loader))
                .reduce(Declaration::and)
                .orElseThrow(() -> new IllegalArgumentException("Name the class of at least one declaration"));
    }

    private Declaration declaration(final String className, final ClassLoader loader) {
        final Class<?> type;
        try {
            type = Class.forName(className, true, loader);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("No class " + className + " to declare what to check", e);
        }
        if (!Declaration.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(className + " is no " + Declaration.class.getName());
        }
        return Instances.INSTANCE.of(type, Declaration.class);
    }
}
