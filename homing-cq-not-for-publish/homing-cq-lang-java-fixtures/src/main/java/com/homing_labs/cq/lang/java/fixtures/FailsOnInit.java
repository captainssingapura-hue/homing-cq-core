package com.homing_labs.cq.lang.java.fixtures;

/** A class that throws when initialised: reading it must load it without initialising it. */
public class FailsOnInit {

    static {
        if (true) {
            throw new IllegalStateException("FailsOnInit was initialised");
        }
    }
}
