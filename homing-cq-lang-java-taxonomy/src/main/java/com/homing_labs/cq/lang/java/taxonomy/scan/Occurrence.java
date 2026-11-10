package com.homing_labs.cq.lang.java.taxonomy.scan;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An object of the taxonomy found in a class file: a class, of its kind, or one of its members. Names are as the class
 * file has them: a class by its internal name, a method by its name and descriptor.
 */
public sealed interface Occurrence extends Scalar {

    /** The kind of object it is. */
    ObjectKind kind();

    /** A class, of its kind. */
    record OfClass(String name, ClassKind kind) implements Occurrence {
    }

    /** A member of a class. */
    record OfMember(String inClass, ObjectKind kind, String name) implements Occurrence {
    }
}
