package com.homing_labs.cq.demo.taxonomy;

/**
 * An abstract class with every object of the taxonomy an abstract class can have:
 * <ul>
 *     <li>superclass: {@link Object}, which its source does not name; superinterface: {@link SampleInterface}</li>
 *     <li>constructor: one; static initialiser: the static block</li>
 *     <li>class object - field: {@code PREFIX}; implementation: {@code prefixed}; native implementation:
 *     {@code ticks}; member class: {@code Registry}</li>
 *     <li>instances - field: {@code id}; declaration: {@code size}; implementation: {@code label}; native
 *     implementation: {@code hash}; member class: {@code Part}</li>
 * </ul>
 */
public abstract class SampleAbstractClass implements SampleInterface {

    static final String PREFIX;

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
