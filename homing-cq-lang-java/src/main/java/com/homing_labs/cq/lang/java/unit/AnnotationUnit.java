package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassInitialisation;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;

import java.util.Optional;

/**
 * The unit of an annotation.
 */
public record AnnotationUnit(Class<?> type, Identity identity, Attributes attributes,
                             WithSource<ClassInitialisation> initialisation, Optional<JavaSource> source)
        implements JavaUnit {

    public AnnotationUnit {
        JavaUnits.INSTANCE.check(type, identity, attributes, ClassKind.Annotation.INSTANCE);
    }
}
