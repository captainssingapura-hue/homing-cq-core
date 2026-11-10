package com.homing_labs.cq.demo.taxonomy;

import java.net.HttpURLConnection;
import java.util.function.IntSupplier;

/**
 * An enum with every object of the taxonomy an enum can have:
 * <ul>
 *     <li>superclass: {@link Enum}, which its source does not name; superinterface: {@link IntSupplier}</li>
 *     <li>constructor: one</li>
 *     <li>class object - fields: the constants {@code FIRST} and {@code SECOND}, {@code COUNT} and {@code OK};
 *     implementations: {@code named}, beside {@code values} and {@code valueOf}; native implementation:
 *     {@code seed}; member class: {@code Table}</li>
 *     <li>instances - field: {@code order}; declaration: {@code weight}, which each constant's body implements;
 *     implementation: {@code getAsInt}; native implementation: {@code code}; member class: {@code Slot}</li>
 *     <li>in its code - local class: the record {@code Wanted}, in {@code named}; anonymous classes: the constants'
 *     bodies, made when the enum is initialised</li>
 *     <li>dependencies - in signatures, an annotation ({@link SampleAnnotation}), in code, in local variables, in
 *     nesting, permitted subclasses (the constants' bodies) and an inlined constant
 *     ({@link HttpURLConnection#HTTP_OK})</li>
 * </ul>
 * <p>Beside them, and no object of the taxonomy, how it is initialised: making its constants, as every enum's
 * is, and here a static block too.</p>
 */
@SampleAnnotation("enum")
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

    static final int OK = HttpURLConnection.HTTP_OK;

    static {
        COUNT = values().length;
    }

    final int order;

    SampleEnum(final int order) {
        this.order = order;
    }

    static SampleEnum named(final String name) {
        record Wanted(String name) {
        }
        return valueOf(new Wanted(name.toUpperCase()).name());
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
