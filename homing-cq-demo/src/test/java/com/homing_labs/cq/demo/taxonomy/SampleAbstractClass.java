package com.homing_labs.cq.demo.taxonomy;

import java.net.HttpURLConnection;
import java.util.function.Supplier;

/**
 * An abstract class with every object of the taxonomy an abstract class can have:
 * <ul>
 *     <li>type parameter: {@code T}; superclass: {@link Object}, which its source does not name; superinterface:
 *     {@link SampleInterface}, given {@code T}</li>
 *     <li>constructor: one, which declares what it throws; static initialiser: the static block</li>
 *     <li>class object - fields: {@code PREFIX} and {@code OK}; implementation: {@code prefixed}; native
 *     implementation: {@code ticks}; member class: {@code Registry}</li>
 *     <li>instances - field: {@code id}; declaration: {@code size}; implementations: {@code label}, and
 *     {@code require}, which throws its own type parameter; native implementation: {@code hash}; member class:
 *     {@code Part}</li>
 *     <li>in its code - local class: the interface {@code Labeller}; anonymous class: one that implements it; both in
 *     {@code label}</li>
 *     <li>dependencies - in signatures, an annotation ({@link SampleAnnotation}), in code, in local variables, in
 *     nesting, a permitted subclass ({@link SampleClass}) and an inlined constant
 *     ({@link HttpURLConnection#HTTP_OK})</li>
 * </ul>
 */
@SampleAnnotation("abstract class")
public abstract sealed class SampleAbstractClass<T> implements SampleInterface<T> permits SampleClass {

    static final String PREFIX;

    static final int OK = HttpURLConnection.HTTP_OK;

    static {
        PREFIX = "sample-";
    }

    protected final String id;

    protected SampleAbstractClass(final String id) throws IllegalArgumentException {
        if (id.isBlank()) {
            throw new IllegalArgumentException("blank id");
        }
        this.id = id;
    }

    static String prefixed(final String text) {
        return PREFIX + text;
    }

    static native long ticks();

    abstract int size();

    String label() {
        interface Labeller {
            String label(String text);
        }
        final Labeller labeller = new Labeller() {
            @Override
            public String label(final String text) {
                return prefixed(text);
            }
        };
        return labeller.label(id);
    }

    <X extends Exception> void require(final boolean holds, final Supplier<? extends X> failure) throws X {
        if (!holds) {
            throw failure.get();
        }
    }

    native int hash();

    static class Registry {
    }

    class Part {
    }
}
