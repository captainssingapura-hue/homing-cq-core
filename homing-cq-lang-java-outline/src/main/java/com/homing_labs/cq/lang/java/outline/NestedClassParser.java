package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Reads the body of a type nested in a top-level one from a block of code copied out of its file: the body as written,
 * from the brace that opens it. The block is lexed again, on its own, and its parts are read as a top-level type's
 * are, knowing the word the type is declared with and its name - an enum's constants come first, and a constructor is
 * named as its type. A type nested in it in turn is read down to its header, and its body is left unread: bodies are
 * read one level down only.
 */
public record NestedClassParser() implements StatelessFunctionalObject {

    public static final NestedClassParser INSTANCE = new NestedClassParser();

    /** The body's parts, and what was found wrong in them, at the block's positions. */
    public NestedBody parse(final TypeKeyword keyword, final String name, final CodeBlock block) {
        final TokenStream tokens = JavaLexer.INSTANCE.scan(new SourceText(block.text()));
        return new BodyScan(tokens, keyword, name, inside(tokens)).runNested(block);
    }

    /** The tokens between the block's braces: after the one that opens it, up to the one that closes it, or the end. */
    private TokenRange inside(final TokenStream tokens) {
        int open = 0;
        while (open < tokens.size() && tokens.get(open).kind().comment()) {
            open++;
        }
        if (open == tokens.size() || !tokens.text(tokens.get(open)).equals("{")) {
            return new TokenRange(open, tokens.size());
        }
        int depth = 0;
        for (int at = open; at < tokens.size(); at++) {
            final String text = tokens.text(tokens.get(at));
            if (text.equals("{") && !tokens.get(at).kind().comment()) {
                depth++;
            } else if (text.equals("}") && !tokens.get(at).kind().comment() && --depth == 0) {
                return new TokenRange(open + 1, at);
            }
        }
        return new TokenRange(open + 1, tokens.size());
    }
}
