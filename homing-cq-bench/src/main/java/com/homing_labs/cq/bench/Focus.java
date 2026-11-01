package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.model.ClassInitialisation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.Values;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What a view has selected, for the source to show where it is written: a class of the selected unit, values of one of
 * the taxonomy's objects in such a class - all of them, from a summary line, or one - or how such a class is
 * initialised.
 */
public sealed interface Focus extends Scalar {

    /** A class of the unit: the top-level class, or one nested in it. */
    record OfClass(ClassName name) implements Focus {
    }

    /** Values a class holds as one object of the taxonomy. */
    record OfValues(ClassName inClass, ObjectKind object, Values values) implements Focus {
    }

    /**
     * How a class is initialised, no part of it: all of it, or some of the steps its source writes - as the
     * initialisation those steps alone would be.
     */
    record OfInitialisation(ClassName inClass, WithSource<ClassInitialisation> initialisation) implements Focus {
    }
}
