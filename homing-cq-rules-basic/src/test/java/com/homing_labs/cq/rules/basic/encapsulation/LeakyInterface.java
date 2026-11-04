package com.homing_labs.cq.rules.basic.encapsulation;

/**
 * An interface has constants and no instance field: only the class nested in it exposes one.
 */
public interface LeakyInterface {

    int LIMIT = 10;

    class Part {

        public int size;
    }
}
