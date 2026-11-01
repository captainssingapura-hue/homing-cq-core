package com.homing_labs.cq.demo.taxonomy;

import java.net.HttpURLConnection;
import java.util.Comparator;
import java.util.List;

/**
 * A record with every object of the taxonomy a record can have:
 * <ul>
 *     <li>type parameter: {@code T}, bounded by a class, {@link Number}, and by an interface, {@link Comparable} of
 *     {@code ? super T}; superclass: {@link Record}, which its source does not name; superinterface:
 *     {@link Comparable}, of the record</li>
 *     <li>record components: {@code name}, {@code size}, and {@code parts}, a list of {@code T}</li>
 *     <li>constructors: the canonical one, compact, and one more</li>
 *     <li>class object - fields: {@code EMPTY} and {@code OK}; implementation: {@code of}; member class:
 *     {@code Comparators}</li>
 *     <li>instances - fields: the components', {@code name}, {@code size} and {@code parts}; implementations:
 *     {@code compareTo}, beside the accessors, {@code equals}, {@code hashCode} and {@code toString}; member class:
 *     {@code Cursor}</li>
 *     <li>in its code - local class: {@code Sizes}; anonymous class: a {@link Comparator}; both in
 *     {@code compareTo}</li>
 *     <li>dependencies - in signatures, an annotation ({@link SampleAnnotation}), in code, in local variables, in
 *     nesting and an inlined constant ({@link HttpURLConnection#HTTP_OK}); a record cannot be sealed</li>
 * </ul>
 * <p>Beside them, and no object of the taxonomy, how it is initialised: here its static block.</p>
 */
@SampleAnnotation("record")
public record SampleRecord<T extends Number & Comparable<? super T>>(String name, int size, List<T> parts)
        implements Comparable<SampleRecord<T>> {

    static final SampleRecord<Integer> EMPTY;

    static final int OK = HttpURLConnection.HTTP_OK;

    static {
        EMPTY = new SampleRecord<Integer>("", 0, List.of());
    }

    public SampleRecord {
        if (size < 0) {
            throw new IllegalArgumentException("size " + size);
        }
    }

    public SampleRecord(final String name) {
        this(name, 0, List.of());
    }

    static SampleRecord<Integer> of(final String name) {
        return new SampleRecord<>(name);
    }

    @Override
    public int compareTo(final SampleRecord<T> other) {
        class Sizes {
            int of(final SampleRecord<T> sample) {
                return sample.size;
            }
        }
        final Comparator<SampleRecord<T>> bySize = new Comparator<>() {
            @Override
            public int compare(final SampleRecord<T> left, final SampleRecord<T> right) {
                return Integer.compare(new Sizes().of(left), new Sizes().of(right));
            }
        };
        return bySize.compare(this, other);
    }

    static class Comparators {
    }

    class Cursor {
    }
}
