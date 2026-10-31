package com.homing_labs.cq.lang.java.extract;

/** A record with a compact constructor, and the members javac adds to it. */
record PlacedRecord(int count, String... names) {

    PlacedRecord {
        names = names.clone();
    }
}
