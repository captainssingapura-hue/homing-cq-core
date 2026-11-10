package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The parts of a file, in order: together they cover all of its tokens. */
public record FileParts(List<FilePart> items) implements Vector<FilePart> {

    public FileParts {
        items = List.copyOf(items);
    }
}
