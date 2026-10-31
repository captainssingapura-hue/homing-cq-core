package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.WithSource;

/**
 * A class nested in a unit, as a unit of the second level: the top-level unit it belongs to, and the nested class with
 * its source. A static member class and an inner class are told apart, as the taxonomy tells them, so a sub-rule can be
 * for either, or for both. A rule's result still goes on the top-level unit, which delegates to its nested units;
 * {@link JavaNests} lists them.
 */
public sealed interface JavaNestedUnit extends QcUnit<JavaLang> {

    /** The top-level unit the nested class belongs to. */
    JavaUnit top();

    /** The nested class, with its source. */
    WithSource<NestedClass> nested();

    /** A static member class. */
    record Static(JavaUnit top, WithSource<NestedClass> nested) implements JavaNestedUnit {
    }

    /** An inner class. */
    record Inner(JavaUnit top, WithSource<NestedClass> nested) implements JavaNestedUnit {
    }
}
