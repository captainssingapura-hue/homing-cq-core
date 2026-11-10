package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The rules applied to a unit, or to a class of it, by the name of each rule. */
public record AppliedRules(List<Applied> items) implements Vector<Applied> {

    public AppliedRules {
        items = List.copyOf(items);
    }
}
