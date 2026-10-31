package com.homing_labs.cq.demo;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.enforcer.def.Enforcers;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.JavaUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs one rule's enforcer over the units it checks and keeps the results that found something, by the class of their
 * unit. A hand-written stand-in for the engine the framework does not have yet.
 */
record DemoFindings() {

    static final DemoFindings INSTANCE = new DemoFindings();

    @SuppressWarnings("unchecked")
    <R extends Rule<?, JavaLang>> Map<Class<?>, Result<JavaLang, R>> of(
            final Enforcer<JavaLang, R, ? extends JavaUnit> enforcer, final List<JavaUnit> units) {
        final EnforcerSignature signature = Enforcers.INSTANCE.signatureOf(enforcer);
        final Map<Class<?>, Result<JavaLang, R>> found = new HashMap<>();
        for (final JavaUnit unit : units) {
            if (!signature.checks(unit)) {
                continue;
            }
            final Result<JavaLang, R> result = (Result<JavaLang, R>) Enforcers.INSTANCE.check(enforcer, unit);
            if (result.hasFindings()) {
                found.put(unit.type(), result);
            }
        }
        return found;
    }
}
