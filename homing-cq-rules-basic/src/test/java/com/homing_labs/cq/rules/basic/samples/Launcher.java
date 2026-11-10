package com.homing_labs.cq.rules.basic.samples;

/**
 * Starts a program from each place a {@code public static void main(String[])} can be declared: a class, a nested
 * class, an interface and an enum, and written with varargs, which compile to the same method.
 */
public class Launcher {

    public static void main(final String[] args) {
    }

    public static class Nested {

        public static void main(final String[] args) {
        }
    }

    public interface Contract {

        static void main(final String[] args) {
        }
    }

    public enum Choice {
        ONE;

        public static void main(final String[] args) {
        }
    }

    public static class WithVarargs {

        public static void main(final String... args) {
        }
    }
}
