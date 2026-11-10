package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/** The word a clause of a type's header starts with. */
public sealed interface ClauseWord extends Stateless, Scalar {

    /** The word as written. */
    default String word() {
        return switch (this) {
            case Extends _ -> "extends";
            case Implements _ -> "implements";
            case Permits _ -> "permits";
        };
    }

    /** Where the clause comes among the others: the clauses of a header are written in this order. */
    default int order() {
        return switch (this) {
            case Extends _ -> 0;
            case Implements _ -> 1;
            case Permits _ -> 2;
        };
    }

    record Extends() implements ClauseWord {
        public static final Extends INSTANCE = new Extends();
    }

    record Implements() implements ClauseWord {
        public static final Implements INSTANCE = new Implements();
    }

    record Permits() implements ClauseWord {
        public static final Permits INSTANCE = new Permits();
    }
}
