package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Arrays;
import java.util.List;

/**
 * What is selected on the bench: a unit, or one of the classes nested in it.
 */
public record Selection(JavaUnit unit, Class<?> type) implements Scalar {

    /** The classes selected: the whole nest for a unit, the one class for a class nested in it. */
    public List<Class<?>> classes() {
        return type == unit.type() ? Arrays.asList(type.getNestMembers()) : List.of(type);
    }
}
