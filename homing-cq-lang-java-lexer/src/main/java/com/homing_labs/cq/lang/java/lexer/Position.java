package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Scalar;

/** Where a character is in a source text: its line and its column, both counted from one. */
public record Position(int line, int column) implements Scalar {
}
