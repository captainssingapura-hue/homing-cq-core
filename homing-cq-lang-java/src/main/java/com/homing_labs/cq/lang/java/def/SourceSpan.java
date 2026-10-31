package com.homing_labs.cq.lang.java.def;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A stretch of a source file's text, by the indices of its characters: from the first up to, not including, the end.
 */
public record SourceSpan(int start, int end) implements Scalar {
}
