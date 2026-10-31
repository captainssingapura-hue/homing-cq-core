package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * Types, in order: the bounds of a type parameter, or the types a signature says are thrown.
 */
public record TypeUses(List<TypeUse> items) implements Vector<TypeUse> {

    public TypeUses {
        items = List.copyOf(items);
    }
}
