package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Mutable;

import java.util.ArrayList;
import java.util.List;

/** One pass of the lexer over one source text: where it is, and the tokens and the diagnostics found so far. */
final class Scan implements Mutable {

    private static final String SEPARATORS = "(){}[];,.@";

    /** What may follow a backslash in a literal, besides the octal digits. */
    private static final String ESCAPES = "btnfrs\"'\\";

    private static final String DECIMAL = "0123456789";

    private static final String HEXADECIMAL = "0123456789abcdefABCDEF";

    private static final String BINARY = "01";

    /** The character that may end a text, from the days when one marked where a file ended (JLS 3.5). */
    private static final int SUBSTITUTE = 0x1a;

    private final SourceText source;

    private final List<Token> tokens = new ArrayList<>();

    private final List<Diagnostic> diagnostics = new ArrayList<>();

    private final Cursor cursor;

    Scan(final SourceText source) {
        this.source = source;
        this.cursor = new Cursor(source.text(), diagnostics);
    }

    TokenStream run() {
        while (!cursor.atEnd()) {
            final int start = cursor.mark();
            final int c = cursor.peek();
            if (c == ' ' || c == '\t' || c == '\f' || c == '\n' || c == '\r' || endsText()) {
                cursor.advance();
            } else if (c == '/' && cursor.peek(1) == '/') {
                lineComment(start);
            } else if (c == '/' && cursor.peek(1) == '*') {
                blockComment(start);
            } else if (Character.isJavaIdentifierStart(cursor.codePoint())) {
                word(start);
            } else if (digit(c) || c == '.' && digit(cursor.peek(1))) {
                number(start);
            } else if (cursor.matches("\"\"\"")) {
                textBlock(start);
            } else if (c == '"') {
                string(start);
            } else if (c == '\'') {
                character(start);
            } else {
                symbol(start);
            }
        }
        return new TokenStream(source, new Tokens(tokens), new Diagnostics(diagnostics));
    }

    private boolean endsText() {
        return cursor.peek() == SUBSTITUTE && cursor.peek(1) < 0;
    }

    private boolean digit(final int c) {
        return c >= '0' && c <= '9';
    }

    private void lineComment(final int start) {
        while (!cursor.atEnd() && cursor.peek() != '\n' && cursor.peek() != '\r') {
            cursor.advance();
        }
        emit(TokenKind.LineComment.INSTANCE, start);
    }

    private void blockComment(final int start) {
        cursor.advance(2);
        final boolean doc = cursor.peek() == '*' && cursor.peek(1) != '/';
        while (!cursor.matches("*/")) {
            if (cursor.atEnd()) {
                error(start, start + 2, "unclosed comment");
                break;
            }
            cursor.advance();
        }
        cursor.advance(2);
        emit(doc ? TokenKind.DocComment.INSTANCE : TokenKind.BlockComment.INSTANCE, start);
    }

    private void word(final int start) {
        cursor.advanceCodePoint();
        while (!cursor.atEnd() && !endsText() && Character.isJavaIdentifierPart(cursor.codePoint())) {
            cursor.advanceCodePoint();
        }
        final String word = cursor.since(start);
        if (keyword(word)) {
            emit(TokenKind.Keyword.INSTANCE, start);
        } else if (word.equals("true") || word.equals("false")) {
            emit(TokenKind.BooleanLiteral.INSTANCE, start);
        } else if (word.equals("null")) {
            emit(TokenKind.NullLiteral.INSTANCE, start);
        } else {
            emit(TokenKind.Identifier.INSTANCE, start);
        }
    }

    /** Whether the word is a keyword (JLS 3.9): a reserved one, or {@code _}. */
    private boolean keyword(final String word) {
        return switch (word) {
            case "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
                    "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
                    "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
                    "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
                    "volatile", "while", "_" -> true;
            default -> false;
        };
    }

    /** A number (JLS 3.10.1, 3.10.2): its digits, its point, its exponent and its suffix, each where it may be. */
    private void number(final int start) {
        final boolean hexadecimal = cursor.peek() == '0' && (cursor.peek(1) == 'x' || cursor.peek(1) == 'X');
        final boolean binary = cursor.peek() == '0' && (cursor.peek(1) == 'b' || cursor.peek(1) == 'B');
        final boolean pointed = hexadecimal ? hexadecimalNumber(start)
                : binary ? binaryNumber(start) : decimalNumber(start);
        final boolean floating = suffix(start, hexadecimal, binary, pointed);
        if (!cursor.atEnd() && Character.isJavaIdentifierPart(cursor.codePoint())) {
            while (!cursor.atEnd() && Character.isJavaIdentifierPart(cursor.codePoint())) {
                cursor.advanceCodePoint();
            }
            error(start, cursor.mark(), "malformed number");
        }
        emit(floating ? TokenKind.FloatingPointLiteral.INSTANCE : TokenKind.IntegerLiteral.INSTANCE, start);
    }

    /**
     * A hexadecimal number's prefix, digits, point and binary exponent; whether they make it a floating-point one. A
     * point needs the exponent.
     */
    private boolean hexadecimalNumber(final int start) {
        cursor.advance(2);
        boolean floating = false;
        boolean any = digits(HEXADECIMAL, true);
        if (cursor.peek() == '.') {
            cursor.advance();
            floating = true;
            any |= digits(HEXADECIMAL, true);
        }
        if (!any) {
            error(start, cursor.mark(), "hexadecimal numbers must contain at least one hexadecimal digit");
        }
        if (cursor.peek() == 'p' || cursor.peek() == 'P') {
            floating = true;
            exponent(start);
        } else if (floating) {
            error(start, cursor.mark(), "malformed floating-point literal");
        }
        return floating;
    }

    /** A binary number's prefix and digits, which never make it a floating-point one. */
    private boolean binaryNumber(final int start) {
        cursor.advance(2);
        if (!digits(BINARY, true)) {
            error(start, cursor.mark(), "binary numbers must contain at least one binary digit");
        }
        return false;
    }

    /** A decimal number's digits, point and exponent; whether they make it a floating-point one. */
    private boolean decimalNumber(final int start) {
        boolean floating = false;
        digits(DECIMAL, false);
        if (cursor.peek() == '.') {
            cursor.advance();
            floating = true;
            digits(DECIMAL, true);
        }
        if (cursor.peek() == 'e' || cursor.peek() == 'E') {
            floating = true;
            exponent(start);
        }
        return floating;
    }

    /**
     * A number's suffix, where it may have one; whether the number, its digits floating-point or not, is a
     * floating-point one with it. A hexadecimal number takes {@code f} or {@code d} as a suffix only once it is a
     * floating-point one; before, they are digits.
     */
    private boolean suffix(final int start, final boolean hexadecimal, final boolean binary, final boolean floating) {
        final int suffix = cursor.peek();
        if (suffix == 'l' || suffix == 'L') {
            cursor.advance();
            if (floating) {
                error(start, cursor.mark(), "malformed floating-point literal");
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
    private boolean digits(final String allowed, final boolean followsSomething) {
        final int from = cursor.mark();
        boolean any = false;
        int previous = -1;
        while (!cursor.atEnd() && (allowed.indexOf(cursor.peek()) >= 0 || cursor.peek() == '_')) {
            any |= cursor.peek() != '_';
            previous = cursor.peek();
            cursor.advance();
        }
        if (previous == '_' || followsSomething && cursor.mark() > from && cursor.since(from).startsWith("_")) {
            error(from, cursor.mark(), "illegal underscore");
        }
        return any;
    }

    private void exponent(final int start) {
        cursor.advance();
        if (cursor.peek() == '+' || cursor.peek() == '-') {
            cursor.advance();
        }
        if (!digits(DECIMAL, true)) {
            error(start, cursor.mark(), "malformed floating-point literal");
        }
    }

    private void string(final int start) {
        cursor.advance();
        while (true) {
            final int c = cursor.peek();
            if (c < 0 || c == '\n' || c == '\r') {
                error(start, start + 1, "unclosed string literal");
                break;
            }
            cursor.advance();
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                escape(false);
            }
        }
        emit(TokenKind.StringLiteral.INSTANCE, start);
    }

    private void character(final int start) {
        cursor.advance();
        final int c = cursor.peek();
        if (c == '\'') {
            cursor.advance();
            error(start, cursor.mark(), "empty character literal");
        } else if (c < 0 || c == '\n' || c == '\r') {
            error(start, start + 1, "unclosed character literal");
        } else {
            cursor.advance();
            if (c == '\\') {
                escape(false);
            }
            if (cursor.peek() == '\'') {
                cursor.advance();
            } else {
                error(start, start + 1, "unclosed character literal");
            }
        }
        emit(TokenKind.CharacterLiteral.INSTANCE, start);
    }

    /** A text block (JLS 3.10.6): its opening quotes and the end of their line, then all up to the closing quotes. */
    private void textBlock(final int start) {
        cursor.advance(3);
        while (cursor.peek() == ' ' || cursor.peek() == '\t' || cursor.peek() == '\f') {
            cursor.advance();
        }
        if (cursor.peek() != '\n' && cursor.peek() != '\r') {
            error(start, cursor.mark(), "illegal text block open delimiter sequence, missing line terminator");
        }
        while (!cursor.matches("\"\"\"")) {
            if (cursor.atEnd()) {
                error(start, start + 3, "unclosed text block");
                break;
            }
            final int c = cursor.peek();
            cursor.advance();
            if (c == '\\') {
                escape(true);
            }
        }
        cursor.advance(3);
        emit(TokenKind.TextBlock.INSTANCE, start);
    }

    /**
     * What follows a backslash in a literal (JLS 3.10.7): one of the escape letters, up to three octal digits, or, in a
     * text block, the end of the line. A line's end in a string is left for the string to find unclosed.
     */
    private void escape(final boolean inTextBlock) {
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
            error(from, cursor.mark(), "illegal escape character");
        }
    }

    /** A separator or an operator, as long as one can be, or a character none starts with. */
    private void symbol(final int start) {
        if (cursor.matches("...") || cursor.matches("::")) {
            cursor.advance(cursor.matches("...") ? 3 : 2);
            emit(TokenKind.Separator.INSTANCE, start);
            return;
        }
        if (SEPARATORS.indexOf(cursor.peek()) >= 0) {
            cursor.advance();
            emit(TokenKind.Separator.INSTANCE, start);
            return;
        }
        for (final String operator : operators()) {
            if (cursor.matches(operator)) {
                cursor.advance(operator.length());
                emit(TokenKind.Operator.INSTANCE, start);
                return;
            }
        }
        final int c = cursor.codePoint();
        cursor.advanceCodePoint();
        emit(TokenKind.Error.INSTANCE, start);
        error(start, cursor.mark(), String.format("illegal character: '\\u%04x'", c));
    }

    /** The operators, longest first so the longest that fits is taken; {@code >} is never part of a longer one. */
    private List<String> operators() {
        return List.of("<<=", "->", "==", "<=", "!=", "&&", "||", "++", "--", "<<", "+=", "-=", "*=", "/=", "&=",
                "|=", "^=", "%=", "=", ">", "<", "!", "~", "?", ":", "+", "-", "*", "/", "&", "|", "^", "%");
    }

    private void emit(final TokenKind kind, final int start) {
        tokens.add(new Token(kind, cursor.written(start), cursor.written(cursor.mark())));
    }

    private void error(final int from, final int to, final String message) {
        diagnostics.add(new Diagnostic(Severity.Error.INSTANCE, message, cursor.written(from), cursor.written(to)));
    }
}
