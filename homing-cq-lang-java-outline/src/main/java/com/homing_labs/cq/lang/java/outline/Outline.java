package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.Diagnostics;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A source file read down to the parts of its types' bodies: the tokens it was read from, its parts, and what was found
 * wrong in it. The lexer's diagnostics stay with the tokens.
 */
public record Outline(TokenStream tokens, FileParts parts, Diagnostics diagnostics) implements Scalar {

    /** The package declaration, if the file has one. */
    public Optional<FilePart.PackageDeclaration> packageDeclaration() {
        return partsOf(FilePart.PackageDeclaration.class).findFirst();
    }

    /** The imports, in order. */
    public List<FilePart.ImportDeclaration> imports() {
        return partsOf(FilePart.ImportDeclaration.class).toList();
    }

    /** The top-level types, in order. */
    public List<FilePart.TypeDeclaration> types() {
        return partsOf(FilePart.TypeDeclaration.class).toList();
    }

    /** The text the tokens of the range are written with, and the comments and the whitespace between them. */
    public String text(final TokenRange range) {
        if (range.isEmpty()) {
            return "";
        }
        return tokens.source().text().substring(tokens.get(range.from()).start(), tokens.get(range.to() - 1).end());
    }

    private <P extends FilePart> Stream<P> partsOf(final Class<P> kind) {
        return parts.items().stream().filter(kind::isInstance).map(kind::cast);
    }
}
