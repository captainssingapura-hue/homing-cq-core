package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.Diagnostic;
import com.homing_labs.cq.lang.java.lexer.Diagnostics;
import com.homing_labs.cq.lang.java.lexer.Severity;
import com.homing_labs.cq.lang.java.lexer.TokenKind;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.Mutable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * One reading of a file's top level: the token being read, the phase the file is in - before its imports, among
 * them, or among its types - and the parts and the diagnostics found so far. Comments are stepped over; the token
 * being read is never one.
 */
final class FileScan implements Mutable {

    private static final Set<String> MODIFIER_KEYWORDS = Set.of("public", "protected", "private", "abstract",
            "static", "final", "strictfp", "transient", "volatile", "synchronized", "native", "default");

    /** Words no type may be named, so the grammar can read them as keywords where it has them (JLS 3.9). */
    private static final Set<String> RESTRICTED_NAMES = Set.of("permits", "record", "sealed", "var", "yield");

    private static final Set<String> PRIMITIVES = Set.of("boolean", "byte", "char", "short", "int", "long", "float",
            "double");

    private static final Set<String> CLAUSE_WORDS = Set.of("extends", "implements", "permits");

    /** Modifiers that place a type in another type, which a top-level type is not. */
    private static final Set<String> MEMBER_ONLY = Set.of("protected", "private", "static");

    /** Pairs of modifiers no type may have both of. */
    private static final List<List<String>> CONTRADICTIONS = List.of(List.of("abstract", "final"),
            List.of("final", "sealed"), List.of("final", "non-sealed"), List.of("sealed", "non-sealed"));

    private static final int BEFORE_IMPORTS = 0;

    private static final int AMONG_IMPORTS = 1;

    private static final int AMONG_TYPES = 2;

    private final TokenStream stream;

    private final String fileName;

    /** Where each part found starts, and how to make it once its range is known. */
    private final List<Integer> starts = new ArrayList<>();

    private final List<Function<TokenRange, FilePart>> parts = new ArrayList<>();

    private final List<Diagnostic> diagnostics = new ArrayList<>();

    /** The documentation comments a declaration has taken. */
    private final Set<Integer> attached = new HashSet<>();

    /** The tokens left for another stage, or not read at all: the bodies, and a file in a form not handled. */
    private final List<TokenRange> unread = new ArrayList<>();

    /** Semicolons that declare nothing, with no type after them yet: an import after one is an error. */
    private final List<Integer> semicolons = new ArrayList<>();

    private int at;

    private int last = -1;

    private int phase = BEFORE_IMPORTS;

    private boolean packaged;

    FileScan(final TokenStream stream) {
        this.stream = stream;
        this.fileName = stream.source().fileName();
    }

    Outline run() {
        at = significant(0);
        while (at < size()) {
            final int start = at;
            final List<ModifierItem> modifiers = modifiers();
            final Optional<TypeKeyword> keyword = typeKeyword();
            if (is("package")) {
                packageDeclaration(start, modifiers);
            } else if (is("import")) {
                importDeclaration(start, modifiers);
            } else if (is(";") && modifiers.isEmpty()) {
                emptyDeclaration(start);
            } else if (keyword.isPresent()) {
                typeDeclaration(start, modifiers, keyword.get());
            } else if (moduleDeclaration()) {
                notHandled(start, "a module declaration is not read yet: only ordinary compilation units are");
            } else if (memberShaped()) {
                notHandled(start, "a method or a field outside any class is not read yet: only ordinary compilation"
                        + " units are");
            } else {
                unrecognised(start);
            }
        }
        danglingDocComments();
        diagnostics.sort(Comparator.comparingInt(Diagnostic::start));
        return new Outline(stream, new FileParts(parts()), new Diagnostics(diagnostics));
    }

    private void packageDeclaration(final int start, final List<ModifierItem> modifiers) {
        final boolean packageInfo = fileName.equals("package-info.java");
        final Optional<DocComment> doc = packageInfo ? attach(start) : Optional.empty();
        final Modifiers written = new Modifiers(modifiers);
        written.words().forEach(word -> error("modifier " + word.word() + " not allowed here", word.range()));
        if (!packageInfo && !written.annotations().isEmpty()) {
            error("package annotations should be in file package-info.java",
                    written.annotations().getFirst().range().from(), written.annotations().getLast().range().to());
        }
        if (packaged) {
            error("a file declares one package only", at, at + 1);
        } else if (phase != BEFORE_IMPORTS) {
            error("the package declaration comes before the imports and the types", at, at + 1);
        }
        packaged = true;
        advance();
        final Name name = name();
        semicolon();
        phase = Math.max(phase, AMONG_IMPORTS);
        found(doc.map(DocComment::token).orElse(start),
                range -> new FilePart.PackageDeclaration(doc, written, name, range));
    }

    private void importDeclaration(final int start, final List<ModifierItem> modifiers) {
        if (!modifiers.isEmpty()) {
            error("an import takes no modifiers and no annotations", start, at);
        }
        if (phase == AMONG_TYPES) {
            error("the imports come before the types", at, at + 1);
        }
        semicolons.forEach(semicolon -> error("extraneous semicolon", semicolon, semicolon + 1));
        semicolons.clear();
        advance();
        final boolean statically = is("static");
        final boolean module = !statically && is("module") && identifier(at) && identifier(next(at));
        if (statically || module) {
            advance();
        }
        final Name name = name();
        final boolean onDemand = !module && is(".") && is(next(at), "*");
        if (onDemand) {
            advance();
            advance();
        } else if (!module && !name.text().isEmpty() && !name.text().contains(".")) {
            error("'.' expected", name.range());
        }
        semicolon();
        phase = Math.max(phase, AMONG_IMPORTS);
        final ImportKind kind = module ? ImportKind.Module.INSTANCE
                : statically && onDemand ? ImportKind.StaticOnDemand.INSTANCE
                : statically ? ImportKind.SingleStatic.INSTANCE
                : onDemand ? ImportKind.OnDemand.INSTANCE
                : ImportKind.SingleType.INSTANCE;
        found(start, range -> new FilePart.ImportDeclaration(kind, name, range));
    }

    private void emptyDeclaration(final int start) {
        advance();
        if (phase != AMONG_TYPES) {
            semicolons.add(start);
        }
        found(start, FilePart.EmptyDeclaration::new);
    }

    private void typeDeclaration(final int start, final List<ModifierItem> modifiers, final TypeKeyword keyword) {
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
        final TokenRange headerRange = new TokenRange(start, last + 1);
        if (!is("{")) {
            error("'{' expected", at, Math.min(at + 1, size()));
            while (at < size() && !is("{") && !startsPart(at)) {
                advance();
            }
        }
        final BodyEnd end;
        final TokenRange body;
        if (is("{")) {
            final int open = at;
            end = skipBlock() ? BodyEnd.Closed.INSTANCE : BodyEnd.Unterminated.INSTANCE;
            body = new TokenRange(open, end instanceof BodyEnd.Closed ? last + 1 : size());
            if (end instanceof BodyEnd.Unterminated) {
                error("reached end of file while parsing: the body of " + name.text() + " is never closed",
                        open, open + 1);
            }
            unread.add(body);
        } else {
            end = BodyEnd.Missing.INSTANCE;
            body = new TokenRange(at, at);
        }
        final TypeHeader header = new TypeHeader(doc, new Modifiers(modifiers), keyword, name,
                new TypeParameters(parameters), components.map(RecordComponents::new), new Clauses(clauses),
                headerRange);
        check(header);
        phase = AMONG_TYPES;
        semicolons.clear();
        found(doc.map(DocComment::token).orElse(start), range -> new FilePart.TypeDeclaration(header, body, end,
                range));
    }

    /** What the grammar asks of a type's header beyond its shape: which modifiers, parameters and clauses it takes. */
    private void check(final TypeHeader header) {
        final TypeKeyword keyword = header.keyword();
        final Set<String> allowed = allowed(keyword);
        final Set<String> written = new HashSet<>();
        for (final ModifierItem.Word word : header.modifiers().words()) {
            if (!written.add(word.word())) {
                error("repeated modifier", word.range());
            } else if (!allowed.contains(word.word()) || MEMBER_ONLY.contains(word.word())) {
                error("modifier " + word.word() + " not allowed here", word.range());
            }
        }
        for (final List<String> pair : CONTRADICTIONS) {
            if (written.containsAll(pair) && allowed.containsAll(pair)) {
                error("illegal combination of modifiers: " + pair.get(0) + " and " + pair.get(1), header.range());
            }
        }
        header.modifiers().words().stream().filter(word -> word.word().equals("strictfp")).findFirst()
                .ifPresent(word -> warning("strictfp is not required: since Java 17 all floating-point arithmetic is"
                        + " strict", word.range()));
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
        final String name = header.name().text();
        if (header.modifiers().has("public") && fileName.endsWith(".java") && !name.isEmpty()
                && !fileName.equals(name + ".java")) {
            error(keyword.word() + " " + name + " is public, should be declared in a file named " + name + ".java",
                    header.name().range());
        }
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

    private String noun(final TypeKeyword keyword) {
        return switch (keyword) {
            case TypeKeyword.Class _ -> "a class";
            case TypeKeyword.Interface _ -> "an interface";
            case TypeKeyword.Enum _ -> "an enum";
            case TypeKeyword.Record _ -> "a record";
            case TypeKeyword.Annotation _ -> "an annotation interface";
        };
    }

    /** Annotations and modifier words, in the order written; any number of each, none at all included. */
    private List<ModifierItem> modifiers() {
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

    private ModifierItem.Annotation annotation() {
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
    private void annotationsOnly(final List<ModifierItem> items) {
        new Modifiers(items).words()
                .forEach(word -> error("modifier " + word.word() + " not allowed here", word.range()));
    }

    private Optional<TypeKeyword> typeKeyword() {
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
    private Name name() {
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

    private List<TypeParameter> typeParameters() {
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
    private List<TypeText> types(final String separator, final Set<String> stops) {
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

    private boolean moduleDeclaration() {
        return is("module") && identifier(at) && identifier(next(at)) || is("open") && is(next(at), "module");
    }

    /** Whether what follows is shaped like a method or a field: a type, a name, and what follows a member's name. */
    private boolean memberShaped() {
        int index = is("<") ? pastAngles(at) : at;
        if (!(identifier(index) || is(index, "void") || keyword(index) && PRIMITIVES.contains(text(index)))) {
            return false;
        }
        index = next(index);
        while (index < size()) {
            if (is(index, ".") || is(index, "[") || is(index, "]")) {
                index = next(index);
            } else if (is(index, "<")) {
                index = pastAngles(index);
            } else if (identifier(index)) {
                final int after = next(index);
                if (is(after, "(") || is(after, "=") || is(after, ";") || is(after, ",")) {
                    return true;
                }
                index = after;
            } else {
                return false;
            }
        }
        return false;
    }

    private int pastAngles(final int from) {
        int depth = 0;
        int index = from;
        while (index < size() && !is(index, "{") && !is(index, ";")) {
            if (is(index, "<")) {
                depth++;
            } else if (is(index, ">") && --depth == 0) {
                return next(index);
            }
            index = next(index);
        }
        return index;
    }

    /** A form of file that is not read yet: the rest of it, with its documentation comment, is one part, unread. */
    private void notHandled(final int start, final String message) {
        final int from = attach(start).map(DocComment::token).orElse(start);
        error(message, at, Math.min(at + 1, size()));
        unread.add(new TokenRange(from, size()));
        last = size() - 1;
        at = size();
        found(from, FilePart.Unrecognised::new);
    }

    private void unrecognised(final int start) {
        if (at == start) {
            advance();
        }
        recover();
        error("class, interface, enum, or record expected", start, last + 1);
        found(start, FilePart.Unrecognised::new);
    }

    private void semicolon() {
        if (is(";")) {
            advance();
            return;
        }
        error("';' expected", at, Math.min(at + 1, size()));
        recover();
    }

    /** Steps over what cannot be read: up to the next semicolon and over it, or up to the next start of a part. */
    private void recover() {
        while (at < size() && !startsPart(at)) {
            if (is("{")) {
                skipBlock();
            } else if (is(";")) {
                advance();
                return;
            } else {
                advance();
            }
        }
    }

    private boolean startsPart(final int index) {
        return is(index, "package") || is(index, "import") || is(index, "@") || is(index, "class")
                || is(index, "interface") || is(index, "enum") || is(index, "record") && identifier(next(index))
                || keyword(index) && MODIFIER_KEYWORDS.contains(text(index));
    }

    /** Steps over the braces here and all between them; whether the closing one was found before the file ended. */
    private boolean skipBlock() {
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

    /** Steps over the parentheses here and all between them; a semicolon, which none can hold, stops it short. */
    private void skipParentheses() {
        final int open = at;
        int depth = 0;
        while (at < size() && !is(";")) {
            if (is("(")) {
                depth++;
            } else if (is(")") && --depth == 0) {
                advance();
                return;
            }
            advance();
        }
        error("')' expected", open, open + 1);
    }

    /** The documentation comment right before the token, if there is one: the declaration the token starts takes it. */
    private Optional<DocComment> attach(final int start) {
        for (int index = start - 1; index >= 0 && comment(index); index--) {
            if (stream.get(index).kind() instanceof TokenKind.DocComment) {
                attached.add(index);
                return Optional.of(new DocComment(index));
            }
        }
        return Optional.empty();
    }

    /** Warns of each documentation comment no declaration took, but none in what was not read or after the end. */
    private void danglingDocComments() {
        int end = size();
        while (end > 0 && comment(end - 1)) {
            end--;
        }
        for (int index = 0; index < end; index++) {
            final int token = index;
            if (stream.get(token).kind() instanceof TokenKind.DocComment && !attached.contains(token)
                    && unread.stream().noneMatch(range -> range.contains(token))) {
                warning("documentation comment is not attached to any declaration", token, token + 1);
            }
        }
    }

    private void found(final int start, final Function<TokenRange, FilePart> part) {
        starts.add(start);
        parts.add(part);
    }

    private List<FilePart> parts() {
        final List<FilePart> made = new ArrayList<>();
        final int first = starts.isEmpty() ? size() : starts.getFirst();
        if (first > 0) {
            made.add(new FilePart.Preamble(new TokenRange(0, first)));
        }
        for (int index = 0; index < starts.size(); index++) {
            final int end = index + 1 < starts.size() ? starts.get(index + 1) : size();
            made.add(parts.get(index).apply(new TokenRange(starts.get(index), end)));
        }
        return made;
    }

    private int size() {
        return stream.size();
    }

    private boolean comment(final int index) {
        return stream.get(index).kind().comment();
    }

    /** The first token from the index on that is not a comment, or the end. */
    private int significant(final int from) {
        int index = from;
        while (index < size() && comment(index)) {
            index++;
        }
        return index;
    }

    private int next(final int index) {
        return significant(index + 1);
    }

    private void advance() {
        last = at;
        at = next(at);
    }

    private String text(final int index) {
        return stream.text(stream.get(index));
    }

    private boolean is(final String text) {
        return is(at, text);
    }

    /** Whether the token at the index is a word or a symbol written so; a literal or a comment never is. */
    private boolean is(final int index, final String text) {
        if (index >= size()) {
            return false;
        }
        final TokenKind kind = stream.get(index).kind();
        return (kind instanceof TokenKind.Identifier || kind instanceof TokenKind.Keyword
                || kind instanceof TokenKind.Separator || kind instanceof TokenKind.Operator)
                && text(index).equals(text);
    }

    private boolean identifier(final int index) {
        return index < size() && stream.get(index).kind() instanceof TokenKind.Identifier;
    }

    private boolean keyword(final int index) {
        return index < size() && stream.get(index).kind() instanceof TokenKind.Keyword;
    }

    private void error(final String message, final TokenRange range) {
        error(message, range.from(), range.to());
    }

    private void error(final String message, final int from, final int to) {
        report(Severity.Error.INSTANCE, message, from, to);
    }

    private void warning(final String message, final TokenRange range) {
        warning(message, range.from(), range.to());
    }

    private void warning(final String message, final int from, final int to) {
        report(Severity.Warning.INSTANCE, message, from, to);
    }

    /** A diagnostic over the tokens from the first up to the end; at the end of the file when there are none. */
    private void report(final Severity severity, final String message, final int from, final int to) {
        final int start = from < size() ? stream.get(from).start() : stream.source().text().length();
        final int end = to > from && to <= size() ? stream.get(to - 1).end() : start;
        diagnostics.add(new Diagnostic(severity, message, start, end));
    }
}
