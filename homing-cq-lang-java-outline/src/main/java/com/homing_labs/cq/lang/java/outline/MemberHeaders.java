package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The header of a method, a constructor or a field, read where a scan is, from after its modifiers - and its type
 * parameters, if it has any - to its end; and what the grammar asks of it as a member of its own type. Each gives the
 * part it reads, to be made once the part's range is known. What follows a header is stepped over, and left unread.
 */
record MemberHeaders() implements StatelessFunctionalObject {

    static final MemberHeaders INSTANCE = new MemberHeaders();

    Function<TokenRange, BodyPart> constructor(final TokenScan scan, final OwnType own, final int start,
                                               final Optional<DocComment> doc, final List<ModifierItem> modifiers,
                                               final List<TypeParameter> typeParameters) {
        final Name name = scan.simpleName();
        if (!name.text().equals(own.name())) {
            scan.error("invalid method declaration; return type required", name.range());
        } else if (own.interfaceLike()) {
            scan.error(HeaderChecks.INSTANCE.noun(own.keyword()) + " has no constructors", name.range());
        }
        final List<Parameter> parameters = ParameterLists.INSTANCE.read(scan);
        final List<TypeText> thrown = thrown(scan);
        final TokenRange header = new TokenRange(start, scan.last() + 1);
        scan.docs.flush(scan.at(), doc);
        final boolean bodiless = scan.is(";");
        final Optional<TokenRange> body = methodBody(scan);
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, own.keyword() instanceof TypeKeyword.Enum
                ? Set.of("private") : Set.of("public", "protected", "private"), false, header);
        if (bodiless) {
            scan.error("missing method body, or declare abstract", header);
        }
        return range -> new BodyPart.Constructor(doc, written, new TypeParameters(typeParameters), name,
                new Parameters(parameters), new TypeTexts(thrown), body, range);
    }

    Function<TokenRange, BodyPart> compactConstructor(final TokenScan scan, final OwnType own, final int start,
                                                      final Optional<DocComment> doc,
                                                      final List<ModifierItem> modifiers) {
        final Name name = scan.simpleName();
        if (!(own.keyword() instanceof TypeKeyword.Record)) {
            scan.error("only a record has a compact constructor", name.range());
        } else if (!name.text().equals(own.name())) {
            scan.error("invalid method declaration; return type required", name.range());
        }
        final TokenRange header = new TokenRange(start, scan.last() + 1);
        scan.docs.flush(scan.at(), doc);
        final TokenRange body = SkippedCode.INSTANCE.block(scan);
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, Set.of("public", "protected", "private"), false, header);
        return range -> new BodyPart.CompactConstructor(doc, written, name, body, range);
    }

    Function<TokenRange, BodyPart> method(final TokenScan scan, final OwnType own, final int start,
                                          final Optional<DocComment> doc, final List<ModifierItem> modifiers,
                                          final List<TypeParameter> typeParameters, final TypeText result,
                                          final Name name) {
        final List<Parameter> parameters = ParameterLists.INSTANCE.read(scan);
        final TokenRange dimensions = WrittenTypes.INSTANCE.dimensions(scan);
        final List<TypeText> thrown = thrown(scan);
        final TokenRange header = new TokenRange(start, scan.last() + 1);
        scan.docs.flush(scan.at(), doc);
        final boolean bodiless = scan.is(";") || scan.is("default");
        final Optional<TokenRange> defaultValue;
        final Optional<TokenRange> body;
        if (scan.is("default")) {
            scan.advance();
            defaultValue = Optional.of(SkippedCode.INSTANCE.elementValue(scan));
            body = Optional.empty();
            SkippedCode.INSTANCE.semicolon(scan);
        } else {
            defaultValue = Optional.empty();
            body = methodBody(scan);
        }
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, methodModifiers(own, written), false, header);
        notVar(scan, result);
        if (own.keyword() instanceof TypeKeyword.Annotation) {
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
            checkBody(scan, own, written, body, header);
        }
        return range -> new BodyPart.Method(doc, written, new TypeParameters(typeParameters), result, name,
                new Parameters(parameters), dimensions, new TypeTexts(thrown), defaultValue, body, range);
    }

    Function<TokenRange, BodyPart> field(final TokenScan scan, final OwnType own, final int start,
                                         final Optional<DocComment> doc, final List<ModifierItem> modifiers,
                                         final TypeText fieldType, final Name first) {
        final List<Declarator> declarators = new ArrayList<>();
        Name name = first;
        while (true) {
            final TokenRange dimensions = WrittenTypes.INSTANCE.dimensions(scan);
            scan.docs.flush(scan.at(), doc);
            Optional<TokenRange> initializer = Optional.empty();
            if (scan.is("=")) {
                scan.advance();
                final TokenRange expression = SkippedCode.INSTANCE.expression(scan);
                if (expression.isEmpty()) {
                    scan.error("illegal start of expression", scan.at(), Math.min(scan.at() + 1, scan.size()));
                } else {
                    scan.unread.add(expression);
                    initializer = Optional.of(expression);
                }
            } else if (own.interfaceLike()) {
                scan.error("= expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            }
            declarators.add(new Declarator(name, dimensions, initializer,
                    new TokenRange(name.range().from(), Math.max(name.range().from(), scan.last() + 1))));
            if (!scan.is(",")) {
                break;
            }
            scan.advance();
            name = scan.simpleName();
        }
        SkippedCode.INSTANCE.semicolon(scan);
        final Modifiers written = new Modifiers(modifiers);
        HeaderChecks.INSTANCE.checkModifiers(scan, written, own.interfaceLike() ? Set.of("public", "static", "final")
                : Set.of("public", "protected", "private", "static", "final", "transient", "volatile"), false,
                new TokenRange(start, scan.last() + 1));
        notVar(scan, fieldType);
        if (own.keyword() instanceof TypeKeyword.Record && !written.has("static")) {
            scan.error("field declaration must be static (consider replacing field with record component)",
                    first.range());
        }
        final Declarators read = new Declarators(declarators);
        return range -> new BodyPart.Field(doc, written, fieldType, read, range);
    }

    /** The modifiers a method may have in its own type, as javac masks them (Check.checkFlags). */
    private Set<String> methodModifiers(final OwnType own, final Modifiers written) {
        return switch (own.keyword()) {
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
    private void checkBody(final TokenScan scan, final OwnType own, final Modifiers modifiers,
                           final Optional<TokenRange> body, final TokenRange header) {
        final boolean declaredOnly = switch (own.keyword()) {
            case TypeKeyword.Annotation _ -> true;
            case TypeKeyword.Interface _ -> !(modifiers.has("default") || modifiers.has("static")
                    || modifiers.has("private"));
            case TypeKeyword.Class _, TypeKeyword.Enum _, TypeKeyword.Record _ -> modifiers.has("abstract")
                    || modifiers.has("native");
        };
        if (body.isEmpty() && !declaredOnly) {
            scan.error("missing method body, or declare abstract", header);
        } else if (body.isPresent() && declaredOnly) {
            scan.error(own.interfaceLike() ? "interface abstract methods cannot have body"
                    : modifiers.has("abstract") ? "abstract methods cannot have a body"
                    : "native methods cannot have a body", body.get());
        }
    }

    private List<TypeText> thrown(final TokenScan scan) {
        if (!scan.is("throws")) {
            return List.of();
        }
        scan.advance();
        return TypeHeaders.INSTANCE.types(scan, ",", Set.of());
    }

    /** A method's body, if a brace opens one; none if a semicolon is written in its place, which is stepped over. */
    private Optional<TokenRange> methodBody(final TokenScan scan) {
        if (scan.is("{")) {
            return Optional.of(SkippedCode.INSTANCE.block(scan));
        }
        if (scan.is(";")) {
            scan.advance();
            return Optional.empty();
        }
        scan.error("'{' or ';' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
        SkippedCode.INSTANCE.recover(scan);
        return Optional.empty();
    }

    /** {@code var} infers a local variable's type; a member's is written. */
    private void notVar(final TokenScan scan, final TypeText written) {
        if (written.range().to() == written.range().from() + 1 && scan.is(written.range().from(), "var")) {
            scan.error("'var' is not allowed here", written.range());
        }
    }
}
