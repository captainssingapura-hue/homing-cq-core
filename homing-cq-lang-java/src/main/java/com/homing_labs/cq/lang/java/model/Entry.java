package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.List;
import java.util.Optional;

/**
 * What a class has of one object of the taxonomy, shaped by how many of it a class of its kind has: exactly one value,
 * one at most, or any number. Each value comes with its source, when that is at hand.
 */
public sealed interface Entry extends Scalar {

    /** The object of the taxonomy the values are of. */
    ObjectKind object();

    /** Every value, however many there are. */
    List<WithSource<? extends Scalar>> all();

    /** Exactly one value. */
    record One(ObjectKind object, WithSource<? extends Scalar> value) implements Entry {

        @Override
        public List<WithSource<? extends Scalar>> all() {
            return List.of(value);
        }
    }

    /** One value at most. */
    record ZeroOrOne(ObjectKind object, Optional<WithSource<? extends Scalar>> value) implements Entry {

        @Override
        public List<WithSource<? extends Scalar>> all() {
            return value.stream().toList();
        }
    }

    /** Any number of values: none, or one at least, as the object's cardinality has it. */
    record Many(ObjectKind object, Values values) implements Entry {

        @Override
        public List<WithSource<? extends Scalar>> all() {
            return values.items();
        }
    }
}
