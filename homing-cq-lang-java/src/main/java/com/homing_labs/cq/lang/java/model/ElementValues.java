package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * The values of an array an annotation's element holds, in order.
 */
public record ElementValues(List<ElementValue> items) implements Vector<ElementValue> {

    public ElementValues {
        items = List.copyOf(items);
    }
}
