package com.homing_labs.cq.rules.basic.nonconformant;

import java.util.HashMap;
import java.util.Map;

/**
 * An enum with a lookup table and a static lookup method of its own - beside values() and valueOf(String), which the
 * compiler makes and the rules do not count.
 */
public enum Palette {
    RED, GREEN, BLUE;

    private static final Map<String, Palette> BY_NAME = new HashMap<>();

    static {
        for (final Palette colour : values()) {
            BY_NAME.put(colour.name().toLowerCase(), colour);
        }
    }

    public static Palette named(final String name) {
        return BY_NAME.get(name);
    }
}
