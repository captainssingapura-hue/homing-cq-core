package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.Diagnostic;
import com.homing_labs.cq.lang.java.lexer.Diagnostics;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import hue.captains.singapura.tao.ontology.Mutable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * One reading of a type's body, between its braces: its parts, each a member's header read in full - a field's, a
 * method's, a constructor's, a member type's - or an initializer, an enum constant, or a semicolon. What follows a
 * header - a body, an initial value, the arguments an enum constant is made with - is stepped over, and left unread.
 *
 * <p>A top-level type's body passes the body of each of its member types on to the {@link NestedClassParser}, as a
 * block of code copied out of the file, to be read the same way - a member type's body, read so, passes on nothing.</p>
 */
final class BodyScan implements Mutable {

    /** The body's tokens, as this reads them: the file's, or those of a block of code. */
    private final TokenScan scan;

    /** The word the type whose body this is is declared with, and its name. */
    private final TypeKeyword ownKeyword;

    private final String ownName;

    /** Whether the body of each member type is read too: a top-level type's is, and no other. */
    private final boolean passesOn;

    /** Where each part found starts, and how to make it once its range is known. */
    private final List<Integer> starts = new ArrayList<>();

    private final List<Function<TokenRange, BodyPart>> parts = new ArrayList<>();

    /** Whether an enum constant was found among the members, which javac says once only. */
    private boolean misplaced;

    /** A reading of a top-level type's body, as the reading of the file has stepped over it. */
    BodyScan(final TokenScan outer, final TypeHeader type, final TokenRange inside) {
        this.scan = new TokenScan(outer, inside);
        this.ownKeyword = type.keyword();
        this.ownName = type.name().text();
        this.passesOn = true;
    }

    /** A reading of a member type's body, from a block of code lexed on its own. */
    BodyScan(final TokenStream block, final TypeKeyword keyword, final String name, final TokenRange inside) {
        this.scan = new TokenScan(block, inside);
        this.ownKeyword = keyword;
        this.ownName = name;
        this.passesOn = false;
    }

    /** The body read from a block of code: its parts, and what was found wrong in them, at the block's positions. */
    NestedBody runNested(final CodeBlock block) {
        final List<BodyPart> read = run();
        scan.docs.dangling(scan.stream, scan.unread).forEach(token -> scan.warning("documentation comment is not"
                + " attached to any declaration", token, token + 1));
        scan.diagnostics.sort(Comparator.comparingInt(Diagnostic::start));
        return new NestedBody(block, scan.stream, new BodyParts(read), new Diagnostics(scan.diagnostics));
    }

    List<BodyPart> run() {
        if (ownKeyword instanceof TypeKeyword.Enum) {
            enumConstants();
        }
        while (scan.at() < scan.size()) {
            final int start = scan.at();
            final List<ModifierItem> modifiers = scan.modifiers();
            final Optional<TypeKeyword> keyword = TypeHeaders.INSTANCE.keyword(scan);
            if (keyword.isPresent()) {
                typeDeclaration(start, modifiers, keyword.get());
            } else if (scan.is("{")) {
                initializer(start, modifiers);
            } else if (scan.is(";") && modifiers.isEmpty()) {
                scan.advance();
                found(start, BodyPart.EmptyDeclaration::new);
            } else if (modifiers.isEmpty() && enumConstantShaped()) {
                if (!misplaced) {
                    scan.error("enum constant not expected here", scan.at(), scan.at() + 1);
                    misplaced = true;
                }
                enumConstant();
                if (scan.is(",")) {
                    scan.advance();
                }
            } else if (scan.is("<") || typeStart(scan.at())) {
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
        if (scan.is(",")) {
            scan.advance();
            if (scan.at() < scan.size() && !scan.is(";")) {
                scan.error("'}' or ';' expected", scan.at(), scan.at() + 1);
            }
        }
        while (scan.at() < scan.size() && !scan.is(";")) {
            if (!constantStart()) {
                scan.error("enum constant expected here", scan.at(), scan.at() + 1);
                return;
            }
            enumConstant();
            if (scan.is(",")) {
                scan.advance();
            } else if (scan.at() < scan.size() && !scan.is(";")) {
                scan.error("',', '}', or ';' expected", scan.at(), scan.at() + 1);
                if (!constantStart()) {
                    return;
                }
            }
        }
        if (scan.is(";")) {
            scan.advance();
        }
    }

    /** Whether an enum constant can start here: an annotation, or a name. */
    private boolean constantStart() {
        return scan.is("@") && !scan.is(scan.next(scan.at()), "interface")
                || scan.identifier(scan.at()) && !(scan.is("record") && scan.identifier(scan.next(scan.at())));
    }

    /**
     * Whether an enum constant starts here, among an enum's members, as javac takes one to: a name other than the
     * enum's, and what follows a constant's name.
     */
    private boolean enumConstantShaped() {
        final int after = scan.next(scan.at());
        return ownKeyword instanceof TypeKeyword.Enum && scan.identifier(scan.at())
                && !scan.text(scan.at()).equals(ownName)
                && (scan.is(after, "(") || scan.is(after, "{") || scan.is(after, ",") || scan.is(after, ";"));
    }

    private void enumConstant() {
        final int start = scan.at();
        final Optional<DocComment> doc = scan.attach(start);
        final List<ModifierItem> annotations = scan.modifiers();
        scan.annotationsOnly(annotations);
        final Name name = memberName();
        scan.docs.flush(scan.at(), doc);
        final Optional<TokenRange> arguments = scan.is("(") ? Optional.of(parenthesized()) : Optional.empty();
        final Optional<TokenRange> body = scan.is("{") ? Optional.of(block()) : Optional.empty();
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.EnumConstant(doc,
                new Modifiers(annotations), name, arguments, body, range));
    }

    private void typeDeclaration(final int start, final List<ModifierItem> modifiers, final TypeKeyword keyword) {
        final TypeHeader header = TypeHeaders.INSTANCE.read(scan, start, modifiers, keyword);
        final TypeBody body = TypeHeaders.INSTANCE.body(scan, this::startsPart);
        if (!(body.end() instanceof BodyEnd.Missing)) {
            scan.unread.add(body.range());
        }
        HeaderChecks.INSTANCE.checkHeader(scan, header, Optional.of(ownKeyword));
        final Optional<NestedBody> nested = passesOn && !(body.end() instanceof BodyEnd.Missing)
                ? Optional.of(passOn(header, body.range())) : Optional.empty();
        found(header.doc().map(DocComment::token).orElse(start), range -> new BodyPart.TypeDeclaration(header,
                body.range(), body.end(), nested, range));
    }

    /**
     * A member type's body, copied out of the file as a block of code and read by the {@link NestedClassParser}. What
     * it finds wrong is found wrong in the file, where it is there.
     */
    private NestedBody passOn(final TypeHeader header, final TokenRange body) {
        final int from = scan.stream.get(body.from()).start();
        final int to = scan.stream.get(body.to() - 1).end();
        final CodeBlock block = new CodeBlock(scan.stream.source().text().substring(from, to), from,
                scan.stream.source().position(from).line());
        final NestedBody read = NestedClassParser.INSTANCE.parse(header.keyword(), header.name().text(), block);
        read.diagnostics().items().forEach(found -> scan.diagnostics.add(new Diagnostic(found.severity(),
                found.message(), from + found.start(), from + found.end())));
        return read;
    }

    /** An initializer: a block, static or not; javac reads a block with any other modifier as no member at all. */
    private void initializer(final int start, final List<ModifierItem> modifiers) {
        final Optional<DocComment> doc = scan.attach(start);
        final Modifiers written = new Modifiers(modifiers);
        if (!written.annotations().isEmpty() || written.words().stream().anyMatch(word -> !word.word().equals(
                "static"))) {
            scan.error("illegal start of type", scan.at(), scan.at() + 1);
        } else {
            HeaderChecks.INSTANCE.checkModifiers(scan, written, Set.of("static"), false,
                    new TokenRange(start, scan.at()));
            if (interfaceLike()) {
                scan.error("initializers not allowed in interfaces", scan.at(), scan.at() + 1);
            } else if (ownKeyword instanceof TypeKeyword.Record && !written.has("static")) {
                scan.error("instance initializers not allowed in records", scan.at(), scan.at() + 1);
            }
        }
        scan.docs.clear(scan.at());
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
        final Optional<DocComment> doc = scan.attach(start);
        final List<TypeParameter> typeParameters = scan.is("<") ? TypeHeaders.INSTANCE.typeParameters(scan) : List.of();
        if (scan.identifier(scan.at()) && scan.is(scan.next(scan.at()), "(")) {
            constructor(start, doc, modifiers, typeParameters);
            return;
        }
        if (scan.identifier(scan.at()) && scan.is(scan.next(scan.at()), "{") && typeParameters.isEmpty()
                && (ownKeyword instanceof TypeKeyword.Record || scan.text(scan.at()).equals(ownName))) {
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
        if (scan.is("(")) {
            method(start, doc, modifiers, typeParameters, result, name);
        } else {
            if (!typeParameters.isEmpty()) {
                scan.error("'(' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            }
            field(start, doc, modifiers, result, name);
        }
    }

    private void constructor(final int start, final Optional<DocComment> doc, final List<ModifierItem> modifiers,
                             final List<TypeParameter> typeParameters) {
        final Name name = memberName();
        if (!name.text().equals(ownName)) {
            scan.error("invalid method declaration; return type required", name.range());
        } else if (interfaceLike()) {
            scan.error(HeaderChecks.INSTANCE.noun(ownKeyword) + " has no constructors", name.range());
        }
        final List<Parameter> parameters = parameters();
        final List<TypeText> thrown = thrown();
        final TokenRange header = new TokenRange(start, scan.last() + 1);
        scan.docs.flush(scan.at(), doc);
        final boolean bodiless = scan.is(";");
        final Optional<TokenRange> body = methodBody();
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, ownKeyword instanceof TypeKeyword.Enum ? Set.of("private")
                : Set.of("public", "protected", "private"), false, header);
        if (bodiless) {
            scan.error("missing method body, or declare abstract", header);
        }
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.Constructor(doc, written,
                new TypeParameters(typeParameters), name, new Parameters(parameters), new TypeTexts(thrown), body,
                range));
    }

    private void compactConstructor(final int start, final Optional<DocComment> doc,
                                    final List<ModifierItem> modifiers) {
        final Name name = memberName();
        if (!(ownKeyword instanceof TypeKeyword.Record)) {
            scan.error("only a record has a compact constructor", name.range());
        } else if (!name.text().equals(ownName)) {
            scan.error("invalid method declaration; return type required", name.range());
        }
        final TokenRange header = new TokenRange(start, scan.last() + 1);
        scan.docs.flush(scan.at(), doc);
        final TokenRange body = block();
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, Set.of("public", "protected", "private"), false, header);
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.CompactConstructor(doc, written, name,
                body, range));
    }

    private void method(final int start, final Optional<DocComment> doc, final List<ModifierItem> modifiers,
                        final List<TypeParameter> typeParameters, final TypeText result, final Name name) {
        final List<Parameter> parameters = parameters();
        final TokenRange dimensions = dimensions();
        final List<TypeText> thrown = thrown();
        final TokenRange header = new TokenRange(start, scan.last() + 1);
        scan.docs.flush(scan.at(), doc);
        final boolean bodiless = scan.is(";") || scan.is("default");
        final Optional<TokenRange> defaultValue;
        final Optional<TokenRange> body;
        if (scan.is("default")) {
            scan.advance();
            defaultValue = Optional.of(elementValue());
            body = Optional.empty();
            semicolon();
        } else {
            defaultValue = Optional.empty();
            body = methodBody();
        }
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, methodModifiers(written), false, header);
        notVar(result);
        if (ownKeyword instanceof TypeKeyword.Annotation) {
            if (!typeParameters.isEmpty()) {
                scan.error("elements in annotation interface declarations cannot be generic methods",
                        typeParameters.getFirst().range().from(), typeParameters.getLast().range().to());
            }
            if (!parameters.isEmpty()) {
                scan.error("elements in annotation interface declarations cannot declare formal parameters",
                        parameters.getFirst().range().from(), parameters.getLast().range().to());
            }
            if (!thrown.isEmpty()) {
                scan.error("throws clause not allowed in @interface members", thrown.getFirst().range().from(),
                        thrown.getLast().range().to());
            }
        } else if (defaultValue.isPresent()) {
            scan.error("default value only allowed in an annotation interface declaration", header);
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
        return switch (ownKeyword) {
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
        final boolean declaredOnly = switch (ownKeyword) {
            case TypeKeyword.Annotation _ -> true;
            case TypeKeyword.Interface _ -> !(modifiers.has("default") || modifiers.has("static")
                    || modifiers.has("private"));
            case TypeKeyword.Class _, TypeKeyword.Enum _, TypeKeyword.Record _ -> modifiers.has("abstract")
                    || modifiers.has("native");
        };
        if (body.isEmpty() && !declaredOnly) {
            scan.error("missing method body, or declare abstract", header);
        } else if (body.isPresent() && declaredOnly) {
            scan.error(interfaceLike() ? "interface abstract methods cannot have body"
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
            scan.docs.flush(scan.at(), doc);
            Optional<TokenRange> initializer = Optional.empty();
            if (scan.is("=")) {
                scan.advance();
                final TokenRange expression = expression();
                if (expression.isEmpty()) {
                    scan.error("illegal start of expression", scan.at(), Math.min(scan.at() + 1, scan.size()));
                } else {
                    scan.unread.add(expression);
                    initializer = Optional.of(expression);
                }
            } else if (interfaceLike()) {
                scan.error("= expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            }
            declarators.add(new Declarator(name, dimensions, initializer,
                    new TokenRange(name.range().from(), Math.max(name.range().from(), scan.last() + 1))));
            if (!scan.is(",")) {
                break;
            }
            scan.advance();
            name = memberName();
        }
        semicolon();
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, interfaceLike() ? Set.of("public", "static", "final")
                : Set.of("public", "protected", "private", "static", "final", "transient", "volatile"), false,
                new TokenRange(start, scan.last() + 1));
        notVar(fieldType);
        if (ownKeyword instanceof TypeKeyword.Record && !written.has("static")) {
            scan.error("field declaration must be static (consider replacing field with record component)",
                    first.range());
        }
        final Declarators read = new Declarators(declarators);
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.Field(doc, written, fieldType, read,
                range));
    }

    /** The parameters, in their parentheses (JLS 8.4.1): the receiver first, if it is written, and the formal ones. */
    private List<Parameter> parameters() {
        final List<Parameter> parameters = new ArrayList<>();
        scan.advance();
        while (scan.at() < scan.size() && !scan.is(")")) {
            final Optional<Parameter> parameter = parameter(parameters.isEmpty());
            if (parameter.isEmpty()) {
                closeParameters();
                return parameters;
            }
            parameters.add(parameter.get());
            if (scan.is(",")) {
                scan.advance();
            } else if (!scan.is(")")) {
                scan.error("',', ')', or '[' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
                closeParameters();
                return parameters;
            }
        }
        if (scan.is(")")) {
            scan.advance();
        } else {
            scan.error("')' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
        }
        checkVariableArity(parameters);
        return parameters;
    }

    /** One parameter, the receiver or a formal one; none, when no type is written where its type would be. */
    private Optional<Parameter> parameter(final boolean first) {
        final int start = scan.at();
        final List<ModifierItem> modifiers = scan.modifiers();
        TypeText parameterType = type();
        if (parameterType.range().isEmpty()) {
            return Optional.empty();
        }
        final boolean variable = scan.is(pastAnnotations(scan.at()), "...");
        if (variable) {
            while (scan.is("@")) {
                scan.annotation();
            }
            parameterType = new TypeText(new TokenRange(parameterType.range().from(), scan.last() + 1));
            scan.advance();
        }
        if (scan.is("this") || scan.identifier(scan.at()) && scan.is(scan.next(scan.at()), ".")
                && scan.is(scan.next(scan.next(scan.at())), "this")) {
            return Optional.of(receiver(start, modifiers, parameterType, first));
        }
        final Name name = memberName();
        final TokenRange dimensions = dimensions();
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, Set.of("final"), false,
                new TokenRange(start, scan.last() + 1));
        return Optional.of(new Parameter.Formal(written, parameterType, variable, name, dimensions,
                new TokenRange(start, scan.last() + 1)));
    }

    /** The receiver parameter, from its name on: {@code this}, alone or after a class's name (JLS 8.4.1). */
    private Parameter.Receiver receiver(final int start, final List<ModifierItem> modifiers,
                                        final TypeText parameterType, final boolean first) {
        scan.annotationsOnly(modifiers);
        if (!first) {
            scan.error("'this' names the receiver parameter only, which comes first", scan.at(), scan.at() + 1);
        }
        final int nameStart = scan.at();
        final StringBuilder text = new StringBuilder();
        while (!scan.is("this")) {
            text.append(scan.text(scan.at()));
            scan.advance();
        }
        scan.advance();
        return new Parameter.Receiver(new Modifiers(modifiers), parameterType,
                new Name(text.append("this").toString(), new TokenRange(nameStart, scan.last() + 1)),
                new TokenRange(start, scan.last() + 1));
    }

    /** Only the last formal parameter may be a variable arity one. */
    private void checkVariableArity(final List<Parameter> parameters) {
        final List<Parameter.Formal> formal = new Parameters(parameters).formal();
        for (int index = 0; index + 1 < formal.size(); index++) {
            if (formal.get(index).variable()) {
                scan.error("varargs parameter must be the last parameter", formal.get(index).range());
            }
        }
    }

    /** Steps over the parenthesis that closes the parameters; or up to a brace or a semicolon, which none can hold. */
    private void closeParameters() {
        int depth = 0;
        while (scan.at() < scan.size() && !scan.is("{") && !scan.is(";")) {
            if (scan.is("(")) {
                depth++;
            } else if (scan.is(")") && depth-- == 0) {
                scan.advance();
                return;
            }
            scan.advance();
        }
    }

    private List<TypeText> thrown() {
        if (!scan.is("throws")) {
            return List.of();
        }
        scan.advance();
        return TypeHeaders.INSTANCE.types(scan, ",", Set.of());
    }

    /** A method's body, if a brace opens one; none if a semicolon is written in its place, which is stepped over. */
    private Optional<TokenRange> methodBody() {
        if (scan.is("{")) {
            return Optional.of(block());
        }
        if (scan.is(";")) {
            scan.advance();
            return Optional.empty();
        }
        scan.error("'{' or ';' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
        recover();
        return Optional.empty();
    }

    /**
     * A type as written, from the token here (JLS 4.1): a primitive type or void, or a class type - names with dots
     * between them, each perhaps annotated and given type arguments - and brackets after it, each pair perhaps
     * annotated. An empty range, and a diagnostic, where no type starts.
     */
    private TypeText type() {
        final int start = scan.at();
        if (scan.keyword(scan.at()) && (scan.primitive(scan.text(scan.at())) || scan.is("void"))) {
            scan.advance();
        } else if (scan.identifier(scan.at())) {
            scan.advance();
            while (true) {
                if (scan.is("<")) {
                    typeArguments();
                } else if (scan.is(".") && scan.identifier(pastAnnotations(scan.next(scan.at())))) {
                    scan.advance();
                    while (scan.is("@")) {
                        scan.annotation();
                    }
                    scan.advance();
                } else {
                    break;
                }
            }
        } else {
            scan.error("<identifier> expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            return new TypeText(new TokenRange(start, start));
        }
        dimensions();
        return new TypeText(new TokenRange(start, scan.last() + 1));
    }

    private boolean typeStart(final int index) {
        return scan.identifier(index)
                || scan.keyword(index) && (scan.primitive(scan.text(index)) || scan.is(index, "void"));
    }

    /** Steps over type arguments, in their angle brackets; a diagnostic where none closes them before a type ends. */
    private void typeArguments() {
        final int open = scan.at();
        int depth = 0;
        while (scan.at() < scan.size() && !scan.is(";") && !scan.is("{") && !scan.is("=") && !scan.is(")")) {
            if (scan.is("@")) {
                scan.annotation();
                continue;
            }
            if (scan.is("<")) {
                depth++;
            } else if (scan.is(">") && --depth == 0) {
                scan.advance();
                return;
            }
            scan.advance();
        }
        scan.error("'>' expected", open, scan.last() + 1);
    }

    /** Brackets written after a type or a name, each pair perhaps annotated: an empty range where there are none. */
    private TokenRange dimensions() {
        final int start = scan.at();
        while (scan.is(pastAnnotations(scan.at()), "[") && scan.is(scan.next(pastAnnotations(scan.at())), "]")) {
            while (scan.is("@")) {
                scan.annotation();
            }
            scan.advance();
            scan.advance();
        }
        return new TokenRange(start, scan.at() == start ? start : scan.last() + 1);
    }

    /** The token after the annotations written from the index on: the index itself, when none is. */
    private int pastAnnotations(final int from) {
        int index = from;
        while (scan.is(index, "@") && !scan.is(scan.next(index), "interface")) {
            index = scan.next(index);
            while (scan.identifier(index) && scan.is(scan.next(index), ".")
                    && scan.identifier(scan.next(scan.next(index)))) {
                index = scan.next(scan.next(index));
            }
            if (scan.identifier(index)) {
                index = scan.next(index);
            }
            if (scan.is(index, "(")) {
                int depth = 0;
                while (index < scan.size() && !scan.is(index, ";")) {
                    if (scan.is(index, "(")) {
                        depth++;
                    } else if (scan.is(index, ")") && --depth == 0) {
                        index = scan.next(index);
                        break;
                    }
                    index = scan.next(index);
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
        final int start = scan.at();
        int depth = 0;
        while (scan.at() < scan.size() && !(depth == 0 && (scan.is(",") || scan.is(";")))) {
            if (scan.is("(") || scan.is("[") || scan.is("{")) {
                depth++;
            } else if (scan.is(")") || scan.is("]") || scan.is("}")) {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (depth == 0 && scan.is("<")) {
                final int past = pastTypeArguments(scan.at());
                if (past > scan.at()) {
                    while (scan.at() < past) {
                        scan.advance();
                    }
                    continue;
                }
            }
            scan.advance();
        }
        return new TokenRange(start, scan.at() == start ? start : scan.last() + 1);
    }

    /**
     * The token after the type arguments the angle bracket at the index opens; the index itself, if what follows the
     * bracket is not all what type arguments hold, as a less-than's operands are not.
     */
    private int pastTypeArguments(final int from) {
        int depth = 0;
        int index = from;
        while (index < scan.size()) {
            if (scan.is(index, "<")) {
                depth++;
            } else if (scan.is(index, ">")) {
                if (--depth == 0) {
                    return scan.next(index);
                }
            } else if (scan.is(index, "@")) {
                final int past = pastAnnotations(index);
                if (past == index) {
                    return from;
                }
                index = past;
                continue;
            } else if (!(scan.identifier(index) || scan.is(index, ".") || scan.is(index, ",") || scan.is(index, "?")
                    || scan.is(index, "&") || scan.is(index, "[") || scan.is(index, "]") || scan.is(index, "extends")
                    || scan.is(index, "super") || scan.keyword(index) && scan.primitive(scan.text(index)))) {
                return from;
            }
            index = scan.next(index);
        }
        return from;
    }

    /** An annotation element's default value as written, up to its semicolon, outside the brackets it opens. */
    private TokenRange elementValue() {
        final int start = scan.at();
        int depth = 0;
        while (scan.at() < scan.size() && !(depth == 0 && scan.is(";"))) {
            if (scan.is("(") || scan.is("[") || scan.is("{")) {
                depth++;
            } else if (scan.is(")") || scan.is("]") || scan.is("}")) {
                if (depth == 0) {
                    break;
                }
                depth--;
            }
            scan.advance();
        }
        if (scan.at() == start) {
            scan.error("illegal start of expression", scan.at(), Math.min(scan.at() + 1, scan.size()));
            return new TokenRange(start, start);
        }
        final TokenRange range = new TokenRange(start, scan.last() + 1);
        scan.unread.add(range);
        return range;
    }

    /** A member's name: one identifier; an empty name, and a diagnostic, where it is missing. */
    private Name memberName() {
        if (!scan.identifier(scan.at())) {
            scan.error("<identifier> expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            return new Name("", new TokenRange(scan.at(), scan.at()));
        }
        final int index = scan.at();
        scan.advance();
        return new Name(scan.text(index), new TokenRange(index, index + 1));
    }

    /** {@code var} infers a local variable's type; a member's is written. */
    private void notVar(final TypeText written) {
        if (written.range().to() == written.range().from() + 1 && scan.is(written.range().from(), "var")) {
            scan.error("'var' is not allowed here", written.range());
        }
    }

    /** Steps over the braces here and all between them, which are left unread. */
    private TokenRange block() {
        final int open = scan.at();
        final TokenRange range = new TokenRange(open, scan.skipBlock() ? scan.last() + 1 : scan.size());
        scan.unread.add(range);
        return range;
    }

    /** Steps over the parentheses here and all between them, which are left unread. */
    private TokenRange parenthesized() {
        final int open = scan.at();
        scan.skipParentheses();
        final TokenRange range = new TokenRange(open, scan.last() + 1);
        scan.unread.add(range);
        return range;
    }

    private boolean interfaceLike() {
        return ownKeyword instanceof TypeKeyword.Interface || ownKeyword instanceof TypeKeyword.Annotation;
    }

    private void unrecognised(final int start) {
        scan.error("illegal start of type", scan.at(), Math.min(scan.at() + 1, scan.size()));
        if (scan.at() == start) {
            scan.advance();
        }
        recover();
        found(start, BodyPart.Unrecognised::new);
    }

    private void semicolon() {
        if (scan.is(";")) {
            scan.advance();
            return;
        }
        scan.error("';' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
        recover();
    }

    /**
     * Steps over what cannot be read: up to the next semicolon and over it, over the next block, or up to the next
     * token a part can start with.
     */
    private void recover() {
        while (scan.at() < scan.size() && !startsPart(scan.at())) {
            if (scan.is("{")) {
                scan.skipBlock();
                return;
            }
            if (scan.is(";")) {
                scan.advance();
                return;
            }
            scan.advance();
        }
    }

    private boolean startsPart(final int index) {
        return scan.is(index, "@") || scan.is(index, "class") || scan.is(index, "interface") || scan.is(index, "enum")
                || scan.is(index, "record") && scan.identifier(scan.next(index))
                || scan.keyword(index) && scan.modifierWord(scan.text(index));
    }

    private void found(final int start, final Function<TokenRange, BodyPart> part) {
        starts.add(start);
        parts.add(part);
    }

    private List<BodyPart> parts() {
        final List<BodyPart> made = new ArrayList<>();
        for (int index = 0; index < starts.size(); index++) {
            final int end = index + 1 < starts.size() ? starts.get(index + 1) : scan.size();
            made.add(parts.get(index).apply(new TokenRange(starts.get(index), end)));
        }
        return made;
    }
}
