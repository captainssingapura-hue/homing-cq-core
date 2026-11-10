package com.homing_labs.cq.demo.taxonomy;

import java.net.HttpURLConnection;
import java.util.Collection;

/**
 * A concrete class with every object of the taxonomy a class can have:
 * <ul>
 *     <li>type parameter: {@code T}; superclass: {@link SampleAbstractClass}, given {@code T}; superinterface:
 *     {@link Runnable}</li>
 *     <li>constructors: three, one of them generic, with a type parameter bounded by {@code T}, and taking an array
 *     of it</li>
 *     <li>class object - fields: {@code KIND} and {@code OK}; implementations: {@code of}, and {@code count}, which
 *     takes a collection of {@code ?}; native implementation: {@code clock}; member classes: {@code Builder} and
 *     {@code Special}</li>
 *     <li>instances - field: {@code size}; implementations: {@code name}, {@code size}, {@code run}, and
 *     {@code view}, whose type is a {@code View} of the {@code SampleClass} of {@code T}; native implementation:
 *     {@code peek}; member class: {@code View}, generic, its type parameter bounded by two interfaces</li>
 *     <li>in its code - local class: {@code Step}; anonymous class: one; both in {@code run}</li>
 *     <li>dependencies - in signatures, an annotation ({@link SampleAnnotation}), in code, in local variables, in
 *     nesting, a permitted subclass ({@code Special}) and an inlined constant ({@link HttpURLConnection#HTTP_OK})</li>
 * </ul>
 * <p>Beside them, and no object of the taxonomy, how it is initialised: here its static block.</p>
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

    public <S extends T> SampleClass(final String id, final S[] seeds) {
        this(id, seeds.length);
    }

    static SampleClass<String> of(final String id) {
        return new SampleClass<>(id);
    }

    static int count(final Collection<?> items) {
        return items.size();
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

    View<String> view() {
        return new View<>();
    }

    static class Builder {
    }

    class View<V extends CharSequence & Comparable<V>> {
    }

    static final class Special extends SampleClass<String> {

        Special() {
            super("special");
        }
    }
}
