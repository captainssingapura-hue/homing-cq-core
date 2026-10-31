package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;

import java.util.Optional;

/**
 * A top-level Java class under check: its identity, and its attributes - what it has of each object of the taxonomy,
 * generated from the taxonomy for its kind - with the source file that declares it when that is at hand. The unit
 * covers the classes nested in it too: each member class is among its attributes, with attributes of its own.
 *
 * <p>A unit is told apart by the kind of its class - a class, an abstract class, an interface, an enum, a record or an
 * annotation - so a rule can be for one kind of unit, or for every unit. {@link JavaUnits} makes the unit of a class's
 * kind. It still keeps the class as loaded, for what reads it by reflection.</p>
 */
public sealed interface JavaUnit extends QcUnit<JavaLang>
        permits ClassUnit, AbstractClassUnit, InterfaceUnit, EnumUnit, RecordUnit, AnnotationUnit {

    /** The top-level class as loaded, without being initialised: for what still reads it by reflection. */
    Class<?> type();

    /** Which class the unit is. */
    Identity identity();

    /** What the class has, by the objects of the taxonomy. */
    Attributes attributes();

    /** The source file that declares the class, when it was found. */
    Optional<JavaSource> source();

    /** The kind of the class. */
    default ClassKind kind() {
        return identity().kind();
    }
}
