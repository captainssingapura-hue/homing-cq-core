package com.homing_labs.cq.rules.basic.samples;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Conforms to both rules on static members, while the compiler makes static members of its own for it: lambda bodies,
 * a nested enum's values() and valueOf(String), and the switch map an enum switch needs.
 */
public final class Clean {

    static final String NAME = "clean";
    private static final int LIMIT = 3;
    private static final Mode START = Mode.ON;

    private final List<String> names = new ArrayList<>();

    public int limit() {
        return LIMIT;
    }

    public Runnable remember() {
        return () -> names.add(NAME);
    }

    public Supplier<String> name() {
        return () -> NAME;
    }

    public Mode flip(final Mode mode) {
        switch (mode) {
            case ON:
                return Mode.OFF;
            default:
                return START;
        }
    }

    /** A nested enum: its constants are static final fields of an immutable type. */
    public enum Mode {
        ON, OFF
    }
}
