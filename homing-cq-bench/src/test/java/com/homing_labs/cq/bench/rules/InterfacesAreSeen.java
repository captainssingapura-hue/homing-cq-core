package com.homing_labs.cq.bench.rules;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;

/** {@link Neatness} for Java interfaces alone: each is seen. */
public record InterfacesAreSeen() implements Rule<Neatness, JavaLang> {
}
