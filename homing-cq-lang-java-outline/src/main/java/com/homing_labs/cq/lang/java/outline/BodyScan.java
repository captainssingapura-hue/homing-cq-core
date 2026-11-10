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
 * The header of a method, a constructor or a field is read by {@link MemberHeaders}, and what is stepped over by
 * {@link SkippedCode}.
 *
 * <p>A top-level type's body passes the body of each of its member types on to the {@link NestedClassParser}, as a
 * block of code copied out of the file, to be read the same way - a member type's body, read so, passes on nothing.</p>
 */
final class BodyScan implements Mutable {

    /** The body's tokens, as this reads them: the file's, or those of a block of code. */
    private final TokenScan scan;

    /** The type whose body this is. */
    private final OwnType own;

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
        this.own = new OwnType(type.keyword(), type.name().text());
        this.passesOn = true;
    }

    /** A reading of a member type's body, from a block of code lexed on its own. */
    BodyScan(final TokenStream block, final TypeKeyword keyword, final String name, final TokenRange inside) {
        this.scan = new TokenScan(block, inside);
        this.own = new OwnType(keyword, name);
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
        if (own.keyword() instanceof TypeKeyword.Enum) {
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
            } else if (scan.is("<") || WrittenTypes.INSTANCE.typeStart(scan, scan.at())) {
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
        return own.keyword() instanceof TypeKeyword.Enum && scan.identifier(scan.at())
                && !scan.text(scan.at()).equals(own.name())
                && (scan.is(after, "(") || scan.is(after, "{") || scan.is(after, ",") || scan.is(after, ";"));
    }

    private void enumConstant() {
        final int start = scan.at();
        final Optional<DocComment> doc = scan.attach(start);
        final List<ModifierItem> annotations = scan.modifiers();
        scan.annotationsOnly(annotations);
        final Name name = scan.simpleName();
        scan.docs.flush(scan.at(), doc);
        final Optional<TokenRange> arguments = scan.is("(") ? Optional.of(SkippedCode.INSTANCE.parenthesized(scan))
                : Optional.empty();
        final Optional<TokenRange> body = scan.is("{") ? Optional.of(SkippedCode.INSTANCE.block(scan))
                : Optional.empty();
        found(doc.map(DocComment::token).orElse(start), range -> new BodyPart.EnumConstant(doc,
                new Modifiers(annotations), name, arguments, body, range));
    }

    private void typeDeclaration(final int start, final List<ModifierItem> modifiers, final TypeKeyword keyword) {
        final TypeHeader header = TypeHeaders.INSTANCE.read(scan, start, modifiers, keyword);
        final TypeBody body = TypeHeaders.INSTANCE.body(scan, index -> SkippedCode.INSTANCE.startsPart(scan, index));
        if (!(body.end() instanceof BodyEnd.Missing)) {
            scan.unread.add(body.range());
        }
        HeaderChecks.INSTANCE.checkHeader(scan, header, Optional.of(own.keyword()));
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
            if (own.interfaceLike()) {
                scan.error("initializers not allowed in interfaces", scan.at(), scan.at() + 1);
            } else if (own.keyword() instanceof TypeKeyword.Record && !written.has("static")) {
                scan.error("instance initializers not allowed in records", scan.at(), scan.at() + 1);
            }
        }
        scan.docs.clear(scan.at());
        final TokenRange body = SkippedCode.INSTANCE.block(scan);
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
        final int from = doc.map(DocComment::token).orElse(start);
        final List<TypeParameter> typeParameters = scan.is("<") ? TypeHeaders.INSTANCE.typeParameters(scan) : List.of();
        if (scan.identifier(scan.at()) && scan.is(scan.next(scan.at()), "(")) {
            found(from, MemberHeaders.INSTANCE.constructor(scan, own, start, doc, modifiers, typeParameters));
            return;
        }
        if (scan.identifier(scan.at()) && scan.is(scan.next(scan.at()), "{") && typeParameters.isEmpty()
                && (own.keyword() instanceof TypeKeyword.Record || scan.text(scan.at()).equals(own.name()))) {
            found(from, MemberHeaders.INSTANCE.compactConstructor(scan, own, start, doc, modifiers));
            return;
        }
        final TypeText result = WrittenTypes.INSTANCE.type(scan);
        if (result.range().isEmpty()) {
            SkippedCode.INSTANCE.recover(scan);
            found(start, BodyPart.Unrecognised::new);
            return;
        }
        final Name name = scan.simpleName();
        if (scan.is("(")) {
            found(from, MemberHeaders.INSTANCE.method(scan, own, start, doc, modifiers, typeParameters, result, name));
        } else {
            if (!typeParameters.isEmpty()) {
                scan.error("'(' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            }
            found(from, MemberHeaders.INSTANCE.field(scan, own, start, doc, modifiers, result, name));
        }
    }

    private void unrecognised(final int start) {
        scan.error("illegal start of type", scan.at(), Math.min(scan.at() + 1, scan.size()));
        if (scan.at() == start) {
            scan.advance();
        }
        SkippedCode.INSTANCE.recover(scan);
        found(start, BodyPart.Unrecognised::new);
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
