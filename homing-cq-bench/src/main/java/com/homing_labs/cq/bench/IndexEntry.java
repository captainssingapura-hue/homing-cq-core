package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * What an entry of the bench's indexes stands for, and how it is shown: a package, or a class to measure, with
 * {@link Badges} for its kind and how it exists.
 */
public sealed interface IndexEntry extends Scalar {

    /** A package. */
    record Package(String name) implements IndexEntry {

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * A unit, or a class nested in one, shown by its name in its package or in the class it is declared in: of its
     * kind, and existing as it does there.
     */
    record OfClass(Selection selection, String label, ClassKind kind, Existence existence) implements IndexEntry {

        @Override
        public String toString() {
            return label;
        }
    }
}
