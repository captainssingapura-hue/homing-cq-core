package com.homing_labs.cq.rules.basic.nonconformant;

/**
 * Looks clean, but a class nested in it keeps a static counter - found, and placed on this top-level class.
 */
public class ReportBuilder {

    public String build(final String title) {
        Counter.built++;
        return title + " #" + Counter.built;
    }

    static class Counter {

        static int built;
    }
}
