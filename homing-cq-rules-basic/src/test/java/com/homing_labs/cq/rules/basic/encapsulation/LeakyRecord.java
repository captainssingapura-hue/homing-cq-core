package com.homing_labs.cq.rules.basic.encapsulation;

/**
 * A record keeps its components, and cannot declare an instance field: only the class nested in it exposes one.
 */
public record LeakyRecord(int value) {

    public static class Part {

        public int size;
    }
}
