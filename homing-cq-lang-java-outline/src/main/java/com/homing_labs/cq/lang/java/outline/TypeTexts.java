package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** Types as written, in order: those a clause lists, or the bounds of a type parameter. */
public record TypeTexts(List<TypeText> items) implements Vector<TypeText> {

    public TypeTexts {
        items = List.copyOf(items);
    }
}
