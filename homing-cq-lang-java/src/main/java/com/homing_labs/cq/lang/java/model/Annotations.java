package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * The annotations applied to a member, those kept at runtime before those kept in the class file alone, each in the
 * order the source gives them.
 */
public record Annotations(List<AppliedAnnotation> items) implements Vector<AppliedAnnotation> {

    public Annotations {
        items = List.copyOf(items);
    }
}
