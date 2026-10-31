package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * What a token is, by the lexical grammar of Java (JLS chapter 3). Comments are tokens too, so nothing written is
 * lost; whitespace is not, and lies between tokens.
 *
 * <p>The lexer knows no context, so a few things are left to the grammar: a contextual keyword - {@code record},
 * {@code sealed}, {@code permits}, {@code module} and the like - is an {@link Identifier}; {@code non-sealed} is three
 * tokens; and {@code >} is always a token of its own, so {@code >>}, {@code >>>} and {@code >=} are read from
 * single ones where an expression has them, and type arguments close without splitting anything.</p>
 */
public sealed interface TokenKind extends Stateless, Scalar {

    /** Whether a token of this kind is a comment: kept with the tokens, but never read by the grammar. */
    default boolean comment() {
        return this instanceof LineComment || this instanceof BlockComment || this instanceof DocComment;
    }

    /** A name, or a keyword only some places have. */
    record Identifier() implements TokenKind {
        public static final Identifier INSTANCE = new Identifier();
    }

    /** A word reserved everywhere: {@code class}, {@code import}, {@code public}, {@code _} and the rest. */
    record Keyword() implements TokenKind {
        public static final Keyword INSTANCE = new Keyword();
    }

    /** A whole number, in decimal, hexadecimal, octal or binary, with or without {@code L}. */
    record IntegerLiteral() implements TokenKind {
        public static final IntegerLiteral INSTANCE = new IntegerLiteral();
    }

    /** A number with a fraction, an exponent, or {@code f} or {@code d}. */
    record FloatingPointLiteral() implements TokenKind {
        public static final FloatingPointLiteral INSTANCE = new FloatingPointLiteral();
    }

    /** {@code true} or {@code false}. */
    record BooleanLiteral() implements TokenKind {
        public static final BooleanLiteral INSTANCE = new BooleanLiteral();
    }

    /** One character, in single quotes. */
    record CharacterLiteral() implements TokenKind {
        public static final CharacterLiteral INSTANCE = new CharacterLiteral();
    }

    /** A string on one line, in double quotes. */
    record StringLiteral() implements TokenKind {
        public static final StringLiteral INSTANCE = new StringLiteral();
    }

    /** A string over lines, in triple double quotes. */
    record TextBlock() implements TokenKind {
        public static final TextBlock INSTANCE = new TextBlock();
    }

    /** {@code null}. */
    record NullLiteral() implements TokenKind {
        public static final NullLiteral INSTANCE = new NullLiteral();
    }

    /** {@code ( ) { } [ ] ; , . ... @ ::} */
    record Separator() implements TokenKind {
        public static final Separator INSTANCE = new Separator();
    }

    /** An operator, as long as it can be, except that {@code >} always stands alone. */
    record Operator() implements TokenKind {
        public static final Operator INSTANCE = new Operator();
    }

    /** A comment from {@code //} to the end of its line. */
    record LineComment() implements TokenKind {
        public static final LineComment INSTANCE = new LineComment();
    }

    /** A comment from {@code /*} to the next star and slash. */
    record BlockComment() implements TokenKind {
        public static final BlockComment INSTANCE = new BlockComment();
    }

    /** A block comment that starts with two stars: documentation, for the declaration that follows it. */
    record DocComment() implements TokenKind {
        public static final DocComment INSTANCE = new DocComment();
    }

    /** A character no token can start with. A diagnostic says which; the lexer carries on after it. */
    record Error() implements TokenKind {
        public static final Error INSTANCE = new Error();
    }
}
