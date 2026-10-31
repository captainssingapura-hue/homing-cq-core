package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A token: its kind, and where it lies in the source text, from its first character up to, not including, its end.
 * Its text is the source's, read through {@link TokenStream#text(Token)}.
 */
public record Token(TokenKind kind, int start, int end) implements Scalar {
}
