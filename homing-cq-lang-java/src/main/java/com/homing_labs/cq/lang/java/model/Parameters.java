package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * The parameter types of a method or a constructor, in order.
 */
public record Parameters(List<TypeUse> items) implements Vector<TypeUse> {

    public Parameters {
        items = List.copyOf(items);
    }
}
