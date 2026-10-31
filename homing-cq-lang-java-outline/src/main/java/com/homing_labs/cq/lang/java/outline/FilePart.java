package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * A part of an ordinary compilation unit (JLS 7.3): what comes before its first declaration, its package declaration,
 * an import, a type, or a semicolon that declares nothing.
 *
 * <p>A part's range runs from its start - its documentation comment, if it has one - up to the next part's start, so
 * the comments and the blank lines after a part are its own. The parts of a file cover all of its tokens, each
 * once.</p>
 */
public sealed interface FilePart extends Scalar {

    /** The tokens of the part, up to the next part's. */
    TokenRange range();

    /** The comments before the first declaration - a licence, say - or all of a file that declares nothing. */
    record Preamble(TokenRange range) implements FilePart {
    }

    /**
     * The package declaration: its modifiers, which can only be annotations, and only in {@code package-info.java},
     * and its name. Only there does a documentation comment belong to it.
     */
    record PackageDeclaration(Optional<DocComment> doc, Modifiers modifiers, Name name, TokenRange range)
            implements FilePart {
    }

    /** An import: what it brings in, and the name it brings it in by, without the {@code .*}. */
    record ImportDeclaration(ImportKind kind, Name name, TokenRange range) implements FilePart {
    }

    /**
     * A type: its header, read in full, its body - its braces and all between them - and the parts of its body, each
     * read down to its own body, which is left as tokens.
     */
    record TypeDeclaration(TypeHeader header, TokenRange body, BodyEnd end, BodyParts parts, TokenRange range)
            implements FilePart {
    }

    /** A semicolon where a type could be: it declares nothing. */
    record EmptyDeclaration(TokenRange range) implements FilePart {
    }

    /** Tokens no part can be read from; a diagnostic says why. */
    record Unrecognised(TokenRange range) implements FilePart {
    }
}
