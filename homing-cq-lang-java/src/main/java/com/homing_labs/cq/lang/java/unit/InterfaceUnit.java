package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;

import java.util.Optional;

/**
 * The unit of an interface.
 */
public record InterfaceUnit(Class<?> type, Identity identity, Attributes attributes, Optional<JavaSource> source)
        implements JavaUnit {

    public InterfaceUnit {
        JavaUnits.INSTANCE.check(type, identity, attributes, ClassKind.Interface.INSTANCE);
    }
}
