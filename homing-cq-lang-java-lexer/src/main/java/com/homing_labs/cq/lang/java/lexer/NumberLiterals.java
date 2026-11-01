package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * Reads a number where the cursor is (JLS 3.10.1, 3.10.2): its digits, its point, its exponent and its suffix, each
 * where it may be. What is malformed is reported, and the number read to its end all the same.
 */
record NumberLiterals() implements StatelessFunctionalObject {

    static final NumberLiterals INSTANCE = new NumberLiterals();

    private static final String DECIMAL = "0123456789";

    private static final String HEXADECIMAL = "0123456789abcdefABCDEF";

    private static final String BINARY = "01";

    /** Reads the number, and says which kind of literal it is: an integer or a floating-point one. */
    TokenKind read(final Cursor cursor) {
        final int start = cursor.mark();
        final boolean hexadecimal = cursor.peek() == '0' && (cursor.peek(1) == 'x' || cursor.peek(1) == 'X');
        final boolean binary = cursor.peek() == '0' && (cursor.peek(1) == 'b' || cursor.peek(1) == 'B');
        final boolean pointed = hexadecimal ? hexadecimalNumber(cursor, start)
                : binary ? binaryNumber(cursor, start) : decimalNumber(cursor, start);
        final boolean floating = suffix(cursor, start, hexadecimal, binary, pointed);
        if (!cursor.atEnd() && Character.isJavaIdentifierPart(cursor.codePoint())) {
            while (!cursor.atEnd() && Character.isJavaIdentifierPart(cursor.codePoint())) {
                cursor.advanceCodePoint();
            }
            cursor.error(start, cursor.mark(), "malformed number");
        }
        return floating ? TokenKind.FloatingPointLiteral.INSTANCE : TokenKind.IntegerLiteral.INSTANCE;
    }

    /**
     * A hexadecimal number's prefix, digits, point and binary exponent; whether they make it a floating-point one. A
     * point needs the exponent.
     */
    private boolean hexadecimalNumber(final Cursor cursor, final int start) {
        cursor.advance(2);
        boolean floating = false;
        boolean any = digits(cursor, HEXADECIMAL, true);
        if (cursor.peek() == '.') {
            cursor.advance();
            floating = true;
            any |= digits(cursor, HEXADECIMAL, true);
        }
        if (!any) {
            cursor.error(start, cursor.mark(), "hexadecimal numbers must contain at least one hexadecimal digit");
        }
        if (cursor.peek() == 'p' || cursor.peek() == 'P') {
            floating = true;
            exponent(cursor, start);
        } else if (floating) {
            cursor.error(start, cursor.mark(), "malformed floating-point literal");
        }
        return floating;
    }

    /** A binary number's prefix and digits, which never make it a floating-point one. */
    private boolean binaryNumber(final Cursor cursor, final int start) {
        cursor.advance(2);
        if (!digits(cursor, BINARY, true)) {
            cursor.error(start, cursor.mark(), "binary numbers must contain at least one binary digit");
        }
        return false;
    }

    /** A decimal number's digits, point and exponent; whether they make it a floating-point one. */
    private boolean decimalNumber(final Cursor cursor, final int start) {
        boolean floating = false;
        digits(cursor, DECIMAL, false);
        if (cursor.peek() == '.') {
            cursor.advance();
            floating = true;
            digits(cursor, DECIMAL, true);
        }
        if (cursor.peek() == 'e' || cursor.peek() == 'E') {
            floating = true;
            exponent(cursor, start);
        }
        return floating;
    }

    /**
     * A number's suffix, where it may have one; whether the number, its digits floating-point or not, is a
     * floating-point one with it. A hexadecimal number takes {@code f} or {@code d} as a suffix only once it is a
     * floating-point one; before, they are digits.
     */
    private boolean suffix(final Cursor cursor, final int start, final boolean hexadecimal, final boolean binary,
                           final boolean floating) {
        final int suffix = cursor.peek();
        if (suffix == 'l' || suffix == 'L') {
            cursor.advance();
            if (floating) {
                cursor.error(start, cursor.mark(), "malformed floating-point literal");
            }
            return floating;
        }
        if ((suffix == 'f' || suffix == 'F' || suffix == 'd' || suffix == 'D') && !binary
                && (!hexadecimal || floating)) {
            cursor.advance();
            return true;
        }
        return floating;
    }

    /**
     * Digits of the allowed ones, and underscores between them; whether there were any digits. An underscore may not
     * end them, nor begin them where they follow a prefix, a point or an exponent.
     */
    private boolean digits(final Cursor cursor, final String allowed, final boolean followsSomething) {
        final int from = cursor.mark();
        boolean any = false;
        int previous = -1;
        while (!cursor.atEnd() && (allowed.indexOf(cursor.peek()) >= 0 || cursor.peek() == '_')) {
            any |= cursor.peek() != '_';
            previous = cursor.peek();
            cursor.advance();
        }
        if (previous == '_' || followsSomething && cursor.mark() > from && cursor.since(from).startsWith("_")) {
            cursor.error(from, cursor.mark(), "illegal underscore");
        }
        return any;
    }

    private void exponent(final Cursor cursor, final int start) {
        cursor.advance();
        if (cursor.peek() == '+' || cursor.peek() == '-') {
            cursor.advance();
        }
        if (!digits(cursor, DECIMAL, true)) {
            cursor.error(start, cursor.mark(), "malformed floating-point literal");
        }
    }
}
