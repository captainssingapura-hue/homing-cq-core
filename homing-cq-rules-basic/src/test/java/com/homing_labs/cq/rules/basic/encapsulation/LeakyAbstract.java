package com.homing_labs.cq.rules.basic.encapsulation;

/**
 * An abstract class that exposes a field, and an enum nested in it that does too.
 */
public abstract class LeakyAbstract {

    public int count;

    public enum Mode {
        ON;

        public int level;
    }
}
