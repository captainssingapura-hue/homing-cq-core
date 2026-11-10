package com.homing_labs.cq.rules.basic.encapsulation;

/**
 * Keeps every field to itself, at every level.
 */
public class Tight {

    private int count;

    public static class Settings {

        private String name;
    }

    public class Cursor {

        private int position;
    }
}
