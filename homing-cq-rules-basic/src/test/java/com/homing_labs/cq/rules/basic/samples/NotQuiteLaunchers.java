package com.homing_labs.cq.rules.basic.samples;

/**
 * Static methods called {@code main} that are not the one a program starts from, each differing from it in one way,
 * and a static method beside one that is.
 */
public class NotQuiteLaunchers {

    public static class NotPublic {

        static void main(final String[] args) {
        }
    }

    public static class NotVoid {

        public static int main(final String[] args) {
            return 0;
        }
    }

    public static class NoArguments {

        public static void main() {
        }
    }

    public static class NotStrings {

        public static void main(final int[] args) {
        }
    }

    public static class Beside {

        public static void main(final String[] args) {
        }

        public static void helper() {
        }
    }
}
