package com.homing_labs.cq.rules.basic.encapsulation;

/**
 * An annotation has no instance field: only the class nested in it exposes one.
 */
public @interface LeakyAnnotation {

    class Part {

        public int size;
    }
}
