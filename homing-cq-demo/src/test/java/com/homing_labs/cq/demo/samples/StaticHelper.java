package com.homing_labs.cq.demo.samples;

/**
 * Declares static methods, one of them in a nested class.
 */
public class StaticHelper {

    public static int twice(final int value) {
        return value * 2;
    }

    public int thrice(final int value) {
        return value * 3;
    }

    static class Inner {

        private static String label(final Object value, final int width) {
            return String.valueOf(value).substring(0, width);
        }
    }
}
