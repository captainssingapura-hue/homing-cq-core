package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The steps of how a class is initialised, in the order they are written and run. */
public record InitialisationSteps(List<InitialisationStep> items) implements Vector<InitialisationStep> {

    public InitialisationSteps {
        items = List.copyOf(items);
    }
}
