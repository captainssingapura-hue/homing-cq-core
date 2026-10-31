package com.homing_labs.cq.lang.java.def;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** Stretches of a source file's text, in the order they are written. */
public record SourceSpans(List<SourceSpan> items) implements Vector<SourceSpan> {

    public SourceSpans {
        items = List.copyOf(items);
    }
}
