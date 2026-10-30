package com.homing_labs.cq.bench.rules;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaUnit;

import java.util.Set;

/**
 * Checks {@link InterfacesAreSeen} on interfaces alone; it has no {@code INSTANCE}, so it is found by its constructor.
 */
public final class InterfacesAreSeenEnforcer implements Enforcer<JavaLang, InterfacesAreSeen, JavaUnit.OfInterface> {

    @Override
    public Result<JavaLang, InterfacesAreSeen> check(final JavaUnit.OfInterface unit) {
        return new Result<>(new Findings<>(Set.of(new Seen<>(unit.type().getSimpleName()))));
    }
}
