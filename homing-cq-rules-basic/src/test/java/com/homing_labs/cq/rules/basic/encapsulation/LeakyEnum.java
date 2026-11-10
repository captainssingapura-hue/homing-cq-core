package com.homing_labs.cq.rules.basic.encapsulation;

/**
 * An enum that exposes a field, and an abstract class nested in it that does too. Its constants and its static constant
 * are no instance fields.
 */
public enum LeakyEnum {
    ONE, TWO;

    public static final int LIMIT = 10;

    public int count;

    public abstract static class Base {

        public int base;
    }
}
