package com.homing_labs.cq.lang.java.def;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Where a class, or an object of one, is written in its source file, as far as the file has been read: exactly there,
 * somewhere inside a part not read yet, nowhere, or not looked for.
 */
public sealed interface SourcePlace extends Scalar {

    /**
     * Written there: in one stretch, or in several - a static initialiser is put together from every static block and
     * static field's initial value.
     */
    record Written(SourceSpans spans) implements SourcePlace {
    }

    /** Written somewhere inside these stretches, in tokens left unread: a method's body, or a member type's. */
    record Within(SourceSpans spans) implements SourcePlace {
    }

    /**
     * Written nowhere: the compiler implied or generated it - an enum's {@code values()}, a record's accessors, a
     * default constructor, a bridge method - or the source is not the one the class was compiled from.
     */
    record Unwritten() implements SourcePlace, Stateless {
        public static final Unwritten INSTANCE = new Unwritten();
    }

    /** Not looked for: an object of its kind - a dependency, named wherever the class uses it - is not placed yet. */
    record Unplaced() implements SourcePlace, Stateless {
        public static final Unplaced INSTANCE = new Unplaced();
    }
}
