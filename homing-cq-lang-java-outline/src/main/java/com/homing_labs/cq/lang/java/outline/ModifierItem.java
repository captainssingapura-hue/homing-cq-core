package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/** A modifier as written before a declaration: a word, or an annotation. */
public sealed interface ModifierItem extends Scalar {

    /** The tokens it is written with. */
    TokenRange range();

    /** A modifier word: {@code public}, {@code abstract}, {@code sealed}, {@code non-sealed} and the rest. */
    record Word(String word, TokenRange range) implements ModifierItem {
    }

    /**
     * An annotation: its name, and its arguments, in their parentheses, as tokens. The arguments are expressions, read
     * by a later stage.
     */
    record Annotation(Name name, Optional<TokenRange> arguments, TokenRange range) implements ModifierItem {
    }
}
