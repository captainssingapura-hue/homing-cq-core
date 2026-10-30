package com.homing_labs.cq.demo.taxonomy;

import java.util.List;
import java.util.function.Supplier;

/**
 * An interface with every object of the taxonomy an interface can have:
 * <ul>
 *     <li>superinterface: {@link Supplier}</li>
 *     <li>static initialiser: for {@code NAMES}, whose initialiser is not a constant</li>
 *     <li>class object - field: {@code NAMES}; implementation: {@code known}; member class: {@code Names}</li>
 *     <li>instances - declaration: {@code name}; default implementation: {@code get}; implementation, private:
 *     {@code key}</li>
 * </ul>
 */
public interface SampleInterface extends Supplier<String> {

    List<String> NAMES = List.of("ada", "alan");

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
