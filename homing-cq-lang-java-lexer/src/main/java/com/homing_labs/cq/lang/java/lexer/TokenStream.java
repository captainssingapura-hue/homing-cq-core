package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Scalar;

/** A source text read into tokens, with what the lexer found wrong in it on the way. */
public record TokenStream(SourceText source, Tokens tokens, Diagnostics diagnostics) implements Scalar {

    /** How many tokens there are. */
    public int size() {
        return tokens.items().size();
    }

    /** The token at the index. */
    public Token get(final int index) {
        return tokens.items().get(index);
    }

    /** The token's text, with any Unicode escape in it translated: &#92;u0041 reads as {@code A}. */
    public String text(final Token token) {
        final String written = source.text().substring(token.start(), token.end());
        return written.indexOf('\\') < 0 ? written : UnicodeEscapes.INSTANCE.translate(written);
    }
}
