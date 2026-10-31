package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The components of a record, in order. */
public record RecordComponents(List<RecordComponent> items) implements Vector<RecordComponent> {

    public RecordComponents {
        items = List.copyOf(items);
    }
}
