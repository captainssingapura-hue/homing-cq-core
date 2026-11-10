package com.homing_labs.cq.lang.java.extract;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

/** A class with an object of each kind, written in each way the source locator tells apart. */
@Deprecated
public class Placed<T extends Comparable<T>> extends AbstractList<T> implements RandomAccess {

    static final int CONSTANT = 1 + 2; // a constant: no initialiser computes it

    static final String NAMED = "named";

    static int counted = count();

    static final Integer BOXED = 3;

    static final int DOUBLED = CONSTANT * 2;

    static final int COPIED = counted; // a name, as a constant's value can be, but of no constant

    static {
        counted++;
    }

    int first = 1, second, third = 3;

    private final List<T> items;

    {
        second = 2;
    }

    /** Hands over to the other. */
    public Placed() {
        this(List.of());
    }

    public Placed(final List<T> items) {
        this.items = items;
    }

    @Override
    public T get(final int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    void overloaded(final int value) {
    }

    void overloaded(final String value) {
    }

    void overloaded(final Map.Entry<String, T>[] entries) {
    }

    void overloaded(final T value, final int... rest) {
    }

    private static int count() {
        return 0;
    }

    /** Written as the next is: only the bound of its own T tells them apart. */
    <T extends Number> String picked(final T value) {
        return new Object() { }.toString();
    }

    <T extends CharSequence> String picked(final T value) {
        return new Object() { }.toString();
    }

    void put(final T value) { // the class's T
    }

    <T> void put(final T value) { // a T of its own
    }

    Runnable local() {
        class Local implements Runnable {
            @Override
            public void run() {
            }
        }
        return new Local();
    }

    static class Member {
        int inside;
    }

    /** An inner class: javac gives its constructors the object of this class first. */
    class Inner {

        static final int LIMIT = 3;

        static int made = LIMIT + count();

        final int count;

        Inner(final int count) {
            this.count = count;
        }

        class Deeper {
            int hidden;
        }
    }
}
