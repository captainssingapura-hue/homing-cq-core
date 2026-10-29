package com.homing_labs.cq.demo.samples;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds state statically: a counter anyone can change, and final fields whose contents can change.
 */
public class GlobalCounter {

    static int count;
    public static final List<String> SEEN = new ArrayList<>();
    private static final int[] TABLE = {1, 2, 3};
    private static final String UNIT = "times";

    public String next() {
        count++;
        SEEN.add(UNIT);
        return count * TABLE.length + " " + UNIT;
    }
}
