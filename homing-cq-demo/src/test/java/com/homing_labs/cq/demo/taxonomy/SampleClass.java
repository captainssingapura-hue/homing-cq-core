package com.homing_labs.cq.demo.taxonomy;

import java.net.HttpURLConnection;

/**
 * A concrete class with every object of the taxonomy a class can have:
 * <ul>
 *     <li>type parameter: {@code T}; superclass: {@link SampleAbstractClass}, given {@code T}; superinterface:
 *     {@link Runnable}</li>
 *     <li>constructors: two; static initialiser: the static block</li>
 *     <li>class object - fields: {@code KIND} and {@code OK}; implementation: {@code of}; native implementation:
 *     {@code clock}; member classes: {@code Builder} and {@code Special}</li>
 *     <li>instances - field: {@code size}; implementations: {@code name}, {@code size} and {@code run}; native
 *     implementation: {@code peek}; member class: {@code View}</li>
 *     <li>in its code - local class: {@code Step}; anonymous class: one; both in {@code run}</li>
 *     <li>dependencies - in signatures, an annotation ({@link SampleAnnotation}), in code, in local variables, in
 *     nesting, a permitted subclass ({@code Special}) and an inlined constant ({@link HttpURLConnection#HTTP_OK})</li>
 * </ul>
 */
@SampleAnnotation("class")
public sealed class SampleClass<T> extends SampleAbstractClass<T> implements Runnable permits SampleClass.Special {

    static final String KIND;

    static final int OK = HttpURLConnection.HTTP_OK;

    static {
        KIND = "class";
    }

    private final int size;

    public SampleClass(final String id, final int size) {
        super(id);
        this.size = size;
    }

    public SampleClass(final String id) {
        this(id, 0);
    }

    static SampleClass<String> of(final String id) {
        return new SampleClass<>(id);
    }

    static native long clock();

    @Override
    public String name() {
        return id;
    }

    @Override
    int size() {
        return size;
    }

    @Override
    public void run() {
        class Step {
        }
        new Step();
        new Object() {
        };
    }

    native int peek();

    static class Builder {
    }

    class View {
    }

    static final class Special extends SampleClass<String> {

        Special() {
            super("special");
        }
    }
}
