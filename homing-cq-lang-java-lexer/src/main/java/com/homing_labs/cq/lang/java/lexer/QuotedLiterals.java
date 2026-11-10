package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Reads a quoted literal where the cursor is (JLS 3.10.4 to 3.10.7): a character, a string or a text block, and the
 * escapes they share. One that is malformed or left open is reported, and read as far as it goes.
 */
record QuotedLiterals() implements StatelessFunctionalObject {

    static final QuotedLiterals INSTANCE = new QuotedLiterals();

    /** What may follow a backslash in a literal, besides the octal digits. */
    private static final String ESCAPES = "btnfrs\"'\\";

    /** Reads the literal, and says which kind it is, by the quotes it opens with. */
    TokenKind read(final Cursor cursor) {
        if (cursor.matches("\"\"\"")) {
            textBlock(cursor);
            return TokenKind.TextBlock.INSTANCE;
        }
        if (cursor.peek() == '"') {
            string(cursor);
            return TokenKind.StringLiteral.INSTANCE;
        }
        character(cursor);
        return TokenKind.CharacterLiteral.INSTANCE;
    }

    private void string(final Cursor cursor) {
        final int start = cursor.mark();
        cursor.advance();
        while (true) {
            final int c = cursor.peek();
            if (c < 0 || c == '\n' || c == '\r') {
                cursor.error(start, start + 1, "unclosed string literal");
                break;
            }
            cursor.advance();
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                escape(cursor, false);
            }
        }
    }

    private void character(final Cursor cursor) {
        final int start = cursor.mark();
        cursor.advance();
        final int c = cursor.peek();
        if (c == '\'') {
            cursor.advance();
            cursor.error(start, cursor.mark(), "empty character literal");
        } else if (c < 0 || c == '\n' || c == '\r') {
            cursor.error(start, start + 1, "unclosed character literal");
        } else {
            cursor.advance();
            if (c == '\\') {
                escape(cursor, false);
            }
            if (cursor.peek() == '\'') {
                cursor.advance();
            } else {
                cursor.error(start, start + 1, "unclosed character literal");
            }
        }
    }

    /** A text block (JLS 3.10.6): its opening quotes and the end of their line, then all up to the closing quotes. */
    private void textBlock(final Cursor cursor) {
        final int start = cursor.mark();
        cursor.advance(3);
        while (cursor.peek() == ' ' || cursor.peek() == '\t' || cursor.peek() == '\f') {
            cursor.advance();
        }
        if (cursor.peek() != '\n' && cursor.peek() != '\r') {
            cursor.error(start, cursor.mark(), "illegal text block open delimiter sequence, missing line terminator");
        }
        while (!cursor.matches("\"\"\"")) {
            if (cursor.atEnd()) {
                cursor.error(start, start + 3, "unclosed text block");
                break;
            }
            final int c = cursor.peek();
            cursor.advance();
            if (c == '\\') {
                escape(cursor, true);
            }
        }
        cursor.advance(3);
    }

    /**
     * What follows a backslash in a literal (JLS 3.10.7): one of the escape letters, up to three octal digits, or, in a
     * text block, the end of the line. A line's end in a string is left for the string to find unclosed.
     */
    private void escape(final Cursor cursor, final boolean inTextBlock) {
        final int from = cursor.mark() - 1;
        final int c = cursor.peek();
        if (c >= '0' && c <= '7') {
            final int most = c <= '3' ? 3 : 2;
            for (int count = 0; count < most && cursor.peek() >= '0' && cursor.peek() <= '7'; count++) {
                cursor.advance();
            }
        } else if (c >= 0 && ESCAPES.indexOf(c) >= 0) {
            cursor.advance();
        } else if (inTextBlock && (c == '\n' || c == '\r')) {
            cursor.advance();
            if (c == '\r' && cursor.peek() == '\n') {
                cursor.advance();
            }
        } else if (c >= 0 && c != '\n' && c != '\r') {
            cursor.advance();
            cursor.error(from, cursor.mark(), "illegal escape character");
        }
    }
}
