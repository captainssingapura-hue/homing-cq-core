package com.homing_labs.cq.lang.java.def;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/**
 * Types in order, as a method declares its parameters: equal by its types in order, each as many times as it comes,
 * and copied when made, so it never changes.
 */
public record JavaTypes(List<JavaType> items) implements Vector<JavaType> {

    public JavaTypes {
        items = List.copyOf(items);
    }
}
