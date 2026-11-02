package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.lexer.Token;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.InitialisationStep;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.BodyParts;
import com.homing_labs.cq.lang.java.outline.TokenRange;
import com.homing_labs.cq.lang.java.outline.TypeHeader;
import com.homing_labs.cq.lang.java.outline.TypeKeyword;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What javac puts together from several parts of a body: how its class is initialised, in steps, from its enum
 * constants, its static blocks and its static fields' initial values that are no constants; and the instance
 * initialisers and the instance fields' initial values each constructor runs that hands over to no other.
 */
record Initialisers() implements StatelessFunctionalObject {

    static final Initialisers INSTANCE = new Initialisers();

    /** Whether a constructor hands over to another of its class with {@code this(...)}, which runs the initialisers. */
    boolean delegates(final Lexed lexed, final Candidate candidate) {
        final TokenStream tokens = lexed.tokens();
        final TokenRange body = candidate.body().orElse(new TokenRange(0, 0));
        int depth = 0;
        for (int at = body.from(); at < body.to(); at++) {
            final Token token = tokens.get(at);
            if (token.kind().comment()) {
                continue;
            }
            switch (tokens.text(token)) {
                case "{" -> depth++;
                case "}" -> depth--;
                case "this" -> {
                    if (depth == 1 && lexed.next(at, body.to()).equals("(")) {
                        return true;
                    }
                }
                default -> {
                }
            }
        }
        return false;
    }

    /** The instance initialisers, and the instance fields' initial values, in the order written. */
    List<TokenRange> instanceInitialisers(final Lexed lexed, final TypeHeader header, final BodyParts parts) {
        final List<TokenRange> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.Initializer initializer when !initializer.isStatic() ->
                        found.add(lexed.trimmed(initializer.range(), false));
                case BodyPart.Field field when !isStatic(header, field) -> field.declarators().items().stream()
                        .filter(declarator -> declarator.initializer().isPresent())
                        .forEach(declarator -> found.add(declarator.range()));
                default -> {
                }
            }
        }
        return found;
    }

    /**
     * The steps javac puts the static initialiser together from, in the order written: an enum's constants, the static
     * blocks, and the static fields' initial values, but for the constants'.
     */
    List<InitialisationStep> staticInitialisers(final Lexed lexed, final TypeHeader header, final BodyParts parts,
                                                final Set<String> constants) {
        final List<InitialisationStep> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.EnumConstant constant -> found.add(new InitialisationStep.EnumConstant(
                        constant.name().text(), lexed.block(lexed.trimmed(constant.range(), true))));
                case BodyPart.Initializer initializer when initializer.isStatic() -> found.add(
                        new InitialisationStep.StaticBlock(lexed.block(lexed.trimmed(initializer.range(), false))));
                case BodyPart.Field field when isStatic(header, field) -> field.declarators().items().stream()
                        .filter(declarator -> declarator.initializer().isPresent()
                                && !constants.contains(declarator.name().text()))
                        .forEach(declarator -> found.add(new InitialisationStep.InitialValue(
                                declarator.name().text(), lexed.block(declarator.range()),
                                lexed.block(declarator.initializer().orElseThrow()))));
                default -> {
                }
            }
        }
        return found;
    }

    /**
     * The fields of a class of the unit that are constants (JLS 4.12.4): the class file holds their values, and no
     * initialiser computes them. Only the class file can say which they are - the names in a value's expression may be
     * any field's - so it is read again for them, through the loader of the unit's class.
     */
    Set<String> constants(final JavaUnit unit, final ClassName inClass) {
        final ClassLoader loader = Optional.ofNullable(unit.type().getClassLoader())
                .orElseGet(ClassLoader::getSystemClassLoader);
        final String classFile = inClass.binaryName().replace('.', '/') + ".class";
        try (InputStream in = loader.getResourceAsStream(classFile)) {
            if (in == null) {
                return Set.of();
            }
            return ClassFile.of().parse(in.readAllBytes()).fields().stream()
                    .filter(field -> field.findAttribute(java.lang.classfile.Attributes.constantValue()).isPresent())
                    .map(field -> field.fieldName().stringValue())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read " + classFile, e);
        }
    }

    /** Every initialiser, every field's initial value, and every enum constant made with arguments or a body. */
    List<TokenRange> initialisers(final Lexed lexed, final BodyParts parts) {
        final List<TokenRange> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.EnumConstant constant when constant.arguments().isPresent()
                        || constant.body().isPresent() -> found.add(lexed.trimmed(constant.range(), true));
                case BodyPart.Initializer initializer -> found.add(lexed.trimmed(initializer.range(), false));
                case BodyPart.Field field -> field.declarators().items().stream()
                        .filter(declarator -> declarator.initializer().isPresent())
                        .forEach(declarator -> found.add(declarator.range()));
                default -> {
                }
            }
        }
        return found;
    }

    private boolean isStatic(final TypeHeader header, final BodyPart.Field field) {
        return field.modifiers().has("static") || interfaceLike(header);
    }

    /** Whether the type is an interface or an annotation interface, whose fields are all static and final. */
    private boolean interfaceLike(final TypeHeader header) {
        return header.keyword() instanceof TypeKeyword.Interface || header.keyword() instanceof TypeKeyword.Annotation;
    }
}
