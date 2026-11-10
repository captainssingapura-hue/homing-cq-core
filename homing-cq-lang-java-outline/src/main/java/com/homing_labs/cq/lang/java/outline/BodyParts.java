package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The parts of a type's body, in the order they are written. */
public record BodyParts(List<BodyPart> items) implements Vector<BodyPart> {

    public BodyParts {
        items = List.copyOf(items);
    }

    /** The parts of one kind, in order. */
    public <P extends BodyPart> List<P> of(final Class<P> kind) {
        return items.stream().filter(kind::isInstance).map(kind::cast).toList();
    }
}
