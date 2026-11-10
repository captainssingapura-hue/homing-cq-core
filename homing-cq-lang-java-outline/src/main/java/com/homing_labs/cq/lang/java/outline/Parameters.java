package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The parameters of a method or a constructor, in the order they are written. */
public record Parameters(List<Parameter> items) implements Vector<Parameter> {

    public Parameters {
        items = List.copyOf(items);
    }

    /** The formal parameters, in order: those it is called with. */
    public List<Parameter.Formal> formal() {
        return items.stream().flatMap(item -> item instanceof Parameter.Formal formal
                ? Stream.of(formal) : Stream.empty()).toList();
    }

    /** The receiver parameter, if one is written. */
    public Optional<Parameter.Receiver> receiver() {
        return items.stream().flatMap(item -> item instanceof Parameter.Receiver receiver
                ? Stream.of(receiver) : Stream.empty()).findFirst();
    }
}
