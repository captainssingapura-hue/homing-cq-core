package com.homing_labs.cq.demo.enforce;

/**
 * A class the size rules are measured on: its own source is short, and each test gives it a source written to the
 * length it needs, with members of the same names and parameters.
 */
class Measured {

    private int counted;

    Measured(final int start) {
        counted = start;
    }

    int count() {
        return counted;
    }

    /** A member class, whose methods are checked too. */
    static class Part {

        private int size;

        void grow() {
            size++;
        }
    }
}
