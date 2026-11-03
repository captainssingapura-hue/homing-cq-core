package com.homing_labs.cq.lang.java.extract;

/**
 * A record with a compact constructor and one of its accessors written, and the members the language declares for it.
 */
record PlacedRecord(int count, String... names) {

    PlacedRecord {
        names = names.clone();
    }

    public String[] names() { return names.clone(); }
}
