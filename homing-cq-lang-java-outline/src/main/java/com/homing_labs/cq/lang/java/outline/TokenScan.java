package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.Diagnostic;
import com.homing_labs.cq.lang.java.lexer.Severity;
import com.homing_labs.cq.lang.java.lexer.Token;
import com.homing_labs.cq.lang.java.lexer.TokenKind;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One reading of tokens, from a first one up to an end: the token being read, the one read last, and what the readings
 * of a file share - its diagnostics, the ranges left unread, and the queue of its documentation comments. Comments are
 * stepped over; the token being read is never one.
 *
 * <p>A file is read by a {@link FileScan}, and the body of each of its types by a {@link BodyScan} of its own; so is
 * the body of each of their member types, from a block of code lexed on its own. What reads a declaration's header -
 * its modifiers, its type parameters, a type's header in full - is here, for both, and so is what the grammar asks of
 * the modifiers written.</p>
 */
abstract sealed class TokenScan implements Mutable permits FileScan, BodyScan {

    static final Set<String> MODIFIER_KEYWORDS = Set.of("public", "protected", "private", "abstract", "static",
            "final", "strictfp", "transient", "volatile", "synchronized", "native", "default");

    /** Words no type may be named, so the grammar can read them as keywords where it has them (JLS 3.9). */
    static final Set<String> RESTRICTED_NAMES = Set.of("permits", "record", "sealed", "var", "yield");

    static final Set<String> PRIMITIVES = Set.of("boolean", "byte", "char", "short", "int", "long", "float",
            "double");

    private static final Set<String> CLAUSE_WORDS = Set.of("extends", "implements", "permits");

    /** Modifiers that place a type in another type, which a top-level type is not. */
    private static final Set<String> MEMBER_ONLY = Set.of("protected", "private", "static");

    /** The modifier words in the order of javac's flags for them, which is the order it names two of them in. */
    private static final List<String> FLAG_ORDER = List.of("public", "private", "protected", "static", "final",
            "synchronized", "volatile", "transient", "native", "abstract", "strictfp", "default", "sealed",
            "non-sealed");

    /**
     * Modifiers no declaration may have one of the first and one of the second of, in the order javac checks them
     * (Check.checkFlags); it says the first two it finds only. A type is not checked against the first nor the
     * seventh.
     */
    private static final List<List<List<String>>> CONTRADICTIONS = List.of(
            List.of(List.of("abstract"), List.of("private", "static", "default")),
            List.of(List.of("static", "private"), List.of("default")),
            List.of(List.of("abstract"), List.of("final", "native", "synchronized")),
            List.of(List.of("public"), List.of("private", "protected")),
            List.of(List.of("private"), List.of("public", "protected")),
            List.of(List.of("final"), List.of("volatile")),
            List.of(List.of("abstract", "native"), List.of("strictfp")),
            List.of(List.of("final"), List.of("sealed", "non-sealed")),
            List.of(List.of("sealed"), List.of("final", "non-sealed")));

    private static final Set<Integer> NOT_FOR_TYPES = Set.of(0, 6);

    final TokenStream stream;

    final List<Diagnostic> diagnostics;

    /** The tokens left for another stage, or not read at all: the bodies, and a file in a form not handled. */
    final List<TokenRange> unread;

    final DocCommentQueue docs;

    /** The token the reading stops at: the end of the file, or a body's closing brace. */
    private final int limit;

    int at;

    int last;

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

    /** Whether a part of what is read can start at the token: where reading what cannot be read stops. */
    abstract boolean startsPart(int index);

    /**
     * A type's header, from its modifiers, already read, to its body's brace: its documentation comment is the one
     * before its start. The comments queued are said where the header ends.
     */
    TypeHeader typeHeader(final int start, final List<ModifierItem> modifiers, final TypeKeyword keyword) {
        final Optional<DocComment> doc = attach(start);
        if (keyword instanceof TypeKeyword.Annotation) {
            advance();
        }
        advance();
        final Name name = typeName();
        final List<TypeParameter> parameters = is("<") ? typeParameters() : List.of();
        final Optional<List<RecordComponent>> components = is("(") ? Optional.of(recordComponents())
                : Optional.empty();
        final List<Clause> clauses = clauses();
        docs.flush(at, doc);
        return new TypeHeader(doc, new Modifiers(modifiers), keyword, name, new TypeParameters(parameters),
                components.map(RecordComponents::new), new Clauses(clauses), new TokenRange(start, last + 1));
    }

    /**
     * Steps over a type's body: from the brace that opens it - after what cannot be read, if the brace is not next - to
     * the brace that closes it, or the end. A body no brace opens is empty, where it should have been opened.
     */
    Body typeBody() {
        if (!is("{")) {
            error("'{' expected", at, Math.min(at + 1, size()));
            while (at < size() && !is("{") && !startsPart(at)) {
                advance();
            }
        }
        if (!is("{")) {
            return new Body(new TokenRange(at, at), BodyEnd.Missing.INSTANCE);
        }
        final int open = at;
        return skipBlock() ? new Body(new TokenRange(open, last + 1), BodyEnd.Closed.INSTANCE)
                : new Body(new TokenRange(open, size()), BodyEnd.Unterminated.INSTANCE);
    }

    /**
     * What the grammar asks of a type's header beyond its shape: which modifiers it takes - at the top level, or as a
     * member of a type declared with the enclosing keyword - and which parameters and clauses.
     */
    void checkHeader(final TypeHeader header, final Optional<TypeKeyword> enclosing) {
        final TypeKeyword keyword = header.keyword();
        final Set<String> allowed = new HashSet<>(allowed(keyword));
        if (enclosing.isEmpty()) {
            allowed.removeAll(MEMBER_ONLY);
        } else if (enclosing.get() instanceof TypeKeyword.Interface
                || enclosing.get() instanceof TypeKeyword.Annotation) {
            allowed.removeAll(Set.of("protected", "private"));
        }
        checkModifiers(header.modifiers(), allowed, true, header.range());
        final List<TypeParameter> parameters = header.typeParameters().items();
        if (!parameters.isEmpty() && (keyword instanceof TypeKeyword.Enum
                || keyword instanceof TypeKeyword.Annotation)) {
            error(noun(keyword) + " takes no type parameters", parameters.getFirst().range().from(),
                    parameters.getLast().range().to());
        }
        if (keyword instanceof TypeKeyword.Record && header.components().isEmpty()) {
            error("a record declares its components, in parentheses after its name", header.name().range());
        } else if (!(keyword instanceof TypeKeyword.Record) && header.components().isPresent()) {
            error("only a record has components", header.name().range());
        }
        checkClauses(keyword, header.clauses().items());
    }

    /**
     * What the grammar asks of the modifier words written before a declaration: each written once, each one it may
     * have, and - when it has none it may not - no two that contradict each other. strictfp, where it may be written,
     * is not needed.
     */
    void checkModifiers(final Modifiers modifiers, final Set<String> allowed, final boolean type,
                        final TokenRange declaration) {
        final Set<String> written = new HashSet<>();
        boolean disallowed = false;
        for (final ModifierItem.Word word : modifiers.words()) {
            if (!written.add(word.word())) {
                error("repeated modifier", word.range());
            } else if (!allowed.contains(word.word())) {
                error("modifier " + word.word() + " not allowed here", word.range());
                disallowed = true;
            }
        }
        if (!disallowed) {
            contradiction(written, type).ifPresent(pair -> error("illegal combination of modifiers: " + pair,
                    declaration));
        }
        if (allowed.contains("strictfp")) {
            modifiers.words().stream().filter(word -> word.word().equals("strictfp")).findFirst()
                    .ifPresent(word -> warning("strictfp is not required: since Java 17 all floating-point"
                            + " arithmetic is strict", word.range()));
        }
    }

    /** The first two words written that contradict each other, as javac checks them in turn. */
    private Optional<String> contradiction(final Set<String> written, final boolean type) {
        for (int index = 0; index < CONTRADICTIONS.size(); index++) {
            if (type && NOT_FOR_TYPES.contains(index)) {
                continue;
            }
            final Optional<String> first = firstWritten(written, CONTRADICTIONS.get(index).get(0));
            final Optional<String> second = firstWritten(written, CONTRADICTIONS.get(index).get(1));
            if (first.isPresent() && second.isPresent()) {
                return Optional.of(first.get() + " and " + second.get());
            }
        }
        return Optional.empty();
    }

    private Optional<String> firstWritten(final Set<String> written, final List<String> words) {
        return FLAG_ORDER.stream().filter(words::contains).filter(written::contains).findFirst();
    }

    private void checkClauses(final TypeKeyword keyword, final List<Clause> clauses) {
        final Set<ClauseWord> seen = new HashSet<>();
        Optional<ClauseWord> furthest = Optional.empty();
        for (final Clause clause : clauses) {
            final ClauseWord word = clause.word();
            if (!takes(keyword, word)) {
                error(noun(keyword) + " has no " + word.word() + " clause", clause.range());
            } else if (!seen.add(word)) {
                error("a second " + word.word() + " clause", clause.range());
            } else if (furthest.isPresent() && word.order() < furthest.get().order()) {
                error("the " + word.word() + " clause comes before the " + furthest.get().word() + " clause",
                        clause.range());
            } else if (keyword instanceof TypeKeyword.Class && word instanceof ClauseWord.Extends
                    && clause.types().items().size() > 1) {
                error("a class extends one class only", clause.range());
            }
            if (furthest.isEmpty() || word.order() > furthest.get().order()) {
                furthest = Optional.of(word);
            }
        }
    }

    /** The modifiers a type declared with the word may have, wherever it is declared (JLS 8.1.1, 8.9, 8.10, 9.1.1). */
    private Set<String> allowed(final TypeKeyword keyword) {
        return switch (keyword) {
            case TypeKeyword.Class _ -> Set.of("public", "protected", "private", "abstract", "static", "final",
                    "sealed", "non-sealed", "strictfp");
            case TypeKeyword.Interface _ -> Set.of("public", "protected", "private", "abstract", "static", "sealed",
                    "non-sealed", "strictfp");
            case TypeKeyword.Enum _ -> Set.of("public", "protected", "private", "static", "strictfp");
            case TypeKeyword.Record _ -> Set.of("public", "protected", "private", "static", "final", "strictfp");
            case TypeKeyword.Annotation _ -> Set.of("public", "protected", "private", "abstract", "static",
                    "strictfp");
        };
    }

    /** Whether a type declared with the word has a clause starting with the other. */
    private boolean takes(final TypeKeyword keyword, final ClauseWord word) {
        return switch (keyword) {
            case TypeKeyword.Class _ -> true;
            case TypeKeyword.Interface _ -> !(word instanceof ClauseWord.Implements);
            case TypeKeyword.Enum _, TypeKeyword.Record _ -> word instanceof ClauseWord.Implements;
            case TypeKeyword.Annotation _ -> false;
        };
    }

    String noun(final TypeKeyword keyword) {
        return switch (keyword) {
            case TypeKeyword.Class _ -> "a class";
            case TypeKeyword.Interface _ -> "an interface";
            case TypeKeyword.Enum _ -> "an enum";
            case TypeKeyword.Record _ -> "a record";
            case TypeKeyword.Annotation _ -> "an annotation interface";
        };
    }

    /** Annotations and modifier words, in the order written; any number of each, none at all included. */
    List<ModifierItem> modifiers() {
        final List<ModifierItem> items = new ArrayList<>();
        while (at < size()) {
            final int start = at;
            if (is("@") && !is(next(at), "interface")) {
                items.add(annotation());
            } else if (keyword(at) && MODIFIER_KEYWORDS.contains(text(at))) {
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

    Optional<TypeKeyword> typeKeyword() {
        if (is("class")) {
            return Optional.of(TypeKeyword.Class.INSTANCE);
        }
        if (is("interface")) {
            return Optional.of(TypeKeyword.Interface.INSTANCE);
        }
        if (is("enum")) {
            return Optional.of(TypeKeyword.Enum.INSTANCE);
        }
        if (is("@") && is(next(at), "interface")) {
            return Optional.of(TypeKeyword.Annotation.INSTANCE);
        }
        if (is("record") && identifier(next(at))) {
            return Optional.of(TypeKeyword.Record.INSTANCE);
        }
        return Optional.empty();
    }

    /** A type's name: one identifier, which may not be a restricted one. */
    private Name typeName() {
        if (!identifier(at)) {
            error("<identifier> expected", at, Math.min(at + 1, size()));
            return new Name("", new TokenRange(at, at));
        }
        final int index = at;
        advance();
        final Name name = new Name(text(index), new TokenRange(index, index + 1));
        if (RESTRICTED_NAMES.contains(name.text())) {
            error("'" + name.text() + "' is a restricted name: it cannot name a type", name.range());
        }
        return name;
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

    List<TypeParameter> typeParameters() {
        final List<TypeParameter> parameters = new ArrayList<>();
        advance();
        if (is(">")) {
            error("<identifier> expected", at, at + 1);
        }
        while (at < size() && !is(">")) {
            final int start = at;
            final List<ModifierItem> annotations = modifiers();
            annotationsOnly(annotations);
            if (!identifier(at)) {
                error("<identifier> expected", at, Math.min(at + 1, size()));
                return parameters;
            }
            final Name name = typeName();
            List<TypeText> bounds = List.of();
            if (is("extends")) {
                advance();
                bounds = types("&", Set.of(",", ">"));
            }
            parameters.add(new TypeParameter(new Modifiers(annotations), name, new TypeTexts(bounds),
                    new TokenRange(start, last + 1)));
            if (is(",")) {
                advance();
            } else if (!is(">")) {
                error("'>' expected", at, Math.min(at + 1, size()));
                return parameters;
            }
        }
        if (is(">")) {
            advance();
        }
        return parameters;
    }

    private List<RecordComponent> recordComponents() {
        final List<RecordComponent> components = new ArrayList<>();
        advance();
        while (at < size() && !is(")")) {
            final int start = at;
            final List<ModifierItem> annotations = modifiers();
            annotationsOnly(annotations);
            final int typeStart = at;
            final List<Integer> read = new ArrayList<>();
            int angles = 0;
            int parentheses = 0;
            while (at < size() && !(parentheses == 0 && (is("{") || is(";") || is(")") || angles == 0 && is(",")))) {
                if (is("(")) {
                    parentheses++;
                } else if (is(")")) {
                    parentheses--;
                } else if (parentheses == 0 && is("<")) {
                    angles++;
                } else if (parentheses == 0 && is(">")) {
                    angles--;
                }
                read.add(at);
                advance();
            }
            final int count = read.size();
            final boolean variable = count >= 3 && is(read.get(count - 2), "...");
            final int typeTokens = count - (variable ? 2 : 1);
            if (count < 2 || typeTokens < 1 || !identifier(read.getLast())) {
                error("<identifier> expected", start, Math.max(start + 1, last + 1));
            } else {
                final int name = read.getLast();
                components.add(new RecordComponent(new Modifiers(annotations),
                        new TypeText(new TokenRange(typeStart, read.get(typeTokens - 1) + 1)), variable,
                        new Name(text(name), new TokenRange(name, name + 1)), new TokenRange(start, last + 1)));
            }
            if (is(",")) {
                advance();
            } else if (!is(")")) {
                error("')' expected", at, Math.min(at + 1, size()));
                return components;
            }
        }
        if (is(")")) {
            advance();
        }
        return components;
    }

    private List<Clause> clauses() {
        final List<Clause> clauses = new ArrayList<>();
        while (is("extends") || is("implements") || is("permits")) {
            final int start = at;
            final ClauseWord word = is("extends") ? ClauseWord.Extends.INSTANCE
                    : is("implements") ? ClauseWord.Implements.INSTANCE : ClauseWord.Permits.INSTANCE;
            advance();
            final List<TypeText> types = types(",", CLAUSE_WORDS);
            clauses.add(new Clause(word, new TypeTexts(types), new TokenRange(start, last + 1)));
        }
        return clauses;
    }

    /**
     * Types as written, with the separator between them. Each runs to the separator, or to one of the stops, outside
     * any angle brackets and parentheses it opens; a brace or a semicolon ends them all, as no type has one.
     */
    List<TypeText> types(final String separator, final Set<String> stops) {
        final List<TypeText> types = new ArrayList<>();
        while (true) {
            final int start = at;
            int angles = 0;
            int parentheses = 0;
            while (at < size()) {
                if (parentheses == 0 && (is("{") || is(";") || is(")"))
                        || parentheses == 0 && angles == 0 && (is(separator) || stops.stream().anyMatch(this::is))) {
                    break;
                }
                if (is("(")) {
                    parentheses++;
                } else if (is(")")) {
                    parentheses--;
                } else if (parentheses == 0 && is("<")) {
                    angles++;
                } else if (parentheses == 0 && is(">")) {
                    if (angles == 0) {
                        break;
                    }
                    angles--;
                }
                advance();
            }
            if (at == start) {
                error("<identifier> expected", at, Math.min(at + 1, size()));
            } else {
                types.add(new TypeText(new TokenRange(start, last + 1)));
                if (angles > 0) {
                    error("'>' expected", start, last + 1);
                }
            }
            if (!is(separator)) {
                return types;
            }
            advance();
        }
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

    /** A body as tokens, its braces included, and how it ends. */
    record Body(TokenRange range, BodyEnd end) implements Scalar {

        /** The tokens between the braces: up to the end, when no brace closes it. */
        TokenRange inside() {
            return new TokenRange(range.from() + 1, end instanceof BodyEnd.Closed ? range.to() - 1 : range.to());
        }
    }
}
