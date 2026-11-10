package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The variables a field declaration declares, in order. */
public record Declarators(List<Declarator> items) implements Vector<Declarator> {

    public Declarators {
        items = List.copyOf(items);
    }
}
