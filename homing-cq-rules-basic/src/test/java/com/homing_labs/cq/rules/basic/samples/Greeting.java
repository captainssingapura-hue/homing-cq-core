package com.homing_labs.cq.rules.basic.samples;

/**
 * An interface with a static factory method.
 */
public interface Greeting {

    String say();

    static Greeting english() {
        return () -> "hello";
    }
}
