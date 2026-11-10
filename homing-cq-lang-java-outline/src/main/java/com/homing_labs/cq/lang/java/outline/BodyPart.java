package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * A part of a type's body (JLS 8.1.7, 8.9, 8.10, 9.1.4, 9.6): a field, a method, a constructor, an initializer, a
 * member type, an enum constant, or a semicolon that declares nothing.
 *
 * <p>A part's header is read in full; what follows it - a method's body, a field's initial value, a member type's body
 * - is left as tokens for a later stage. Its range runs from its start - its documentation comment, if it has one - up
 * to the next part's start, or to the body's closing brace, so the comments and the blank lines after a part are its
 * own. The parts cover the body from the first one's start: what comes before it, after the opening brace, is the
 * type's own.</p>
 */
public sealed interface BodyPart extends Scalar {

    /** The tokens of the part, up to the next part's. */
    TokenRange range();

    /** A field declaration: its type, and the variables declared with it, one at least. */
    record Field(Optional<DocComment> doc, Modifiers modifiers, TypeText type, Declarators declarators,
                 TokenRange range) implements BodyPart {
    }

    /**
     * A method: its type parameters, its result - {@code void} included - as written, its name, its parameters and the
     * types it throws. Brackets written after its parameters - an old form (JLS 8.4) - add to its result's dimensions.
     * An annotation interface's element may have a default value. A method has a body, unless a semicolon is written in
     * its place.
     */
    record Method(Optional<DocComment> doc, Modifiers modifiers, TypeParameters typeParameters, TypeText result,
                  Name name, Parameters parameters, TokenRange dimensions, TypeTexts thrown,
                  Optional<TokenRange> defaultValue, Optional<TokenRange> body, TokenRange range) implements BodyPart {
    }

    /** A constructor: what a method has, but a result, and named as its class is. */
    record Constructor(Optional<DocComment> doc, Modifiers modifiers, TypeParameters typeParameters, Name name,
                       Parameters parameters, TypeTexts thrown, Optional<TokenRange> body, TokenRange range)
            implements BodyPart {
    }

    /** A record's compact canonical constructor: its name and its body; its parameters are the record's components. */
    record CompactConstructor(Optional<DocComment> doc, Modifiers modifiers, Name name, TokenRange body,
                              TokenRange range) implements BodyPart {
    }

    /**
     * An initializer: a block run once, when the class is initialized - a static one - or with each new object. javac
     * gives an initializer no documentation comment; the one written before it is kept here all the same, as the
     * part's range starts there.
     */
    record Initializer(Optional<DocComment> doc, Modifiers modifiers, TokenRange body, TokenRange range)
            implements BodyPart {

        /** Whether it is run once, when the class is initialized. */
        public boolean isStatic() {
            return modifiers.has("static");
        }
    }

    /**
     * A member type: its header, read in full, and its body - its braces and all between them - as tokens. A top-level
     * type's member type has that body read too, from a block of code copied out of the file; one level further down,
     * a member type's body is left unread.
     */
    record TypeDeclaration(TypeHeader header, TokenRange body, BodyEnd end, Optional<NestedBody> nested,
                           TokenRange range) implements BodyPart {
    }

    /**
     * An enum constant: its annotations, its name, and the arguments it is made with, in their parentheses, and its
     * own class body, as tokens, where they are written.
     */
    record EnumConstant(Optional<DocComment> doc, Modifiers annotations, Name name, Optional<TokenRange> arguments,
                        Optional<TokenRange> body, TokenRange range) implements BodyPart {
    }

    /** A semicolon where a member could be: it declares nothing. */
    record EmptyDeclaration(TokenRange range) implements BodyPart {
    }

    /** Tokens no part can be read from; a diagnostic says why. */
    record Unrecognised(TokenRange range) implements BodyPart {
    }
}
