package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The clauses of a type's header, in the order written. */
public record Clauses(List<Clause> items) implements Vector<Clause> {

    public Clauses {
        items = List.copyOf(items);
    }
}
