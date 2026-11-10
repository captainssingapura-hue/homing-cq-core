package com.homing_labs.cq.demo.exempted;

import com.homing_labs.cq.core.def.Excuse;
import com.homing_labs.cq.rules.basic.rule.NoPublicFields;

/** A serialiser writes straight to this field, so it has to be public. */
public record FilledInByASerialiser() implements Excuse<NoPublicFields> {

    public static final FilledInByASerialiser INSTANCE = new FilledInByASerialiser();
}
