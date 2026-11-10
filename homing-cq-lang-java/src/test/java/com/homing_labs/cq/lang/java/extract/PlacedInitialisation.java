package com.homing_labs.cq.lang.java.extract;

/**
 * A class whose initialisation runs code its source does not write - javac sets up its {@code assert}s - and that
 * declares a local class whose initialisation is written in a body the outline does not read.
 */
class PlacedInitialisation {

    void check(final int value) {
        assert value > 0;
    }

    /** A local class whose static field's value is computed. */
    Object counter() {
        class Counter {
            static int made = Integer.parseInt("0");
        }
        return new Counter();
    }
}
