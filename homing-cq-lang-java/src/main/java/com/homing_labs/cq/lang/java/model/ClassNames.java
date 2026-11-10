package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * Classes by name, in order: superinterfaces, or the types a method throws.
 */
public record ClassNames(List<ClassName> items) implements Vector<ClassName> {

    public ClassNames {
        items = List.copyOf(items);
    }
}
