package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * The annotations applied to the parameters of a method or a constructor, by position.
 */
public record ParameterAnnotations(List<ParameterAnnotation> items) implements Vector<ParameterAnnotation> {

    public ParameterAnnotations {
        items = List.copyOf(items);
    }
}
