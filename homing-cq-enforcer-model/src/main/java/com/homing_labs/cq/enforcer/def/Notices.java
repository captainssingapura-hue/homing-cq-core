package com.homing_labs.cq.enforcer.def;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.Set;

/**
 * The notices of one rule on one unit, as a set, like its findings.
 */
public record Notices(Set<? extends Notice> items) implements Vector<Notice> {

    public Notices {
        items = Set.copyOf(items);
    }
}
