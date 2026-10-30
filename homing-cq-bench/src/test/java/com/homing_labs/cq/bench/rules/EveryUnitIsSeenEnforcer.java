package com.homing_labs.cq.bench.rules;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaUnit;

import java.util.Set;

/** Checks {@link EveryUnitIsSeen} on every unit, found by its {@code INSTANCE}. */
public record EveryUnitIsSeenEnforcer() implements Enforcer<JavaLang, EveryUnitIsSeen, JavaUnit> {

    public static final EveryUnitIsSeenEnforcer INSTANCE = new EveryUnitIsSeenEnforcer();

    @Override
    public Result<JavaLang, EveryUnitIsSeen> check(final JavaUnit unit) {
        return new Result<>(new Findings<>(Set.of(new Seen<>(unit.type().getSimpleName()))));
    }
}
