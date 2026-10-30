package com.homing_labs.cq.demo.taxonomy;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.function.Supplier;

/**
 * An interface with every object of the taxonomy an interface can have:
 * <ul>
 *     <li>superinterface: {@link Supplier}</li>
 *     <li>static initialiser: for {@code NAMES}, whose initialiser is not a constant</li>
 *     <li>class object - fields: {@code NAMES} and {@code OK}; implementation: {@code known}; member class:
 *     {@code Names}</li>
 *     <li>instances - declaration: {@code name}; default implementation: {@code get}; implementation, private:
 *     {@code key}</li>
 *     <li>dependencies - in signatures, an annotation ({@link SampleAnnotation}), in code, in local variables, in
 *     nesting, a permitted subclass ({@link SampleAbstractClass}) and an inlined constant
 *     ({@link HttpURLConnection#HTTP_OK})</li>
 * </ul>
 */
@SampleAnnotation("interface")
public sealed interface SampleInterface extends Supplier<String> permits SampleAbstractClass {

    List<String> NAMES = List.of("ada", "alan");

    int OK = HttpURLConnection.HTTP_OK;

    static boolean known(final String name) {
        return NAMES.contains(name);
    }

    String name();

    @Override
    default String get() {
        return key();
    }

    private String key() {
        return name().toLowerCase();
    }

    class Names {
    }
}
