package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * How many of a kind of member a place in a class has: exactly one, at most one, at least one, or any number.
 */
public sealed interface Cardinality extends Stateless {

    /** Exactly one. */
    record One() implements Cardinality {

        public static final One INSTANCE = new One();
    }

    /** None, or one. */
    record ZeroOrOne() implements Cardinality {

        public static final ZeroOrOne INSTANCE = new ZeroOrOne();
    }

    /** One, or more. */
    record OneOrMore() implements Cardinality {

        public static final OneOrMore INSTANCE = new OneOrMore();
    }

    /** Any number, none among them. */
    record ZeroOrMore() implements Cardinality {

        public static final ZeroOrMore INSTANCE = new ZeroOrMore();
    }
}
