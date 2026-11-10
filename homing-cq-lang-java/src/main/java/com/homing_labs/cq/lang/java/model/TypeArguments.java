package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * The type arguments a generic class is given, in order.
 */
public record TypeArguments(List<TypeArgument> items) implements Vector<TypeArgument> {

    public TypeArguments {
        items = List.copyOf(items);
    }
}
