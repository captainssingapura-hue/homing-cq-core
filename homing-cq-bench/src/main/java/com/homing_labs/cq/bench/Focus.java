package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.Values;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What a view has selected, for the source to show where it is written: a class of the selected unit, or values of one
 * of the taxonomy's objects in such a class - all of them, from a summary line, or one.
 */
public sealed interface Focus extends Scalar {

    /** A class of the unit: the top-level class, or one nested in it. */
    record OfClass(ClassName name) implements Focus {
    }

    /** Values a class holds as one object of the taxonomy. */
    record OfValues(ClassName inClass, ObjectKind object, Values values) implements Focus {
    }
}
