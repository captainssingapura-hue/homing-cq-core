package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * Something wrong, or doubtful, in a source text, and where: from its first character up to, not including, its end.
 * A stage that finds one says so and carries on; nothing throws.
 */
public record Diagnostic(Severity severity, String message, int start, int end) implements Scalar {
}
