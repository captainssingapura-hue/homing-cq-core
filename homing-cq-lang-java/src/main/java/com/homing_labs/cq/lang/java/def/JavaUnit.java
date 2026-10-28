package com.homing_labs.cq.lang.java.def;

import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.core.def.lang.JavaLang;

/**
 * A top-level Java class under check: the class, loaded without being initialised, and its source when the source is
 * at hand. The unit covers the classes nested in it too - everything compiled from it, as
 * {@link Class#getNestMembers()} lists them - so a finding on any of those is placed on this unit.
 *
 * <p>A unit has its source or it does not, and the two are told apart by type: code that needs the source asks for
 * {@link WithSource}.</p>
 */
public sealed interface JavaUnit extends QcUnit<JavaLang> {

    /** The top-level class. */
    Class<?> type();

    /** A top-level class with the source file that declares it. */
    record WithSource(Class<?> type, JavaSource source) implements JavaUnit {
        public WithSource {
            if (type.getEnclosingClass() != null) {
                throw new IllegalArgumentException("A JavaUnit is a top-level class, but " + type.getName()
                        + " is nested in " + type.getEnclosingClass().getName());
            }
        }
    }

    /** A top-level class read without its source, as from a jar with no sources beside it. */
    record ClassOnly(Class<?> type) implements JavaUnit {
        public ClassOnly {
            if (type.getEnclosingClass() != null) {
                throw new IllegalArgumentException("A JavaUnit is a top-level class, but " + type.getName()
                        + " is nested in " + type.getEnclosingClass().getName());
            }
        }
    }
}
