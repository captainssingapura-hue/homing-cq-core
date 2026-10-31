package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;

import java.util.Optional;

/**
 * The unit of an abstract class.
 */
public record AbstractClassUnit(Class<?> type, Identity identity, Attributes attributes, Optional<JavaSource> source)
        implements JavaUnit {

    public AbstractClassUnit {
        JavaUnits.INSTANCE.check(type, identity, attributes, ClassKind.AbstractClass.INSTANCE);
    }
}
