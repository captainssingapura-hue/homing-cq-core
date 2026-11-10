package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** The parameters of a method or a constructor as the source writes them, in order. */
public record WrittenParameters(List<WrittenParameter> items) implements Vector<WrittenParameter> {

    public WrittenParameters {
        items = List.copyOf(items);
    }
}
