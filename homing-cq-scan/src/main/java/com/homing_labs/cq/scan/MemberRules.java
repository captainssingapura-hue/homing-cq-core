package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The member classes nested in a unit, each with the rules applied to it alone. */
public record MemberRules(List<ClassRules> items) implements Vector<ClassRules> {

    public MemberRules {
        items = List.copyOf(items);
    }
}
