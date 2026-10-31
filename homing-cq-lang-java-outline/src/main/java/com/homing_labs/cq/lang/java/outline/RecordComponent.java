package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A component of a record as written: its annotations, its type, whether it takes any number of values - written
 * {@code ...} - and its name.
 */
public record RecordComponent(Modifiers annotations, TypeText type, boolean variable, Name name, TokenRange range)
        implements Scalar {
}
