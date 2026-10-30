package com.homing_labs.cq.demo.taxonomy;

import java.net.HttpURLConnection;

/**
 * An abstract class with every object of the taxonomy an abstract class can have:
 * <ul>
 *     <li>superclass: {@link Object}, which its source does not name; superinterface: {@link SampleInterface}</li>
 *     <li>constructor: one; static initialiser: the static block</li>
 *     <li>class object - fields: {@code PREFIX} and {@code OK}; implementation: {@code prefixed}; native
 *     implementation: {@code ticks}; member class: {@code Registry}</li>
 *     <li>instances - field: {@code id}; declaration: {@code size}; implementation: {@code label}; native
 *     implementation: {@code hash}; member class: {@code Part}</li>
 *     <li>dependencies - in signatures, an annotation ({@link SampleAnnotation}), in code, in local variables, in
 *     nesting, a permitted subclass ({@link SampleClass}) and an inlined constant
 *     ({@link HttpURLConnection#HTTP_OK})</li>
 * </ul>
 */
@SampleAnnotation("abstract class")
public abstract sealed class SampleAbstractClass implements SampleInterface permits SampleClass {

    static final String PREFIX;

    static final int OK = HttpURLConnection.HTTP_OK;

    static {
        PREFIX = "sample-";
    }

    protected final String id;

    protected SampleAbstractClass(final String id) {
        this.id = id;
    }

    static String prefixed(final String text) {
        return PREFIX + text;
    }

    static native long ticks();

    abstract int size();

    String label() {
        return prefixed(id);
    }

    native int hash();

    static class Registry {
    }

    class Part {
    }
}
