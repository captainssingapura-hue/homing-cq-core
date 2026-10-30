package com.homing_labs.cq.demo.taxonomy;

import java.util.function.IntSupplier;

/**
 * An enum with every object of the taxonomy an enum can have:
 * <ul>
 *     <li>superclass: {@link Enum}, which its source does not name; superinterface: {@link IntSupplier}</li>
 *     <li>constructor: one; static initialiser: the one every enum has, here with a static block too</li>
 *     <li>class object - fields: the constants {@code FIRST} and {@code SECOND}, and {@code COUNT}; implementations:
 *     {@code first}, beside {@code values} and {@code valueOf}; native implementation: {@code seed}; member class:
 *     {@code Table}</li>
 *     <li>instances - field: {@code order}; declaration: {@code weight}, which each constant's body implements;
 *     implementation: {@code getAsInt}; native implementation: {@code code}; member class: {@code Slot}</li>
 * </ul>
 */
public enum SampleEnum implements IntSupplier {

    FIRST(1) {
        @Override
        int weight() {
            return order * 10;
        }
    },
    SECOND(2) {
        @Override
        int weight() {
            return order;
        }
    };

    static final int COUNT;

    static {
        COUNT = values().length;
    }

    final int order;

    SampleEnum(final int order) {
        this.order = order;
    }

    static SampleEnum first() {
        return FIRST;
    }

    static native long seed();

    abstract int weight();

    @Override
    public int getAsInt() {
        return order;
    }

    native int code();

    static class Table {
    }

    class Slot {
    }
}
