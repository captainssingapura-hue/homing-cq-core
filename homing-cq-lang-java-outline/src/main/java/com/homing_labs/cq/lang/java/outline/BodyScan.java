package com.homing_labs.cq.lang.java.outline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * One reading of a type's body, between its braces: its parts, each a member's header read in full - a field's, a
 * method's, a constructor's, a member type's - or an initializer, an enum constant, or a semicolon. What follows a
 * header - a body, an initial value, the arguments an enum constant is made with - is stepped over, and left unread.
 */
final class BodyScan extends TokenScan {

    /** The header of the type whose body this is. */
    private final TypeHeader type;

    /** Where each part found starts, and how to make it once its range is known. */
    private final List<Integer> starts = new ArrayList<>();

    private final List<Function<TokenRange, BodyPart>> parts = new ArrayList<>();

    /** Whether an enum constant was found among the members, which javac says once only. */
    private boolean misplaced;

    BodyScan(final TokenScan outer, final TypeHeader type, final TokenRange inside) {
        super(outer, inside);
        this.type = type;
    }

    List<BodyPart> run() {
        if (type.keyword() instanceof TypeKeyword.Enum) {
            enumConstants();
        }
        while (at < size()) {
            final int start = at;
            final List<ModifierItem> modifiers = modifiers();
            final Optional<TypeKeyword> keyword = typeKeyword();
            if (keyword.isPresent()) {
                typeDeclaration(start, modifiers, keyword.get());
            } else if (is("{")) {
                initializer(start, modifiers);
            } else if (is(";") && modifiers.isEmpty()) {
                advance();
                found(start, BodyPart.EmptyDeclaration::new);
            } else if (modifiers.isEmpty() && enumConstantShaped()) {
                if (!misplaced) {
                    error("enum constant not expected here", at, at + 1);
                    misplaced = true;
                }
                enumConstant();
                if (is(",")) {
                    advance();
                }
            } else if (is("<") || typeStart(at)) {
                methodOrField(start, modifiers);
            } else {
                unrecognised(start);
            }
        }
        return parts();
    }

    /**
     * An enum's constants, up to the semicolon that ends them, if one does, which is the last constant's (JLS 8.9.1).
     * What cannot start a constant ends them, as javac reads it: it and the rest are read as the enum's members.
     */
    private void enumConstants() {
        if (is(",")) {
            advance();
            if (at < size() && !is(";")) {
                error("'}' or ';' expected", at, at + 1);
            }
        }
        while (at < size() && !is(";")) {
            if (!constantStart()) {
                error("enum constant expected here", at, at + 1);
                return;
            }
            enumConstant();
            if (is(",")) {
                advance();
            } else if (at < size() && !is(";")) {
                error("',', '}', or ';' expected", at, at + 1);
                if (!constantStart()) {
                    return;
                }
            }
        }
        if (is(";")) {
            advance();
        }
    }

    /** Whether an enum constant can start here: an annotation, or a name. */
    private boolean constantStart() {
        return is("@") && !is(next(at), "interface") || identifier(at) && !(is("record") && identifier(next(at)));
    }

    /**
     * Whether an enum constant starts here, among an enum's members, as javac takes one to: a name other than the
     * enum's, and what follows a constant's name.
     */
    private boolean enumConstantShaped() {
        return type.keyword() instanceof TypeKeyword.Enum && identifier(at) && !text(at).equals(type.name().text())
                && (is(next(at), "(") || is(next(at), "{") || is(next(at), ",") || is(next(at), ";"));
    }

    private void enumConstant() {
        final int start = at;
        final Optional<DocComment> doc = attach(start);
        final List<ModifierItem> annotations = modifiers();
        annotationsOnly(annotations);
        final Name name = memberName();
        docs.flush(at, doc);
        final Optional<TokenRange> arguments = is("(") ? Optional.of(parenthesized()) : Optional.empty();
        final Optional<TokenRange> body = is("{") ? Optional.of(block()) : Optional.empty();
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.EnumConstant(doc,
                new Modifiers(annotations), name, arguments, body, range));
    }

    private void typeDeclaration(final int start, final List<ModifierItem> modifiers, final TypeKeyword keyword) {
        final TypeHeader header = typeHeader(start, modifiers, keyword);
        final Body body = typeBody();
        if (!(body.end() instanceof BodyEnd.Missing)) {
            unread.add(body.range());
        }
        checkHeader(header, Optional.of(type.keyword()));
        found(header.doc().map(DocComment::token).orElse(start), range -> new BodyPart.TypeDeclaration(header,
                body.range(), body.end(), range));
    }

    /** An initializer: a block, static or not; javac reads a block with any other modifier as no member at all. */
    private void initializer(final int start, final List<ModifierItem> modifiers) {
        final Optional<DocComment> doc = attach(start);
        final Modifiers written = new Modifiers(modifiers);
        if (!written.annotations().isEmpty() || written.words().stream().anyMatch(word -> !word.word().equals(
                "static"))) {
            error("illegal start of type", at, at + 1);
        } else {
            checkModifiers(written, Set.of("static"), false, new TokenRange(start, at));
            if (interfaceLike()) {
                error("initializers not allowed in interfaces", at, at + 1);
            } else if (type.keyword() instanceof TypeKeyword.Record && !written.has("static")) {
                error("instance initializers not allowed in records", at, at + 1);
            }
        }
        docs.clear(at);
        final TokenRange body = block();
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.Initializer(doc, written, body,
                range));
    }

    /**
     * A method, a constructor or a field, after its modifiers: what follows its type parameters, if it has any, says
     * which - a name and a parenthesis a constructor, a type and a name a method or a field, and the parenthesis or
     * not which of them. A name and a brace is a record's compact constructor.
     */
    private void methodOrField(final int start, final List<ModifierItem> modifiers) {
        final Optional<DocComment> doc = attach(start);
        final List<TypeParameter> typeParameters = is("<") ? typeParameters() : List.of();
        if (identifier(at) && is(next(at), "(")) {
            constructor(start, doc, modifiers, typeParameters);
            return;
        }
        if (identifier(at) && is(next(at), "{") && typeParameters.isEmpty()
                && (type.keyword() instanceof TypeKeyword.Record || text(at).equals(type.name().text()))) {
            compactConstructor(start, doc, modifiers);
            return;
        }
        final TypeText result = type();
        if (result.range().isEmpty()) {
            recover();
            found(start, BodyPart.Unrecognised::new);
            return;
        }
        final Name name = memberName();
        if (is("(")) {
            method(start, doc, modifiers, typeParameters, result, name);
        } else {
            if (!typeParameters.isEmpty()) {
                error("'(' expected", at, Math.min(at + 1, size()));
            }
            field(start, doc, modifiers, result, name);
        }
    }

    private void constructor(final int start, final Optional<DocComment> doc, final List<ModifierItem> modifiers,
                             final List<TypeParameter> typeParameters) {
        final Name name = memberName();
        if (!name.text().equals(type.name().text())) {
            error("invalid method declaration; return type required", name.range());
        } else if (interfaceLike()) {
            error(noun(type.keyword()) + " has no constructors", name.range());
        }
        final List<Parameter> parameters = parameters();
        final List<TypeText> thrown = thrown();
        final TokenRange header = new TokenRange(start, last + 1);
        docs.flush(at, doc);
        final boolean bodiless = is(";");
        final Optional<TokenRange> body = methodBody();
        final Modifiers written = new Modifiers(modifiers);
        checkModifiers(written, type.keyword() instanceof TypeKeyword.Enum ? Set.of("private")
                : Set.of("public", "protected", "private"), false, header);
        if (bodiless) {
            error("missing method body, or declare abstract", header);
        }
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.Constructor(doc, written,
                new TypeParameters(typeParameters), name, new Parameters(parameters), new TypeTexts(thrown), body,
                range));
    }

    private void compactConstructor(final int start, final Optional<DocComment> doc,
                                    final List<ModifierItem> modifiers) {
        final Name name = memberName();
        if (!(type.keyword() instanceof TypeKeyword.Record)) {
            error("only a record has a compact constructor", name.range());
        } else if (!name.text().equals(type.name().text())) {
            error("invalid method declaration; return type required", name.range());
        }
        final TokenRange header = new TokenRange(start, last + 1);
        docs.flush(at, doc);
        final TokenRange body = block();
        final Modifiers written = new Modifiers(modifiers);
        checkModifiers(written, Set.of("public", "protected", "private"), false, header);
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.CompactConstructor(doc, written, name,
                body, range));
    }

    private void method(final int start, final Optional<DocComment> doc, final List<ModifierItem> modifiers,
                        final List<TypeParameter> typeParameters, final TypeText result, final Name name) {
        final List<Parameter> parameters = parameters();
        final TokenRange dimensions = dimensions();
        final List<TypeText> thrown = thrown();
        final TokenRange header = new TokenRange(start, last + 1);
        docs.flush(at, doc);
        final boolean bodiless = is(";") || is("default");
        final Optional<TokenRange> defaultValue;
        final Optional<TokenRange> body;
        if (is("default")) {
            advance();
            defaultValue = Optional.of(elementValue());
            body = Optional.empty();
            semicolon();
        } else {
            defaultValue = Optional.empty();
            body = methodBody();
        }
        final Modifiers written = new Modifiers(modifiers);
        checkModifiers(written, methodModifiers(written), false, header);
        notVar(result);
        if (type.keyword() instanceof TypeKeyword.Annotation) {
            if (!typeParameters.isEmpty()) {
                error("elements in annotation interface declarations cannot be generic methods",
                        typeParameters.getFirst().range().from(), typeParameters.getLast().range().to());
            }
            if (!parameters.isEmpty()) {
                error("elements in annotation interface declarations cannot declare formal parameters",
                        parameters.getFirst().range().from(), parameters.getLast().range().to());
            }
            if (!thrown.isEmpty()) {
                error("throws clause not allowed in @interface members", thrown.getFirst().range().from(),
                        thrown.getLast().range().to());
            }
        } else if (defaultValue.isPresent()) {
            error("default value only allowed in an annotation interface declaration", header);
        }
        if (defaultValue.isEmpty() && (bodiless || body.isPresent())) {
            checkBody(written, body, header);
        }
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.Method(doc, written,
                new TypeParameters(typeParameters), result, name, new Parameters(parameters), dimensions,
                new TypeTexts(thrown), defaultValue, body, range));
    }

    /** The modifiers a method may have in this type, as javac masks them (Check.checkFlags). */
    private Set<String> methodModifiers(final Modifiers written) {
        return switch (type.keyword()) {
            case TypeKeyword.Annotation _ -> Set.of("public", "abstract");
            case TypeKeyword.Interface _ -> written.has("default") || written.has("static") || written.has("private")
                    ? Set.of("public", "private", "abstract", "static", "strictfp", "default")
                    : Set.of("public", "abstract");
            case TypeKeyword.Record _ -> Set.of("public", "protected", "private", "abstract", "static", "final",
                    "synchronized", "strictfp");
            case TypeKeyword.Class _, TypeKeyword.Enum _ -> Set.of("public", "protected", "private", "abstract",
                    "static", "final", "synchronized", "native", "strictfp");
        };
    }

    /**
     * Whether a method has a body where it must, and none where it may not: an abstract method has none, nor a native
     * one, nor an interface's, unless it is a default, static or private one.
     */
    private void checkBody(final Modifiers modifiers, final Optional<TokenRange> body, final TokenRange header) {
        final boolean declaredOnly = switch (type.keyword()) {
            case TypeKeyword.Annotation _ -> true;
            case TypeKeyword.Interface _ -> !(modifiers.has("default") || modifiers.has("static")
                    || modifiers.has("private"));
            case TypeKeyword.Class _, TypeKeyword.Enum _, TypeKeyword.Record _ -> modifiers.has("abstract")
                    || modifiers.has("native");
        };
        if (body.isEmpty() && !declaredOnly) {
            error("missing method body, or declare abstract", header);
        } else if (body.isPresent() && declaredOnly) {
            error(interfaceLike() ? "interface abstract methods cannot have body"
                    : modifiers.has("abstract") ? "abstract methods cannot have a body"
                    : "native methods cannot have a body", body.get());
        }
    }

    private void field(final int start, final Optional<DocComment> doc, final List<ModifierItem> modifiers,
                       final TypeText fieldType, final Name first) {
        final List<Declarator> declarators = new ArrayList<>();
        Name name = first;
        while (true) {
            final TokenRange dimensions = dimensions();
            docs.flush(at, doc);
            Optional<TokenRange> initializer = Optional.empty();
            if (is("=")) {
                advance();
                final TokenRange expression = expression();
                if (expression.isEmpty()) {
                    error("illegal start of expression", at, Math.min(at + 1, size()));
                } else {
                    unread.add(expression);
                    initializer = Optional.of(expression);
                }
            } else if (interfaceLike()) {
                error("= expected", at, Math.min(at + 1, size()));
            }
            declarators.add(new Declarator(name, dimensions, initializer,
                    new TokenRange(name.range().from(), Math.max(name.range().from(), last + 1))));
            if (!is(",")) {
                break;
            }
            advance();
            name = memberName();
        }
        semicolon();
        final Modifiers written = new Modifiers(modifiers);
        checkModifiers(written, interfaceLike() ? Set.of("public", "static", "final")
                : Set.of("public", "protected", "private", "static", "final", "transient", "volatile"), false,
                new TokenRange(start, last + 1));
        notVar(fieldType);
        if (type.keyword() instanceof TypeKeyword.Record && !written.has("static")) {
            error("field declaration must be static (consider replacing field with record component)",
                    first.range());
        }
        final Declarators read = new Declarators(declarators);
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.Field(doc, written, fieldType, read,
                range));
    }

    /** The parameters, in their parentheses (JLS 8.4.1): the receiver first, if it is written, and the formal ones. */
    private List<Parameter> parameters() {
        final List<Parameter> parameters = new ArrayList<>();
        advance();
        while (at < size() && !is(")")) {
            final int start = at;
            final List<ModifierItem> modifiers = modifiers();
            TypeText parameterType = type();
            if (parameterType.range().isEmpty()) {
                closeParameters();
                return parameters;
            }
            final boolean variable = is(pastAnnotations(at), "...");
            if (variable) {
                while (is("@")) {
                    annotation();
                }
                parameterType = new TypeText(new TokenRange(parameterType.range().from(), last + 1));
                advance();
            }
            if (is("this") || identifier(at) && is(next(at), ".") && is(next(next(at)), "this")) {
                annotationsOnly(modifiers);
                if (!parameters.isEmpty()) {
                    error("'this' names the receiver parameter only, which comes first", at, at + 1);
                }
                final int nameStart = at;
                final StringBuilder text = new StringBuilder();
                while (!is("this")) {
                    text.append(text(at));
                    advance();
                }
                advance();
                parameters.add(new Parameter.Receiver(new Modifiers(modifiers), parameterType,
                        new Name(text.append("this").toString(), new TokenRange(nameStart, last + 1)),
                        new TokenRange(start, last + 1)));
            } else {
                final Name name = memberName();
                final TokenRange dimensions = dimensions();
                final Modifiers written = new Modifiers(modifiers);
                checkModifiers(written, Set.of("final"), false, new TokenRange(start, last + 1));
                parameters.add(new Parameter.Formal(written, parameterType, variable, name, dimensions,
                        new TokenRange(start, last + 1)));
            }
            if (is(",")) {
                advance();
            } else if (!is(")")) {
                error("',', ')', or '[' expected", at, Math.min(at + 1, size()));
                closeParameters();
                return parameters;
            }
        }
        if (is(")")) {
            advance();
        } else {
            error("')' expected", at, Math.min(at + 1, size()));
        }
        final List<Parameter.Formal> formal = new Parameters(parameters).formal();
        for (int index = 0; index + 1 < formal.size(); index++) {
            if (formal.get(index).variable()) {
                error("varargs parameter must be the last parameter", formal.get(index).range());
            }
        }
        return parameters;
    }

    /** Steps over the parenthesis that closes the parameters; or up to a brace or a semicolon, which none can hold. */
    private void closeParameters() {
        int depth = 0;
        while (at < size() && !is("{") && !is(";")) {
            if (is("(")) {
                depth++;
            } else if (is(")") && depth-- == 0) {
                advance();
                return;
            }
            advance();
        }
    }

    private List<TypeText> thrown() {
        if (!is("throws")) {
            return List.of();
        }
        advance();
        return types(",", Set.of());
    }

    /** A method's body, if a brace opens one; none if a semicolon is written in its place, which is stepped over. */
    private Optional<TokenRange> methodBody() {
        if (is("{")) {
            return Optional.of(block());
        }
        if (is(";")) {
            advance();
            return Optional.empty();
        }
        error("'{' or ';' expected", at, Math.min(at + 1, size()));
        recover();
        return Optional.empty();
    }

    /**
     * A type as written, from the token here (JLS 4.1): a primitive type or void, or a class type - names with dots
     * between them, each perhaps annotated and given type arguments - and brackets after it, each pair perhaps
     * annotated. An empty range, and a diagnostic, where no type starts.
     */
    private TypeText type() {
        final int start = at;
        if (keyword(at) && (PRIMITIVES.contains(text(at)) || is("void"))) {
            advance();
        } else if (identifier(at)) {
            advance();
            while (true) {
                if (is("<")) {
                    typeArguments();
                } else if (is(".") && identifier(pastAnnotations(next(at)))) {
                    advance();
                    while (is("@")) {
                        annotation();
                    }
                    advance();
                } else {
                    break;
                }
            }
        } else {
            error("<identifier> expected", at, Math.min(at + 1, size()));
            return new TypeText(new TokenRange(start, start));
        }
        dimensions();
        return new TypeText(new TokenRange(start, last + 1));
    }

    private boolean typeStart(final int index) {
        return identifier(index) || keyword(index) && (PRIMITIVES.contains(text(index)) || is(index, "void"));
    }

    /** Steps over type arguments, in their angle brackets; a diagnostic where none closes them before a type ends. */
    private void typeArguments() {
        final int open = at;
        int depth = 0;
        while (at < size() && !is(";") && !is("{") && !is("=") && !is(")")) {
            if (is("@")) {
                annotation();
                continue;
            }
            if (is("<")) {
                depth++;
            } else if (is(">") && --depth == 0) {
                advance();
                return;
            }
            advance();
        }
        error("'>' expected", open, last + 1);
    }

    /** Brackets written after a type or a name, each pair perhaps annotated: an empty range where there are none. */
    private TokenRange dimensions() {
        final int start = at;
        while (is(pastAnnotations(at), "[") && is(next(pastAnnotations(at)), "]")) {
            while (is("@")) {
                annotation();
            }
            advance();
            advance();
        }
        return new TokenRange(start, at == start ? start : last + 1);
    }

    /** The token after the annotations written from the index on: the index itself, when none is. */
    private int pastAnnotations(final int from) {
        int index = from;
        while (is(index, "@") && !is(next(index), "interface")) {
            index = next(index);
            while (identifier(index) && is(next(index), ".") && identifier(next(next(index)))) {
                index = next(next(index));
            }
            if (identifier(index)) {
                index = next(index);
            }
            if (is(index, "(")) {
                int depth = 0;
                while (index < size() && !is(index, ";")) {
                    if (is(index, "(")) {
                        depth++;
                    } else if (is(index, ")") && --depth == 0) {
                        index = next(index);
                        break;
                    }
                    index = next(index);
                }
            }
        }
        return index;
    }

    /**
     * A field's initial value as written, up to the comma or the semicolon that ends it, outside the brackets it opens.
     * Angle brackets holding only what type arguments hold are taken for type arguments, so a comma between them does
     * not end it: {@code new HashMap<String, Integer>()}.
     */
    private TokenRange expression() {
        final int start = at;
        int depth = 0;
        while (at < size() && !(depth == 0 && (is(",") || is(";")))) {
            if (is("(") || is("[") || is("{")) {
                depth++;
            } else if (is(")") || is("]") || is("}")) {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (depth == 0 && is("<")) {
                final int past = pastTypeArguments(at);
                if (past > at) {
                    while (at < past) {
                        advance();
                    }
                    continue;
                }
            }
            advance();
        }
        return new TokenRange(start, at == start ? start : last + 1);
    }

    /**
     * The token after the type arguments the angle bracket at the index opens; the index itself, if what follows the
     * bracket is not all what type arguments hold, as a less-than's operands are not.
     */
    private int pastTypeArguments(final int from) {
        int depth = 0;
        int index = from;
        while (index < size()) {
            if (is(index, "<")) {
                depth++;
            } else if (is(index, ">")) {
                if (--depth == 0) {
                    return next(index);
                }
            } else if (is(index, "@")) {
                final int past = pastAnnotations(index);
                if (past == index) {
                    return from;
                }
                index = past;
                continue;
            } else if (!(identifier(index) || is(index, ".") || is(index, ",") || is(index, "?") || is(index, "&")
                    || is(index, "[") || is(index, "]") || is(index, "extends") || is(index, "super")
                    || keyword(index) && PRIMITIVES.contains(text(index)))) {
                return from;
            }
            index = next(index);
        }
        return from;
    }

    /** An annotation element's default value as written, up to its semicolon, outside the brackets it opens. */
    private TokenRange elementValue() {
        final int start = at;
        int depth = 0;
        while (at < size() && !(depth == 0 && is(";"))) {
            if (is("(") || is("[") || is("{")) {
                depth++;
            } else if (is(")") || is("]") || is("}")) {
                if (depth == 0) {
                    break;
                }
                depth--;
            }
            advance();
        }
        if (at == start) {
            error("illegal start of expression", at, Math.min(at + 1, size()));
            return new TokenRange(start, start);
        }
        final TokenRange range = new TokenRange(start, last + 1);
        unread.add(range);
        return range;
    }

    /** A member's name: one identifier; an empty name, and a diagnostic, where it is missing. */
    private Name memberName() {
        if (!identifier(at)) {
            error("<identifier> expected", at, Math.min(at + 1, size()));
            return new Name("", new TokenRange(at, at));
        }
        final int index = at;
        advance();
        return new Name(text(index), new TokenRange(index, index + 1));
    }

    /** {@code var} infers a local variable's type; a member's is written. */
    private void notVar(final TypeText written) {
        if (written.range().to() == written.range().from() + 1 && is(written.range().from(), "var")) {
            error("'var' is not allowed here", written.range());
        }
    }

    /** Steps over the braces here and all between them, which are left unread. */
    private TokenRange block() {
        final int open = at;
        final TokenRange range = new TokenRange(open, skipBlock() ? last + 1 : size());
        unread.add(range);
        return range;
    }

    /** Steps over the parentheses here and all between them, which are left unread. */
    private TokenRange parenthesized() {
        final int open = at;
        skipParentheses();
        final TokenRange range = new TokenRange(open, last + 1);
        unread.add(range);
        return range;
    }

    private boolean interfaceLike() {
        return type.keyword() instanceof TypeKeyword.Interface || type.keyword() instanceof TypeKeyword.Annotation;
    }

    private void unrecognised(final int start) {
        error("illegal start of type", at, Math.min(at + 1, size()));
        if (at == start) {
            advance();
        }
        recover();
        found(start, BodyPart.Unrecognised::new);
    }

    private void semicolon() {
        if (is(";")) {
            advance();
            return;
        }
        error("';' expected", at, Math.min(at + 1, size()));
        recover();
    }

    /**
     * Steps over what cannot be read: up to the next semicolon and over it, over the next block, or up to the next
     * token a part can start with.
     */
    private void recover() {
        while (at < size() && !startsPart(at)) {
            if (is("{")) {
                skipBlock();
                return;
            }
            if (is(";")) {
                advance();
                return;
            }
            advance();
        }
    }

    @Override
    boolean startsPart(final int index) {
        return is(index, "@") || is(index, "class") || is(index, "interface") || is(index, "enum")
                || is(index, "record") && identifier(next(index))
                || keyword(index) && MODIFIER_KEYWORDS.contains(text(index));
    }

    private void found(final int start, final Function<TokenRange, BodyPart> part) {
        starts.add(start);
        parts.add(part);
    }

    private List<BodyPart> parts() {
        final List<BodyPart> made = new ArrayList<>();
        for (int index = 0; index < starts.size(); index++) {
            final int end = index + 1 < starts.size() ? starts.get(index + 1) : size();
            made.add(parts.get(index).apply(new TokenRange(starts.get(index), end)));
        }
        return made;
    }
}
