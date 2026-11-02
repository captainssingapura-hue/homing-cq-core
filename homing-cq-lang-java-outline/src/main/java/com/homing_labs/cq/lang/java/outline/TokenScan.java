package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.Diagnostic;
import com.homing_labs.cq.lang.java.lexer.Severity;
import com.homing_labs.cq.lang.java.lexer.Token;
import com.homing_labs.cq.lang.java.lexer.TokenKind;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.Mutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One reading of tokens, from a first one up to an end: the token being read, the one read last, and what the readings
 * of a file share - its diagnostics, the ranges left unread, and the queue of its documentation comments. Comments are
 * stepped over; the token being read is never one.
 *
 * <p>A file is read by a {@link FileScan}, and the body of each of its types by a {@link BodyScan}, each through a
 * reading of its own; so is the body of each of their member types, from a block of code lexed on its own. What any
 * declaration starts with - its annotations and modifier words - is read here, and so are names; a type's header is
 * read by {@link TypeHeaders}, and what the grammar asks of a header is checked by {@link HeaderChecks}.</p>
 */
final class TokenScan implements Mutable {

    final TokenStream stream;

    final List<Diagnostic> diagnostics;

    /** The tokens left for another stage, or not read at all: the bodies, and a file in a form not handled. */
    final List<TokenRange> unread;

    final DocCommentQueue docs;

    /** The token the reading stops at: the end of the file, or a body's closing brace. */
    private final int limit;

    private int at;

    private int last;

    /** A reading of the whole file. */
    TokenScan(final TokenStream stream) {
        this.stream = stream;
        this.diagnostics = new ArrayList<>();
        this.unread = new ArrayList<>();
        this.docs = new DocCommentQueue();
        this.limit = stream.size();
        this.last = -1;
        this.at = significant(0);
    }

    /** A reading of some of the tokens of a block of code lexed on its own: what it finds is its own. */
    TokenScan(final TokenStream block, final TokenRange tokens) {
        this.stream = block;
        this.diagnostics = new ArrayList<>();
        this.unread = new ArrayList<>();
        this.docs = new DocCommentQueue();
        this.limit = tokens.to();
        this.last = tokens.from() - 1;
        this.at = significant(tokens.from());
    }

    /** A reading of some of the tokens another reading has stepped over, sharing what it has found. */
    TokenScan(final TokenScan outer, final TokenRange tokens) {
        this.stream = outer.stream;
        this.diagnostics = outer.diagnostics;
        this.unread = outer.unread;
        this.docs = outer.docs;
        this.limit = tokens.to();
        this.last = tokens.from() - 1;
        this.at = significant(tokens.from());
    }

    /** The token being read: the end, once all are read. */
    int at() {
        return at;
    }

    /** The token read last. */
    int last() {
        return last;
    }

    /** Steps over all the tokens left, as if each were read. */
    void toEnd() {
        last = size() - 1;
        at = size();
    }

    /** Annotations and modifier words, in the order written; any number of each, none at all included. */
    List<ModifierItem> modifiers() {
        final List<ModifierItem> items = new ArrayList<>();
        while (at < size()) {
            final int start = at;
            if (is("@") && !is(next(at), "interface")) {
                items.add(annotation());
            } else if (keyword(at) && modifierWord(text(at))) {
                advance();
                items.add(new ModifierItem.Word(text(start), new TokenRange(start, last + 1)));
            } else if (is("sealed") && (identifier(next(at)) || keyword(next(at)) || is(next(at), "@"))) {
                advance();
                items.add(new ModifierItem.Word("sealed", new TokenRange(start, last + 1)));
            } else if (is("non") && is(next(at), "-") && is(next(next(at)), "sealed")) {
                final int minus = next(at);
                final int sealed = next(minus);
                if (stream.get(start).end() != stream.get(minus).start()
                        || stream.get(minus).end() != stream.get(sealed).start()) {
                    error("non-sealed is written without spaces", start, sealed + 1);
                }
                at = sealed;
                advance();
                items.add(new ModifierItem.Word("non-sealed", new TokenRange(start, last + 1)));
            } else {
                break;
            }
        }
        return items;
    }

    ModifierItem.Annotation annotation() {
        final int start = at;
        advance();
        final Name name = name();
        Optional<TokenRange> arguments = Optional.empty();
        if (is("(")) {
            final int open = at;
            skipParentheses();
            arguments = Optional.of(new TokenRange(open, last + 1));
        }
        return new ModifierItem.Annotation(name, arguments, new TokenRange(start, last + 1));
    }

    /** Reports the modifier words among the items: where they are written, only annotations may be. */
    void annotationsOnly(final List<ModifierItem> items) {
        new Modifiers(items).words()
                .forEach(word -> error("modifier " + word.word() + " not allowed here", word.range()));
    }

    /** A name, qualified or not: identifiers with dots between them. */
    Name name() {
        if (!identifier(at)) {
            error("<identifier> expected", at, Math.min(at + 1, size()));
            return new Name("", new TokenRange(at, at));
        }
        final int start = at;
        final StringBuilder text = new StringBuilder(text(at));
        advance();
        while (is(".") && identifier(next(at))) {
            advance();
            text.append('.').append(text(at));
            advance();
        }
        return new Name(text.toString(), new TokenRange(start, last + 1));
    }

    /** Steps over the braces here and all between them; whether the closing one was found before the end. */
    boolean skipBlock() {
        int depth = 0;
        while (at < size()) {
            if (is("{")) {
                depth++;
            } else if (is("}") && --depth == 0) {
                advance();
                return true;
            }
            advance();
        }
        return false;
    }

    /**
     * Steps over the parentheses here and all between them. A semicolon stops it short, unless it is in braces between
     * them - a lambda's body, or an anonymous class's - as nothing else in parentheses can hold one.
     */
    void skipParentheses() {
        final int open = at;
        int depth = 0;
        int braces = 0;
        while (at < size() && !(is(";") && braces == 0)) {
            if (is("(")) {
                depth++;
            } else if (is(")") && --depth == 0) {
                advance();
                return;
            } else if (is("{")) {
                braces++;
            } else if (is("}") && braces > 0) {
                braces--;
            }
            advance();
        }
        error("')' expected", open, open + 1);
    }

    /** The documentation comment right before the token, if there is one: the declaration the token starts takes it. */
    Optional<DocComment> attach(final int start) {
        for (int index = start - 1; index >= 0 && comment(index); index--) {
            if (stream.get(index).kind() instanceof TokenKind.DocComment) {
                return Optional.of(new DocComment(index));
            }
        }
        return Optional.empty();
    }

    int size() {
        return limit;
    }

    boolean comment(final int index) {
        return stream.get(index).kind().comment();
    }

    /** The first token from the index on that is not a comment, or the end. */
    int significant(final int from) {
        int index = from;
        while (index < size() && comment(index)) {
            index++;
        }
        return index;
    }

    int next(final int index) {
        return significant(index + 1);
    }

    void advance() {
        last = at;
        at = next(at);
    }

    String text(final int index) {
        return stream.text(stream.get(index));
    }

    boolean is(final String text) {
        return is(at, text);
    }

    /**
     * Whether the token at the index is a word or a symbol written so; a literal or a comment never is. The source is
     * compared in place: only a token written with a unicode escape, longer than its text, is translated first.
     */
    boolean is(final int index, final String text) {
        if (index >= size()) {
            return false;
        }
        final Token token = stream.get(index);
        final TokenKind kind = token.kind();
        if (!(kind instanceof TokenKind.Identifier || kind instanceof TokenKind.Keyword
                || kind instanceof TokenKind.Separator || kind instanceof TokenKind.Operator)) {
            return false;
        }
        final String source = stream.source().text();
        final int length = token.end() - token.start();
        if (length == text.length()) {
            return source.regionMatches(token.start(), text, 0, length);
        }
        return length > text.length() && source.substring(token.start(), token.end()).indexOf('\\') >= 0
                && text(index).equals(text);
    }

    boolean identifier(final int index) {
        return index < size() && stream.get(index).kind() instanceof TokenKind.Identifier;
    }

    boolean keyword(final int index) {
        return index < size() && stream.get(index).kind() instanceof TokenKind.Keyword;
    }

    /** Whether the keyword is a modifier, besides {@code sealed} and {@code non-sealed}, which are not keywords. */
    boolean modifierWord(final String keyword) {
        return switch (keyword) {
            case "public", "protected", "private", "abstract", "static", "final", "strictfp", "transient", "volatile",
                    "synchronized", "native", "default" -> true;
            default -> false;
        };
    }

    /** Whether the keyword names a primitive type. */
    boolean primitive(final String keyword) {
        return switch (keyword) {
            case "boolean", "byte", "char", "short", "int", "long", "float", "double" -> true;
            default -> false;
        };
    }

    void error(final String message, final TokenRange range) {
        error(message, range.from(), range.to());
    }

    void error(final String message, final int from, final int to) {
        report(Severity.Error.INSTANCE, message, from, to);
    }

    void warning(final String message, final TokenRange range) {
        warning(message, range.from(), range.to());
    }

    void warning(final String message, final int from, final int to) {
        report(Severity.Warning.INSTANCE, message, from, to);
    }

    /** A diagnostic over the tokens from the first up to the end; at the end of the file when there are none. */
    private void report(final Severity severity, final String message, final int from, final int to) {
        final int start = from < stream.size() ? stream.get(from).start() : stream.source().text().length();
        final int end = to > from && to <= stream.size() ? stream.get(to - 1).end() : start;
        diagnostics.add(new Diagnostic(severity, message, start, end));
    }
}
