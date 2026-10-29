package com.homing_labs.cq.demo;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs one rule's enforcer over units and keeps the results that found something, by the class of their unit. A
 * hand-written stand-in for the engine the framework does not have yet.
 */
record DemoFindings() {

    static final DemoFindings INSTANCE = new DemoFindings();

    <R extends Rule<?, JavaLang>> Map<Class<?>, Result<JavaLang, R>> of(
            final Enforcer<JavaLang, R, JavaUnit> enforcer, final List<JavaUnit> units) {
        final Map<Class<?>, Result<JavaLang, R>> found = new HashMap<>();
        for (final JavaUnit unit : units) {
            final Result<JavaLang, R> result = enforcer.check(unit);
            if (result.hasFindings()) {
                found.put(unit.type(), result);
            }
        }
        return found;
    }
}
