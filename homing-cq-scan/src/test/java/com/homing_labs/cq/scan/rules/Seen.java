package com.homing_labs.cq.scan.rules;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Finding;

/** That a rule saw a unit. */
public record Seen<R extends Rule<?, JavaLang>>(String unit) implements Finding<JavaLang, R> {
}
