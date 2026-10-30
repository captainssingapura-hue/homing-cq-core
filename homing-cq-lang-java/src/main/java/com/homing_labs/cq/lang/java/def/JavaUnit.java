package com.homing_labs.cq.lang.java.def;

import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;

import java.util.Optional;

/**
 * A top-level Java class under check: the class, loaded without being initialised, and its source when the source is
 * at hand. The unit covers the classes nested in it too - everything compiled from it, as
 * {@link Class#getNestMembers()} lists them - so a finding on any of those is placed on this unit.
 *
 * <p>A unit is told apart by the kind of its class, as the taxonomy has it - a class, an abstract class, an interface,
 * an enum, a record or an annotation - so a rule can be for one kind of unit, or for every unit. {@link JavaUnits}
 * makes the unit of a class's kind.</p>
 */
public sealed interface JavaUnit extends QcUnit<JavaLang> {

    /** The top-level class. */
    Class<?> type();

    /** The source file that declares the class, when it was found. */
    Optional<JavaSource> source();

    /** The kind of the class. */
    ClassKind kind();

    /** A concrete class. */
    record OfClass(Class<?> type, Optional<JavaSource> source) implements JavaUnit {

        public OfClass {
            JavaUnits.INSTANCE.check(type, ClassKind.Class.INSTANCE);
        }

        @Override
        public ClassKind kind() {
            return ClassKind.Class.INSTANCE;
        }
    }

    /** An abstract class. */
    record OfAbstractClass(Class<?> type, Optional<JavaSource> source) implements JavaUnit {

        public OfAbstractClass {
            JavaUnits.INSTANCE.check(type, ClassKind.AbstractClass.INSTANCE);
        }

        @Override
        public ClassKind kind() {
            return ClassKind.AbstractClass.INSTANCE;
        }
    }

    /** An interface. */
    record OfInterface(Class<?> type, Optional<JavaSource> source) implements JavaUnit {

        public OfInterface {
            JavaUnits.INSTANCE.check(type, ClassKind.Interface.INSTANCE);
        }

        @Override
        public ClassKind kind() {
            return ClassKind.Interface.INSTANCE;
        }
    }

    /** An enum. */
    record OfEnum(Class<?> type, Optional<JavaSource> source) implements JavaUnit {

        public OfEnum {
            JavaUnits.INSTANCE.check(type, ClassKind.Enum.INSTANCE);
        }

        @Override
        public ClassKind kind() {
            return ClassKind.Enum.INSTANCE;
        }
    }

    /** A record. */
    record OfRecord(Class<?> type, Optional<JavaSource> source) implements JavaUnit {

        public OfRecord {
            JavaUnits.INSTANCE.check(type, ClassKind.Record.INSTANCE);
        }

        @Override
        public ClassKind kind() {
            return ClassKind.Record.INSTANCE;
        }
    }

    /** An annotation. */
    record OfAnnotation(Class<?> type, Optional<JavaSource> source) implements JavaUnit {

        public OfAnnotation {
            JavaUnits.INSTANCE.check(type, ClassKind.Annotation.INSTANCE);
        }

        @Override
        public ClassKind kind() {
            return ClassKind.Annotation.INSTANCE;
        }
    }
}
