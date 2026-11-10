package com.homing_labs.cq.lang.java.fixtures;

/**
 * A top-level class with a classic switch on another class's enum, for which javac writes a class of its own, flagged
 * synthetic, to hold the switch map; and beside it an anonymous class the source declares.
 */
public class Switching {

    /** Switches on another class's enum. */
    public int of(final AnEnum constant) {
        switch (constant) {
            case PLAIN:
                return 1;
            default:
                return 0;
        }
    }

    /** Declares an anonymous class. */
    public Runnable anonymous() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }
}
