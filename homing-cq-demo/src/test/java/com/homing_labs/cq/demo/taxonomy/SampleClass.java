package com.homing_labs.cq.demo.taxonomy;

/**
 * A concrete class with every object of the taxonomy a class can have:
 * <ul>
 *     <li>superclass: {@link SampleAbstractClass}; superinterface: {@link Runnable}</li>
 *     <li>constructors: two; static initialiser: the static block</li>
 *     <li>class object - field: {@code KIND}; implementation: {@code of}; native implementation: {@code clock};
 *     member class: {@code Builder}</li>
 *     <li>instances - field: {@code size}; implementations: {@code name}, {@code size} and {@code run}; native
 *     implementation: {@code peek}; member class: {@code View}</li>
 * </ul>
 */
public class SampleClass extends SampleAbstractClass implements Runnable {

    static final String KIND;

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

    static SampleClass of(final String id) {
        return new SampleClass(id);
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
    }

    native int peek();

    static class Builder {
    }

    class View {
    }
}
