package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.lexer.TokenStream;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.CodeBlocks;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Enclosure;
import com.homing_labs.cq.lang.java.model.Entry;
import com.homing_labs.cq.lang.java.model.InitialisationStep;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.NestedBody;
import com.homing_labs.cq.lang.java.outline.Outline;
import com.homing_labs.cq.lang.java.outline.TokenRange;
import com.homing_labs.cq.lang.java.outline.TypeHeader;
import com.homing_labs.cq.lang.java.taxonomy.MemberClass;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * One reading of a unit's source: the unit, its outline, the file's tokens, and, as they are come to, the tokens of
 * each member type's body and where each class of the unit is declared - found as it is asked for.
 */
final class SourceReading implements Mutable {

    /** How the name of javac's method for a lambda starts. */
    private static final String LAMBDA = "lambda$";

    private final JavaUnit unit;

    private final Outline outline;

    private final Lexed file;

    private final Map<NestedBody, Lexed> bodies = new IdentityHashMap<>();

    /** Where each class of the unit is declared, as it has been found. */
    private final Map<ClassName, Found> located = new HashMap<>();

    /** A reading of the unit's outlined source. */
    SourceReading(final JavaUnit unit, final Outline outline) {
        this.unit = unit;
        this.outline = outline;
        this.file = lexed(outline.tokens(), 0, 1);
    }

    JavaUnit unit() {
        return unit;
    }

    /** Where a class of the unit is declared in the source, as far as the outline read it. */
    Found found(final ClassName name) {
        final Found known = located.get(name);
        if (known != null) {
            return known;
        }
        final ClassName top = unit.identity().name();
        final Found found = name.equals(top)
                ? outline.types().stream()
                .filter(type -> type.header().name().text().equals(top.simpleName()))
                .findFirst()
                .<Found>map(type -> new Declared(file, type.header(), file.trimmed(type.range(), false),
                        Optional.of(new ReadBody(file, type.parts())), false))
                .orElseGet(Missing::new)
                : nesting(top, unit.attributes(), name)
                .map(nesting -> nested(nesting, found(nesting.in())))
                .orElseGet(Missing::new);
        located.put(name, found);
        return found;
    }

    /**
     * Where a local or an anonymous class is declared: in the method or the constructor whose code declares it - the
     * one its class file names by its descriptor, written where that method's own source is - or in the initialisers,
     * or, when none of those is found, anywhere in the class.
     */
    CodeBlocks enclosing(final ClassName inClass, final Declared declared, final ReadBody body,
                         final Enclosure enclosure) {
        final Lexed lexed = body.lexed();
        final CodeBlocks found = switch (enclosure) {
            case Enclosure.InMethod method when method.name().startsWith(LAMBDA) ->
                    inLambda(inClass, declared, body, method.name());
            case Enclosure.InMethod method -> lexed.blocks(values(inClass, MethodValue.class)
                    .filter(value -> value.name().equals(method.name())
                            && value.parameters().equals(method.parameters()))
                    .flatMap(value -> Overloads.INSTANCE.written(body, value).stream())
                    .map(Candidate::range)
                    .limit(1)
                    .toList());
            case Enclosure.InConstructor constructor -> lexed.blocks(values(inClass, ConstructorValue.class)
                    .filter(value -> value.parameters().equals(constructor.parameters()))
                    .flatMap(value -> Overloads.INSTANCE.written(declared, body, value).stream())
                    .map(Candidate::range)
                    .limit(1)
                    .toList());
            case Enclosure.InInitialiser _ -> lexed.blocks(Initialisers.INSTANCE.initialisers(lexed, body.parts()));
        };
        return found.items().isEmpty() ? declared.lexed().blocks(List.of(declared.declaration())) : found;
    }

    /** Where a class nested in another is declared, from where the other is. */
    private Found nested(final Nesting nesting, final Found in) {
        return switch (in) {
            case Declared declared when declared.body().isPresent() -> {
                final ReadBody body = declared.body().get();
                yield switch (nesting.value()) {
                    case NestedClass nested -> {
                        final Optional<BodyPart.TypeDeclaration> type = body.memberType(nesting.in(), nested.name());
                        yield type.isEmpty() ? new Missing() : new Declared(body.lexed(), type.get().header(),
                                body.lexed().trimmed(type.get().range(), false),
                                type.get().nested().map(read -> new ReadBody(lexed(read), read.parts())),
                                nesting.object() instanceof MemberClass);
                    }
                    case EnclosedClass enclosed ->
                            new Inside(enclosing(nesting.in(), declared, body, enclosed.enclosure()));
                    default -> new Missing();
                };
            }
            case Declared declared -> new Inside(declared.lexed().blocks(List.of(declared.declaration())));
            case Inside inside -> inside;
            case Missing missing -> missing;
        };
    }

    /** The class of the unit a class is nested in, the object it is there, and the value that holds it. */
    private Optional<Nesting> nesting(final ClassName in, final Attributes attributes, final ClassName name) {
        for (final Entry entry : attributes.entries()) {
            for (final WithSource<? extends Scalar> held : entry.all()) {
                final Optional<Nesting> found = switch (held.object()) {
                    case NestedClass nested -> nested.name().equals(name)
                            ? Optional.of(new Nesting(in, entry.object(), nested))
                            : nesting(nested.name(), nested.attributes(), name);
                    case EnclosedClass enclosed -> enclosed.name().equals(name)
                            ? Optional.of(new Nesting(in, entry.object(), enclosed))
                            : nesting(enclosed.name(), enclosed.attributes(), name);
                    default -> Optional.empty();
                };
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    /** The values of a kind that a class of the unit holds, as its class file has them. */
    private <T extends Scalar> Stream<T> values(final ClassName inClass, final Class<T> kind) {
        final ClassName top = unit.identity().name();
        final Optional<Attributes> attributes = inClass.equals(top) ? Optional.of(unit.attributes())
                : nesting(top, unit.attributes(), inClass).flatMap(nesting -> switch (nesting.value()) {
                    case NestedClass nested -> Optional.of(nested.attributes());
                    case EnclosedClass enclosed -> Optional.of(enclosed.attributes());
                    default -> Optional.<Attributes>empty();
                });
        return attributes.stream()
                .flatMap(held -> held.entries().stream())
                .flatMap(entry -> entry.all().stream())
                .map(WithSource::object)
                .filter(kind::isInstance)
                .map(kind::cast);
    }

    /**
     * Where javac's method for a lambda was written: {@code lambda$name$0} is in the method of that name,
     * {@code lambda$new$0} in a constructor or an instance initialiser, {@code lambda$static$0} in a static one.
     */
    private CodeBlocks inLambda(final ClassName inClass, final Declared declared, final ReadBody body,
                                final String name) {
        final Lexed lexed = body.lexed();
        final TypeHeader header = declared.header();
        final String in = name.substring(LAMBDA.length(), Math.max(LAMBDA.length(), name.lastIndexOf('$')));
        return switch (in) {
            case "new" -> lexed.blocks(Stream.concat(
                    Overloads.INSTANCE.ranges(Overloads.INSTANCE.constructors(declared, body)).stream(),
                    Initialisers.INSTANCE.instanceInitialisers(lexed, header, body.parts()).stream()).toList());
            case "static" -> new CodeBlocks(Initialisers.INSTANCE.staticInitialisers(lexed, header, body.parts(),
                    Initialisers.INSTANCE.constants(unit, inClass)).stream().map(InitialisationStep::block).toList());
            default -> lexed.blocks(Overloads.INSTANCE.ranges(Overloads.INSTANCE.methods(lexed, body.parts(), in)));
        };
    }

    /** The tokens a member type's body was read into, at its block's place in the file: lexed once a reading. */
    private Lexed lexed(final NestedBody body) {
        return bodies.computeIfAbsent(body, read -> lexed(read.tokens(), read.block().offset(), read.block().line()));
    }

    /** Tokens read from text that starts at the offset and the line in the file, with where each line of it starts. */
    private Lexed lexed(final TokenStream tokens, final int offset, final int line) {
        final String text = tokens.source().text();
        final List<Integer> starts = new ArrayList<>(List.of(0));
        for (int at = 0; at < text.length(); at++) {
            final char c = text.charAt(at);
            if (c == '\n' || c == '\r' && (at + 1 == text.length() || text.charAt(at + 1) != '\n')) {
                starts.add(at + 1);
            }
        }
        return new Lexed(tokens, offset, line, List.copyOf(starts));
    }

    /** Where a class of the unit is declared, as far as the outline read the source. */
    sealed interface Found {
    }

    /**
     * Declared there, in the tokens the header was read from: with its header, and its body's parts when the outline
     * read those too; and whether it is an inner class, whose constructors javac gives its enclosing object first.
     */
    record Declared(Lexed lexed, TypeHeader header, TokenRange declaration, Optional<ReadBody> body, boolean inner)
            implements Found {
    }

    /** Declared somewhere inside these blocks, in code the outline left unread. */
    record Inside(CodeBlocks blocks) implements Found {
    }

    /** Declared nowhere in the source. */
    record Missing() implements Found {
    }

    /** The class a class is nested in, the object it is there, and the value that holds it: nested or enclosed. */
    private record Nesting(ClassName in, ObjectKind object, Scalar value) {
    }
}
