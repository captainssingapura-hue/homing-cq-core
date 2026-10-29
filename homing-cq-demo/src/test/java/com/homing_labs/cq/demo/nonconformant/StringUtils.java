package com.homing_labs.cq.demo.nonconformant;

/**
 * The classic utility class: nothing but static methods.
 */
public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(final String text) {
        return text == null || text.isBlank();
    }

    public static String capitalise(final String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
