package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The type parameters of a declaration, in order. */
public record TypeParameters(List<TypeParameter> items) implements Vector<TypeParameter> {

    public TypeParameters {
        items = List.copyOf(items);
    }
}
