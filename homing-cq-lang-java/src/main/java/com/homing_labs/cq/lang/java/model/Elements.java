package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * The elements an annotation gives a value, in the order it gives them.
 */
public record Elements(List<Element> items) implements Vector<Element> {

    public Elements {
        items = List.copyOf(items);
    }
}
