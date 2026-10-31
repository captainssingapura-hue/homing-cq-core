package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/** A clause of a type's header as written: its word, and the types it lists. */
public record Clause(ClauseWord word, TypeTexts types, TokenRange range) implements Scalar {
}
