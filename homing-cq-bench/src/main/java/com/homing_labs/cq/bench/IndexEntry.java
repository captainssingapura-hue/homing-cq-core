package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.scan.Compliance;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * What an entry of the bench's indexes stands for, and how it is shown: a package, or a class to measure, with
 * {@link Badges} for its kind and how it exists - and, in the tree of units, how far it complies with the rules.
 */
public sealed interface IndexEntry extends Scalar {

    /**
     * A package, by the last part of its name - {@code cq} for {@code com.homing_labs.cq} - and how far the units in
     * it and in the packages below it comply.
     */
    record Package(String name, String label, Compliance compliance) implements IndexEntry {

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * A unit, or a class nested in one, shown by its name in its package or in the class it is declared in: of its
     * kind, and existing as it does there. A unit in the tree of units has its compliance; a class in a nest has none.
     */
    record OfClass(Selection selection, String label, ClassKind kind, Existence existence,
                   Optional<Compliance> compliance) implements IndexEntry {

        @Override
        public String toString() {
            return label;
        }
    }
}
