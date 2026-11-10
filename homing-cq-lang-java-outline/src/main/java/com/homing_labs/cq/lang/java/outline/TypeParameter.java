package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/** A type parameter as written: its annotations, its name, and the types it extends, if any. */
public record TypeParameter(Modifiers annotations, Name name, TypeTexts bounds, TokenRange range) implements Scalar {
}
