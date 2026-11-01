package com.homing_labs.cq.demo.rule;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.principle.DivideAndConquer;

/**
 * {@link DivideAndConquer} in Java: a method or a constructor - from its documentation comment to its closing
 * brace, the lambdas and the local and anonymous classes written in it included - is written in at most 50 effective
 * lines, counted as {@link TopLevelClassesWithin250Lines} counts them. Every method and constructor written in a
 * top-level class or in a member class of it is checked. A source rule: it reads the file that declares them.
 */
public record MethodsWithin50Lines() implements Rule<DivideAndConquer, JavaLang> {

    public static final MethodsWithin50Lines INSTANCE = new MethodsWithin50Lines();

    /** The most effective lines a method or a constructor can be written in. */
    public int limit() {
        return 50;
    }
}
