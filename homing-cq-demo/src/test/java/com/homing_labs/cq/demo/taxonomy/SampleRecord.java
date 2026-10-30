package com.homing_labs.cq.demo.taxonomy;

/**
 * A record with every object of the taxonomy a record can have:
 * <ul>
 *     <li>superclass: {@link Record}, which its source does not name; superinterface: {@link Comparable}</li>
 *     <li>constructors: the canonical one, compact, and one more; static initialiser: the static block</li>
 *     <li>class object - field: {@code EMPTY}; implementation: {@code of}; member class: {@code Comparators}</li>
 *     <li>instances - fields: the components', {@code name} and {@code size}; implementations: {@code compareTo},
 *     beside the accessors, {@code equals}, {@code hashCode} and {@code toString}; member class: {@code Cursor}</li>
 * </ul>
 */
public record SampleRecord(String name, int size) implements Comparable<SampleRecord> {

    static final SampleRecord EMPTY;

    static {
        EMPTY = new SampleRecord("", 0);
    }

    public SampleRecord {
        if (size < 0) {
            throw new IllegalArgumentException("size " + size);
        }
    }

    public SampleRecord(final String name) {
        this(name, 0);
    }

    static SampleRecord of(final String name) {
        return new SampleRecord(name);
    }

    @Override
    public int compareTo(final SampleRecord other) {
        return Integer.compare(size, other.size);
    }

    static class Comparators {
    }

    class Cursor {
    }
}
