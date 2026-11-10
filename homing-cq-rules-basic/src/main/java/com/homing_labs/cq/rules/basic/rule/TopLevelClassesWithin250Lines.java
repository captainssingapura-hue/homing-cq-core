package com.homing_labs.cq.rules.basic.rule;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.principle.DivideAndConquer;

/**
 * {@link DivideAndConquer} in Java: a top-level class - from its documentation comment to its closing brace, the
 * classes nested in it included - is written in at most 250 effective lines. An effective line is code: a blank line,
 * or one of comments alone, counts none, and a line counts from its start to its last character of code - its
 * indentation in, a comment after it out - as {@code ceiling(length / 120)} lines. A source rule: it reads the file
 * that declares the class.
 */
public record TopLevelClassesWithin250Lines() implements Rule<DivideAndConquer, JavaLang> {

    public static final TopLevelClassesWithin250Lines INSTANCE = new TopLevelClassesWithin250Lines();

    /** The most effective lines a top-level class can be written in. */
    public int limit() {
        return 250;
    }
}
