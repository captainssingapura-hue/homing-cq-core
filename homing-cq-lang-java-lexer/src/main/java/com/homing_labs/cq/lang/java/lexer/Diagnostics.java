package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** Diagnostics, in the order they were found. */
public record Diagnostics(List<Diagnostic> items) implements Vector<Diagnostic> {

    public Diagnostics {
        items = List.copyOf(items);
    }
}
