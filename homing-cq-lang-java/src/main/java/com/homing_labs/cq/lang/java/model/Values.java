package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * The values of an object a class can have any number of, in order, each with its source when that is at hand.
 */
public record Values(List<WithSource<? extends Scalar>> items) implements Vector<WithSource<? extends Scalar>> {

    public Values {
        items = List.copyOf(items);
    }
}
