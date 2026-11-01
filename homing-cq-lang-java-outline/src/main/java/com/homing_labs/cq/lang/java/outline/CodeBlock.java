package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A stretch of a source file's code, copied out of it to be read on its own: its text, and where it was in the file -
 * the index of its first character, and the line that character is on, from 1.
 */
public record CodeBlock(String text, int offset, int line) implements Scalar {
}
