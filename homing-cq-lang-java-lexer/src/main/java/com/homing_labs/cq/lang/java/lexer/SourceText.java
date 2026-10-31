package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * The text of a source file, as it was read, with the file's name - {@code Foo.java} - which a public type's name is
 * checked against. The name is empty for a text that comes from no file.
 *
 * <p>Every offset into the text - a token's, a diagnostic's - counts characters of the text as it was read, before
 * any Unicode escape in it is translated.</p>
 */
public record SourceText(String fileName, String text) implements Scalar {

    /** A text that comes from no file. */
    public SourceText(final String text) {
        this("", text);
    }

    /** The line and the column of the character at the offset, both counted from one. */
    public Position position(final int offset) {
        int line = 1;
        int column = 1;
        for (int at = 0; at < offset && at < text.length(); at++) {
            final char c = text.charAt(at);
            if (c == '\n' || c == '\r' && (at + 1 == text.length() || text.charAt(at + 1) != '\n')) {
                line++;
                column = 1;
            } else if (c != '\r') {
                column++;
            }
        }
        return new Position(line, column);
    }
}
