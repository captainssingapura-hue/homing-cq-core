package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Reads a source text into tokens, in one pass forward over its characters (JLS chapter 3).
 *
 * <p>Unicode escapes are translated first, as Java does, but every offset stays the text's own. Whitespace lies
 * between tokens; comments are tokens. A character no token can start with becomes an {@link TokenKind.Error} token,
 * and anything malformed - a string with no closing quote, a comment with no end, a number with no digits - is a
 * diagnostic: the lexer always reads to the end of the text.</p>
 */
public record JavaLexer() implements StatelessFunctionalObject {

    public static final JavaLexer INSTANCE = new JavaLexer();

    /** The tokens of the text, and what was found wrong in it. */
    public TokenStream scan(final SourceText source) {
        return new Scan(source).run();
    }
}
