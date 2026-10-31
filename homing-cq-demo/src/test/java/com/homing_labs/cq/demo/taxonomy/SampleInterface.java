package com.homing_labs.cq.demo.taxonomy;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An interface with every object of the taxonomy an interface can have:
 * <ul>
 *     <li>type parameter: {@code T}; superinterface: {@link Supplier}</li>
 *     <li>static initialiser: for {@code NAMES}, whose initialiser is not a constant</li>
 *     <li>class object - fields: {@code NAMES} and {@code OK}; implementation: {@code known}; member class:
 *     {@code Names}</li>
 *     <li>instances - declaration: {@code name}; default implementations: {@code get}, and {@code as}, generic in
 *     what it makes, through a function that takes {@code ? super} this and gives {@code ? extends} it;
 *     implementation, private: {@code key}</li>
 *     <li>in its code - local class: the enum {@code Casing}; anonymous class: a {@link Supplier}; both in
 *     {@code get}</li>
 *     <li>dependencies - in signatures, an annotation ({@link SampleAnnotation}), in code, in local variables, in
 *     nesting, a permitted subclass ({@link SampleAbstractClass}) and an inlined constant
 *     ({@link HttpURLConnection#HTTP_OK})</li>
 * </ul>
 */
@SampleAnnotation("interface")
public sealed interface SampleInterface<T> extends Supplier<String> permits SampleAbstractClass {

    List<String> NAMES = List.of("ada", "alan");

    int OK = HttpURLConnection.HTTP_OK;

    static boolean known(final String name) {
        return NAMES.contains(name);
    }

    String name();

    @Override
    default String get() {
        enum Casing {
            LOWER
        }
        final Supplier<String> keyed = new Supplier<>() {
            @Override
            public String get() {
                return key();
            }
        };
        return Casing.LOWER.ordinal() == 0 ? keyed.get() : name();
    }

    default <R> R as(final Function<? super SampleInterface<T>, ? extends R> making) {
        return making.apply(this);
    }

    private String key() {
        return name().toLowerCase();
    }

    class Names {
    }
}
