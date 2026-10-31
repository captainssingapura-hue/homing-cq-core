package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * A type argument a generic class is given: a type, or a wildcard - unbounded, or bounded above or below.
 */
public sealed interface TypeArgument extends Scalar {

    /** A type: the {@code String} of {@code List<String>}. */
    record Exact(TypeUse type) implements TypeArgument {
    }

    /** A wildcard bounded above: {@code ? extends Number}. */
    record Extends(TypeUse bound) implements TypeArgument {
    }

    /** A wildcard bounded below: {@code ? super Integer}. */
    record Super(TypeUse bound) implements TypeArgument {
    }

    /** An unbounded wildcard: {@code ?}. */
    record Unbounded() implements TypeArgument, Stateless {

        public static final Unbounded INSTANCE = new Unbounded();
    }
}
