package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Mutable;

import java.util.ArrayList;
import java.util.List;

/**
 * One pass of the lexer over one source text: where it is, and the tokens and the diagnostics found so far. It reads
 * whitespace, comments, words, separators and operators itself, and has {@link NumberLiterals} and
 * {@link QuotedLiterals} read the literals they know.
 */
final class Scan implements Mutable {

    private static final String SEPARATORS = "(){}[];,.@";

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
                emit(NumberLiterals.INSTANCE.read(cursor), start);
            } else if (c == '"' || c == '\'') {
                emit(QuotedLiterals.INSTANCE.read(cursor), start);
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
                cursor.error(start, start + 2, "unclosed comment");
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
        cursor.error(start, cursor.mark(), String.format("illegal character: '\\u%04x'", c));
    }

    /** The operators, longest first so the longest that fits is taken; {@code >} is never part of a longer one. */
    private List<String> operators() {
        return List.of("<<=", "->", "==", "<=", "!=", "&&", "||", "++", "--", "<<", "+=", "-=", "*=", "/=", "&=",
                "|=", "^=", "%=", "=", ">", "<", "!", "~", "?", ":", "+", "-", "*", "/", "&", "|", "^", "%");
    }

    private void emit(final TokenKind kind, final int start) {
        tokens.add(new Token(kind, cursor.written(start), cursor.written(cursor.mark())));
    }
}
