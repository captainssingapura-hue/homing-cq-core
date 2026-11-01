package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.Diagnostics;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * The body of a type nested in a top-level one, read from a block of code copied out of the file: the block, the tokens
 * it was lexed into on its own, the parts found in it, and what was found wrong in them. The tokens are the block's, at
 * the block's positions; in the file, each is the block's offset further on.
 */
public record NestedBody(CodeBlock block, TokenStream tokens, BodyParts parts, Diagnostics diagnostics)
        implements Scalar {

    /** The text the tokens of the range are written with, and the comments and the whitespace between them. */
    public String text(final TokenRange range) {
        if (range.isEmpty()) {
            return "";
        }
        return block.text().substring(tokens.get(range.from()).start(), tokens.get(range.to() - 1).end());
    }

    /** Where the range starts in the file: the index of its first character there. */
    public int start(final TokenRange range) {
        return block.offset() + tokens.get(range.from()).start();
    }

    /** Where the range ends in the file: the index past its last character there. */
    public int end(final TokenRange range) {
        return block.offset() + tokens.get(range.to() - 1).end();
    }
}
