package com.homing_labs.cq.scan.rules;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;

/** {@link Neatness} for every Java unit: each is seen. */
public record EveryUnitIsSeen() implements Rule<Neatness, JavaLang> {
}
