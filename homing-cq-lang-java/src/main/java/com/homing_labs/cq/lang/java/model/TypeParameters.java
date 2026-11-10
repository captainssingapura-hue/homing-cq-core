package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * The type parameters a method or a constructor declares, in order.
 */
public record TypeParameters(List<TypeParameterValue> items) implements Vector<TypeParameterValue> {

    public TypeParameters {
        items = List.copyOf(items);
    }
}
