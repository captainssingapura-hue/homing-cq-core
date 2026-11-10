package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * How many of a kind of member a place in a class has: exactly one, at most one, at least one, or any number.
 */
public sealed interface Cardinality extends Stateless {

    /** Whether a class can have this many of an object in the place. */
    boolean allows(long count);

    /** Exactly one. */
    record One() implements Cardinality {

        public static final One INSTANCE = new One();

        @Override
        public boolean allows(final long count) {
            return count == 1;
        }
    }

    /** None, or one. */
    record ZeroOrOne() implements Cardinality {

        public static final ZeroOrOne INSTANCE = new ZeroOrOne();

        @Override
        public boolean allows(final long count) {
            return count <= 1;
        }
    }

    /** One, or more. */
    record OneOrMore() implements Cardinality {

        public static final OneOrMore INSTANCE = new OneOrMore();

        @Override
        public boolean allows(final long count) {
            return count >= 1;
        }
    }

    /** Any number, none among them. */
    record ZeroOrMore() implements Cardinality {

        public static final ZeroOrMore INSTANCE = new ZeroOrMore();

        @Override
        public boolean allows(final long count) {
            return true;
        }
    }
}
