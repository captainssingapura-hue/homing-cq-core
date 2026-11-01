package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.lexer.Token;
import com.homing_labs.cq.lang.java.lexer.TokenKind;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import com.homing_labs.cq.lang.java.model.AppliedAnnotation;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.CodeBlocks;
import com.homing_labs.cq.lang.java.model.ConstructorSignature;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Enclosure;
import com.homing_labs.cq.lang.java.model.Entries;
import com.homing_labs.cq.lang.java.model.Entry;
import com.homing_labs.cq.lang.java.model.EnumConstantValue;
import com.homing_labs.cq.lang.java.model.Extensibility;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.InitialiserValue;
import com.homing_labs.cq.lang.java.model.MethodSignature;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.Origin;
import com.homing_labs.cq.lang.java.model.Parameters;
import com.homing_labs.cq.lang.java.model.PrimitiveType;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.Visibility;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.model.WrittenParameter;
import com.homing_labs.cq.lang.java.model.WrittenParameters;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.BodyParts;
import com.homing_labs.cq.lang.java.outline.ClauseWord;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import com.homing_labs.cq.lang.java.outline.Declarator;
import com.homing_labs.cq.lang.java.outline.DocComment;
import com.homing_labs.cq.lang.java.outline.JavaOutliner;
import com.homing_labs.cq.lang.java.outline.ModifierItem;
import com.homing_labs.cq.lang.java.outline.NestedBody;
import com.homing_labs.cq.lang.java.outline.Outline;
import com.homing_labs.cq.lang.java.outline.Parameter;
import com.homing_labs.cq.lang.java.outline.TokenRange;
import com.homing_labs.cq.lang.java.outline.TypeHeader;
import com.homing_labs.cq.lang.java.outline.TypeKeyword;
import com.homing_labs.cq.lang.java.outline.TypeText;
import com.homing_labs.cq.lang.java.taxonomy.MemberClass;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The source of each value a unit's attributes hold: where the unit's source file writes it, as blocks of code copied
 * out of the file, and what the file says of it there, by the kind of value it is. The class files say what a class
 * has, and the source's outline where each part of it is written; the two are matched by name, and a method or a
 * constructor by the types of its parameters too, erased as the class file has them and matched by the names they are
 * written with.
 *
 * <p>The outline reads a top-level type down to the parts of its body, and its member types' bodies so too, one level
 * down; a type nested deeper is read down to its header, and a method's body is left as tokens. What is written in
 * those is written within them, not exactly. What the compiler implies or generates is written nowhere. javac puts
 * some objects together from several parts of the source: the static initialiser from every static block and every
 * static field's initial value that is no constant, and each constructor that hands over to no other runs the instance
 * initialisers and the instance fields' initial values too.</p>
 */
public record SourceLocator() implements StatelessFunctionalObject {

    public static final SourceLocator INSTANCE = new SourceLocator();

    /** How the name of javac's method for a lambda starts. */
    private static final String LAMBDA = "lambda$";

    /** The unit's source, outlined; nothing for a unit read without one. */
    public Optional<Outline> outline(final JavaUnit unit) {
        return unit.source().map(source -> JavaOutliner.INSTANCE.outline(
                JavaLexer.INSTANCE.scan(new SourceText(fileName(source), source.text()))));
    }

    /**
     * The unit's attributes, each value with its source processed - those of the classes nested in it too. A unit read
     * without its source keeps its attributes as they are, every value's source unavailable.
     */
    public Attributes sourced(final JavaUnit unit) {
        return outline(unit)
                .map(outline -> sourced(reading(unit, outline), unit.identity().name(), unit.attributes()))
                .orElse(unit.attributes());
    }

    /** Where the unit's top-level class is declared: from its documentation comment to its closing brace. */
    public Optional<CodeBlock> declaration(final JavaUnit unit) {
        return outline(unit).flatMap(outline -> found(reading(unit, outline), unit.identity().name())
                instanceof Declared declared ? Optional.of(block(declared.lexed(), declared.declaration()))
                : Optional.<CodeBlock>empty());
    }

    /** The attributes of a class of the unit, each value with its source. */
    private Attributes sourced(final Reading reading, final ClassName inClass, final Attributes attributes) {
        final Found where = found(reading, inClass);
        final Map<ObjectKind, List<WithSource<? extends Scalar>>> sourced = new LinkedHashMap<>();
        for (final Entry entry : attributes.entries()) {
            sourced.put(entry.object(), entry.all().stream()
                    .<WithSource<? extends Scalar>>map(held -> sourced(reading, inClass, where, entry.object(),
                            held.object()))
                    .toList());
        }
        return Entries.INSTANCE.of(attributes.kind(), sourced);
    }

    /** A value of a class of the unit, as one of the taxonomy's objects, with its source. */
    private WithSource<? extends Scalar> sourced(final Reading reading, final ClassName inClass, final Found where,
                                                 final ObjectKind object, final Scalar value) {
        return switch (value) {
            case NestedClass nested -> new WithSource<>(
                    new NestedClass(nested.name(), sourced(reading, nested.name(), nested.attributes())),
                    nestedClass(inClass, where, nested));
            case EnclosedClass enclosed -> new WithSource<>(new EnclosedClass(enclosed.name(), enclosed.enclosure(),
                    sourced(reading, enclosed.name(), enclosed.attributes())),
                    inBody(where, (declared, body) -> new ProcessedSource.Within<>(
                            enclosing(reading, inClass, declared, body, enclosed.enclosure()))));
            case FieldValue field -> new WithSource<>(field, inBody(where, (declared, body) -> field(body, field)));
            case EnumConstantValue constant ->
                    new WithSource<>(constant, inBody(where, (declared, body) -> enumConstant(body, constant)));
            case MethodValue method ->
                    new WithSource<>(method, inBody(where, (declared, body) -> method(body, method)));
            case ConstructorValue constructor -> new WithSource<>(constructor,
                    inBody(where, (declared, body) -> constructor(declared, body, constructor)));
            case InitialiserValue initialiser -> new WithSource<>(initialiser,
                    inBody(where, (declared, body) -> initialiser(reading, inClass, declared, body, initialiser)));
            case Visibility visibility -> new WithSource<>(visibility, inHeader(where, this::visibility));
            case Extensibility extensibility -> new WithSource<>(extensibility,
                    inHeader(where, declared -> extensibility(declared, extensibility)));
            case AppliedAnnotation annotation ->
                    new WithSource<>(annotation, inHeader(where, declared -> annotation(declared, annotation)));
            case TypeParameterValue parameter ->
                    new WithSource<>(parameter, inHeader(where, declared -> typeParameter(declared, parameter)));
            case RecordComponentValue component ->
                    new WithSource<>(component, inHeader(where, declared -> recordComponent(declared, component)));
            case TypeUse type -> new WithSource<>(type, inHeader(where, declared -> supertype(declared, object, type)));
            case ClassName dependency -> new WithSource<>(dependency, new ProcessedSource.Unplaced<>());
            default -> throw new IllegalArgumentException(value.getClass().getSimpleName() + " is held by no object");
        };
    }

    /**
     * The source of a member of a class: read from the class's body, or within the class when its body is not read, or
     * within what holds the class, or nowhere when the class is declared nowhere.
     */
    private <T extends Scalar> ProcessedSource<T> inBody(final Found where,
                                                         final BiFunction<Declared, Body, ProcessedSource<T>> read) {
        return switch (where) {
            case Declared declared -> declared.body().map(body -> read.apply(declared, body))
                    .orElseGet(() -> new ProcessedSource.Within<>(blocks(declared.lexed(),
                            List.of(declared.declaration()))));
            case Inside inside -> new ProcessedSource.Within<>(inside.blocks());
            case Missing _ -> new ProcessedSource.Unwritten<>();
        };
    }

    /** The source of what a class's header says: read from the header, or within what holds the class. */
    private <T extends Scalar> ProcessedSource<T> inHeader(final Found where,
                                                           final Function<Declared, ProcessedSource<T>> read) {
        return switch (where) {
            case Declared declared -> read.apply(declared);
            case Inside inside -> new ProcessedSource.Within<>(inside.blocks());
            case Missing _ -> new ProcessedSource.Unwritten<>();
        };
    }

    private ProcessedSource<FieldValue> field(final Body body, final FieldValue value) {
        if (generated(value.origin())) {
            return new ProcessedSource.Unwritten<>();
        }
        final Lexed lexed = body.lexed();
        for (final BodyPart.Field field : body.parts().of(BodyPart.Field.class)) {
            for (final Declarator declarator : field.declarators().items()) {
                if (declarator.name().text().equals(value.name())) {
                    return new ProcessedSource.OfField(block(lexed, trimmed(lexed, field.range(), false)),
                            doc(lexed, field.doc()), block(lexed, declarator.range()),
                            declarator.initializer().map(range -> block(lexed, range)));
                }
            }
        }
        return new ProcessedSource.Unwritten<>();
    }

    private ProcessedSource<EnumConstantValue> enumConstant(final Body body, final EnumConstantValue value) {
        final Lexed lexed = body.lexed();
        return body.parts().of(BodyPart.EnumConstant.class).stream()
                .filter(part -> part.name().text().equals(value.name()))
                .findFirst()
                .<ProcessedSource<EnumConstantValue>>map(part -> new ProcessedSource.OfEnumConstant(
                        block(lexed, trimmed(lexed, part.range(), true)), doc(lexed, part.doc()),
                        part.arguments().map(range -> block(lexed, range)),
                        part.body().map(range -> block(lexed, range))))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** A method, by its name and its parameters' types; of overloads not told apart so, the first written. */
    private ProcessedSource<MethodValue> method(final Body body, final MethodValue value) {
        if (generated(value.origin())) {
            return new ProcessedSource.Unwritten<>();
        }
        final Lexed lexed = body.lexed();
        final List<Candidate> found = matching(methods(lexed, body.parts(), value.name()),
                qualified(value.signature().map(MethodSignature::parameters).orElse(value.parameters())));
        if (found.isEmpty() || !(found.getFirst().part() instanceof BodyPart.Method method)) {
            return new ProcessedSource.Unwritten<>();
        }
        return new ProcessedSource.OfMethod(block(lexed, found.getFirst().range()), doc(lexed, method.doc()),
                parameters(lexed, method.parameters().formal()), method.body().map(range -> block(lexed, range)),
                method.defaultValue().map(range -> block(lexed, range)));
    }

    /** A constructor, and, unless it hands over to another, the instance initialisers it runs. */
    private ProcessedSource<ConstructorValue> constructor(final Declared declared, final Body body,
                                                          final ConstructorValue value) {
        if (generated(value.origin())) {
            return new ProcessedSource.Unwritten<>();
        }
        final Lexed lexed = body.lexed();
        final List<Candidate> found = matching(constructors(declared, body),
                value.signature().map(ConstructorSignature::parameters).map(this::qualified)
                        .orElseGet(() -> writtenParameters(declared, value.parameters())));
        if (found.isEmpty()) {
            return new ProcessedSource.Unwritten<>();
        }
        final Candidate candidate = found.getFirst();
        final CodeBlocks initialisers = delegates(lexed, candidate) ? new CodeBlocks(List.of())
                : blocks(lexed, instanceInitialisers(lexed, declared.header(), body.parts()));
        return switch (candidate.part()) {
            case BodyPart.Constructor constructor -> new ProcessedSource.OfConstructor(
                    block(lexed, candidate.range()), doc(lexed, constructor.doc()),
                    parameters(lexed, constructor.parameters().formal()),
                    constructor.body().map(range -> block(lexed, range)), initialisers);
            case BodyPart.CompactConstructor compact -> new ProcessedSource.OfConstructor(
                    block(lexed, candidate.range()), doc(lexed, compact.doc()), componentParameters(declared),
                    Optional.of(block(lexed, compact.body())), initialisers);
            default -> new ProcessedSource.Unwritten<>();
        };
    }

    private ProcessedSource<InitialiserValue> initialiser(final Reading reading, final ClassName inClass,
                                                          final Declared declared, final Body body,
                                                          final InitialiserValue value) {
        if (generated(value.origin())) {
            return new ProcessedSource.Unwritten<>();
        }
        final List<TokenRange> found = staticInitialisers(body.lexed(), declared.header(), body.parts(),
                constants(reading.unit, inClass));
        return found.isEmpty() ? new ProcessedSource.Unwritten<>()
                : new ProcessedSource.OfInitialiser(blocks(body.lexed(), found));
    }

    /** A member class, as the body of the class it is a member of declares it. */
    private ProcessedSource<NestedClass> nestedClass(final ClassName inClass, final Found where,
                                                     final NestedClass nested) {
        return inBody(where, (declared, body) -> memberType(body.parts(), inClass, nested.name())
                .<ProcessedSource<NestedClass>>map(type -> {
                    final Lexed lexed = body.lexed();
                    return new ProcessedSource.OfNestedClass(block(lexed, trimmed(lexed, type.range(), false)),
                            doc(lexed, type.header().doc()), block(lexed, type.header().range()),
                            type.body().isEmpty() ? Optional.empty() : Optional.of(block(lexed, type.body())));
                })
                .orElseGet(ProcessedSource.Unwritten::new));
    }

    private ProcessedSource<Visibility> visibility(final Declared declared) {
        return words(declared.header(), "public", "protected", "private").stream()
                .findFirst()
                .<ProcessedSource<Visibility>>map(word -> new ProcessedSource.OfVisibility(
                        block(declared.lexed(), word)))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** What a header writes of how its class can be extended: final, sealed and what it permits, or non-sealed. */
    private ProcessedSource<Extensibility> extensibility(final Declared declared, final Extensibility value) {
        final TypeHeader header = declared.header();
        final String word = switch (value) {
            case Extensibility.Final _ -> "final";
            case Extensibility.Open _ -> "non-sealed";
            case Extensibility.Sealed _ -> "sealed";
        };
        final Optional<TokenRange> permits = value instanceof Extensibility.Sealed
                ? header.clauses().items().stream()
                .filter(clause -> clause.word().equals(ClauseWord.Permits.INSTANCE))
                .map(clause -> clause.range())
                .findFirst()
                : Optional.empty();
        return words(header, word).stream()
                .findFirst()
                .<ProcessedSource<Extensibility>>map(written -> new ProcessedSource.OfExtensibility(
                        block(declared.lexed(), written), permits.map(range -> block(declared.lexed(), range))))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** An annotation of the class, by the names its type is written with. */
    private ProcessedSource<AppliedAnnotation> annotation(final Declared declared, final AppliedAnnotation value) {
        return declared.header().modifiers().annotations().stream()
                .filter(annotation -> fits(annotation.name().text(), qualified(value.annotation().type())))
                .findFirst()
                .<ProcessedSource<AppliedAnnotation>>map(annotation -> new ProcessedSource.OfAnnotation(
                        block(declared.lexed(), annotation.range()),
                        annotation.arguments().map(range -> block(declared.lexed(), range))))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    private ProcessedSource<TypeParameterValue> typeParameter(final Declared declared,
                                                              final TypeParameterValue value) {
        return declared.header().typeParameters().items().stream()
                .filter(parameter -> parameter.name().text().equals(value.name()))
                .findFirst()
                .<ProcessedSource<TypeParameterValue>>map(parameter -> new ProcessedSource.OfTypeParameter(
                        block(declared.lexed(), parameter.range()), blocks(declared.lexed(),
                        parameter.bounds().items().stream().map(TypeText::range).toList())))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    private ProcessedSource<RecordComponentValue> recordComponent(final Declared declared,
                                                                  final RecordComponentValue value) {
        return declared.header().components().stream()
                .flatMap(components -> components.items().stream())
                .filter(component -> component.name().text().equals(value.name()))
                .findFirst()
                .<ProcessedSource<RecordComponentValue>>map(component -> new ProcessedSource.OfRecordComponent(
                        block(declared.lexed(), component.range())))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** The superclass or a superinterface, as the header writes it; one it does not write is implied. */
    private ProcessedSource<TypeUse> supertype(final Declared declared, final ObjectKind object, final TypeUse value) {
        final TypeHeader header = declared.header();
        return (object instanceof Superclass ? header.superclass().stream() : header.superinterfaces().stream())
                .filter(type -> fits(erased(declared.lexed(), type.range(), 0), qualified(value)))
                .findFirst()
                .<ProcessedSource<TypeUse>>map(type -> new ProcessedSource.OfSupertype(
                        block(declared.lexed(), type.range())))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** Where a class of the unit is declared in the source, as far as the outline read it. */
    private Found found(final Reading reading, final ClassName name) {
        final Found known = reading.found.get(name);
        if (known != null) {
            return known;
        }
        final ClassName top = reading.unit.identity().name();
        final Found found = name.equals(top)
                ? reading.outline.types().stream()
                .filter(type -> type.header().name().text().equals(top.simpleName()))
                .findFirst()
                .<Found>map(type -> new Declared(reading.file, type.header(),
                        trimmed(reading.file, type.range(), false),
                        Optional.of(new Body(reading.file, type.parts())), false))
                .orElseGet(Missing::new)
                : nesting(top, reading.unit.attributes(), name)
                .map(nesting -> nested(reading, nesting, found(reading, nesting.in())))
                .orElseGet(Missing::new);
        reading.found.put(name, found);
        return found;
    }

    /** Where a class nested in another is declared, from where the other is. */
    private Found nested(final Reading reading, final Nesting nesting, final Found in) {
        return switch (in) {
            case Declared declared when declared.body().isPresent() -> {
                final Body body = declared.body().get();
                yield switch (nesting.value()) {
                    case NestedClass nested -> {
                        final Optional<BodyPart.TypeDeclaration> type = memberType(body.parts(), nesting.in(),
                                nested.name());
                        yield type.isEmpty() ? new Missing() : new Declared(body.lexed(), type.get().header(),
                                trimmed(body.lexed(), type.get().range(), false),
                                type.get().nested().map(read -> new Body(lexed(reading, read), read.parts())),
                                nesting.object() instanceof MemberClass);
                    }
                    case EnclosedClass enclosed ->
                            new Inside(enclosing(reading, nesting.in(), declared, body, enclosed.enclosure()));
                    default -> new Missing();
                };
            }
            case Declared declared -> new Inside(blocks(declared.lexed(), List.of(declared.declaration())));
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

    /** A member type of a class, as the class's body declares it. */
    private Optional<BodyPart.TypeDeclaration> memberType(final BodyParts parts, final ClassName inClass,
                                                          final ClassName name) {
        final String simpleName = name.binaryName().substring(inClass.binaryName().length() + 1);
        return parts.of(BodyPart.TypeDeclaration.class).stream()
                .filter(type -> type.header().name().text().equals(simpleName))
                .findFirst();
    }

    /**
     * Where a local or an anonymous class is declared: in the method, the constructor or the initialisers whose code
     * declares it, or, when none of those is found, anywhere in the class.
     */
    private CodeBlocks enclosing(final Reading reading, final ClassName inClass, final Declared declared,
                                 final Body body, final Enclosure enclosure) {
        final Lexed lexed = body.lexed();
        final List<TokenRange> found = switch (enclosure) {
            case Enclosure.InMethod method when method.name().startsWith(LAMBDA) ->
                    inLambda(reading, inClass, declared, body, method.name());
            case Enclosure.InMethod method ->
                    ranges(resembling(methods(lexed, body.parts(), method.name()), qualified(method.parameters())));
            case Enclosure.InConstructor constructor -> ranges(resembling(constructors(declared, body),
                    writtenParameters(declared, constructor.parameters())));
            case Enclosure.InInitialiser _ -> initialisers(lexed, body.parts());
        };
        return found.isEmpty() ? blocks(declared.lexed(), List.of(declared.declaration())) : blocks(lexed, found);
    }

    /**
     * Where javac's method for a lambda was written: {@code lambda$name$0} is in the method of that name,
     * {@code lambda$new$0} in a constructor or an instance initialiser, {@code lambda$static$0} in a static one.
     */
    private List<TokenRange> inLambda(final Reading reading, final ClassName inClass, final Declared declared,
                                      final Body body, final String name) {
        final Lexed lexed = body.lexed();
        final TypeHeader header = declared.header();
        final String in = name.substring(LAMBDA.length(), Math.max(LAMBDA.length(), name.lastIndexOf('$')));
        return switch (in) {
            case "new" -> Stream.concat(ranges(constructors(declared, body)).stream(),
                    instanceInitialisers(lexed, header, body.parts()).stream()).toList();
            case "static" -> staticInitialisers(lexed, header, body.parts(), constants(reading.unit, inClass));
            default -> ranges(methods(lexed, body.parts(), in));
        };
    }

    /** The methods of a body with the name, each with its parameters' types as written. */
    private List<Candidate> methods(final Lexed lexed, final BodyParts parts, final String name) {
        return parts.of(BodyPart.Method.class).stream()
                .filter(method -> method.name().text().equals(name))
                .map(method -> new Candidate(trimmed(lexed, method.range(), false),
                        writtenTypes(lexed, method.parameters().formal()), method.body(), method))
                .toList();
    }

    /** The constructors of a body - a record's compact one taking its components - each with its parameters' types. */
    private List<Candidate> constructors(final Declared declared, final Body body) {
        final Lexed lexed = body.lexed();
        final List<Candidate> found = new ArrayList<>();
        for (final BodyPart part : body.parts().items()) {
            switch (part) {
                case BodyPart.Constructor constructor -> found.add(new Candidate(
                        trimmed(lexed, constructor.range(), false),
                        writtenTypes(lexed, constructor.parameters().formal()), constructor.body(), constructor));
                case BodyPart.CompactConstructor compact -> found.add(new Candidate(
                        trimmed(lexed, compact.range(), false), componentTypes(declared), Optional.empty(), compact));
                default -> {
                }
            }
        }
        return found;
    }

    /**
     * The types of a record's components as written, in the tokens its header was read from: a member record's are the
     * body's that declares it, not its own body's.
     */
    private List<String> componentTypes(final Declared declared) {
        return declared.header().components().stream()
                .flatMap(components -> components.items().stream())
                .map(component -> erased(declared.lexed(), component.type().range(), component.variable() ? 1 : 0))
                .toList();
    }

    /** A record's components, as the parameters its compact constructor takes. */
    private WrittenParameters componentParameters(final Declared declared) {
        return new WrittenParameters(declared.header().components().stream()
                .flatMap(components -> components.items().stream())
                .map(component -> new WrittenParameter(component.name().text(),
                        block(declared.lexed(), component.range())))
                .toList());
    }

    /** Formal parameters as written: each by its name, with its code. */
    private WrittenParameters parameters(final Lexed lexed, final List<Parameter.Formal> parameters) {
        return new WrittenParameters(parameters.stream()
                .map(parameter -> new WrittenParameter(parameter.name().text(), block(lexed, parameter.range())))
                .toList());
    }

    /**
     * The candidates whose parameters are of the types the class file has, and of those the ones that name them most
     * fully: {@code PlatformLogger.Level} is the one of two overloads that takes it, and {@code Level}, written in the
     * other, can then only be another {@code Level}. When none fits so, those whose types have the class file's simple
     * names: a member type a class inherits can be written by that class's name,
     * {@code JCMemberReference.ReferenceMode} for {@code MemberReferenceTree.ReferenceMode}. A method's or a
     * constructor's own types are matched so: its signature keeps a type variable as it is named, where its descriptor
     * erases it.
     */
    private List<Candidate> matching(final List<Candidate> candidates, final List<String> parameters) {
        final List<Candidate> qualified = candidates.stream()
                .filter(candidate -> fit(candidate.parameters(), parameters, this::fits))
                .toList();
        final List<Candidate> fitting = !qualified.isEmpty() ? qualified : candidates.stream()
                .filter(candidate -> fit(candidate.parameters(), parameters, this::sameSimpleName))
                .toList();
        final int fullest = fitting.stream().mapToInt(candidate -> names(candidate.parameters())).max().orElse(0);
        return fitting.stream().filter(candidate -> names(candidate.parameters()) == fullest).toList();
    }

    private boolean fit(final List<String> written, final List<String> compiled,
                        final BiPredicate<String, String> fits) {
        if (written.size() != compiled.size()) {
            return false;
        }
        for (int at = 0; at < written.size(); at++) {
            if (!fits.test(written.get(at), compiled.get(at))) {
                return false;
            }
        }
        return true;
    }

    /** Whether two types have the same simple name and dimensions: {@code Entry[]} for {@code a.B.Entry[]}. */
    private boolean sameSimpleName(final String written, final String compiled) {
        return simpleName(written).equals(simpleName(compiled));
    }

    private String simpleName(final String type) {
        final String name = type.replace("[]", "");
        return name.substring(name.lastIndexOf('.') + 1) + "[]".repeat((type.length() - name.length()) / 2);
    }

    /**
     * Whether a type as written can be the type the class file names: the same, or written by fewer names, from the
     * right - {@code Map.Entry[]} or {@code Entry[]} for {@code java.util.Map.Entry[]}.
     */
    private boolean fits(final String written, final String compiled) {
        return written.equals(compiled) || compiled.endsWith("." + written);
    }

    /** How many names the types are written with, all told. */
    private int names(final List<String> written) {
        return written.stream().mapToInt(type -> type.split("\\.").length).sum();
    }

    /**
     * The candidates whose parameters are of the erased types the class file has, or, with none, those that take as
     * many: the method that encloses a class is named by its descriptor alone, which erases a type variable to its
     * bound.
     */
    private List<Candidate> resembling(final List<Candidate> candidates, final List<String> erased) {
        final List<Candidate> typed = matching(candidates, erased);
        return !typed.isEmpty() ? typed : candidates.stream()
                .filter(candidate -> candidate.parameters().size() == erased.size())
                .toList();
    }

    private List<TokenRange> ranges(final List<Candidate> candidates) {
        return candidates.stream().map(Candidate::range).toList();
    }

    /** Whether a constructor hands over to another of its class with {@code this(...)}, which runs the initialisers. */
    private boolean delegates(final Lexed lexed, final Candidate candidate) {
        final TokenStream tokens = lexed.tokens();
        final TokenRange body = candidate.body().orElse(new TokenRange(0, 0));
        int depth = 0;
        for (int at = body.from(); at < body.to(); at++) {
            final Token token = tokens.get(at);
            if (comment(token)) {
                continue;
            }
            switch (tokens.text(token)) {
                case "{" -> depth++;
                case "}" -> depth--;
                case "this" -> {
                    if (depth == 1 && next(tokens, at, body.to()).equals("(")) {
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
    private List<TokenRange> instanceInitialisers(final Lexed lexed, final TypeHeader header, final BodyParts parts) {
        final List<TokenRange> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.Initializer initializer when !initializer.isStatic() ->
                        found.add(trimmed(lexed, initializer.range(), false));
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
     * What javac puts the static initialiser together from, in the order written: an enum's constants, the static
     * blocks, and the static fields' initial values, but for the constants'.
     */
    private List<TokenRange> staticInitialisers(final Lexed lexed, final TypeHeader header, final BodyParts parts,
                                                final Set<String> constants) {
        final List<TokenRange> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.EnumConstant constant -> found.add(trimmed(lexed, constant.range(), true));
                case BodyPart.Initializer initializer when initializer.isStatic() ->
                        found.add(trimmed(lexed, initializer.range(), false));
                case BodyPart.Field field when isStatic(header, field) -> field.declarators().items().stream()
                        .filter(declarator -> declarator.initializer().isPresent()
                                && !constants.contains(declarator.name().text()))
                        .forEach(declarator -> found.add(declarator.range()));
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
    private Set<String> constants(final JavaUnit unit, final ClassName inClass) {
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
    private List<TokenRange> initialisers(final Lexed lexed, final BodyParts parts) {
        final List<TokenRange> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.EnumConstant constant when constant.arguments().isPresent()
                        || constant.body().isPresent() -> found.add(trimmed(lexed, constant.range(), true));
                case BodyPart.Initializer initializer -> found.add(trimmed(lexed, initializer.range(), false));
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

    /** The modifier words of a header that are among the words. */
    private List<TokenRange> words(final TypeHeader header, final String... words) {
        final List<String> wanted = List.of(words);
        return header.modifiers().words().stream()
                .filter(word -> wanted.contains(word.word()))
                .map(ModifierItem.Word::range)
                .toList();
    }

    /**
     * The types of the parameters a constructor is written with: javac gives an enum's its name and ordinal first, and
     * an inner class's the object of its enclosing class.
     */
    private List<String> writtenParameters(final Declared declared, final Parameters parameters) {
        final List<String> all = qualified(parameters);
        final int added = declared.header().keyword() instanceof TypeKeyword.Enum ? 2 : declared.inner() ? 1 : 0;
        return all.size() >= added ? all.subList(added, all.size()) : all;
    }

    /** The types of formal parameters as written, each erased to its names and its dimensions. */
    private List<String> writtenTypes(final Lexed lexed, final List<Parameter.Formal> parameters) {
        return parameters.stream()
                .map(parameter -> erased(lexed, parameter.type().range(),
                        brackets(lexed, parameter.dimensions()) + (parameter.variable() ? 1 : 0)))
                .toList();
    }

    /**
     * A type as written, erased to the names it is written with and its dimensions: {@code java.util.Map.Entry[]} for
     * {@code java.util.Map.@A Entry<K, V>[]}. A type variable stays as it is named.
     */
    private String erased(final Lexed lexed, final TokenRange type, final int dimensions) {
        final TokenStream tokens = lexed.tokens();
        final StringBuilder name = new StringBuilder();
        int brackets = dimensions;
        int depth = 0;
        for (int at = type.from(); at < type.to(); at++) {
            final Token token = tokens.get(at);
            if (comment(token)) {
                continue;
            }
            final String text = tokens.text(token);
            if (text.equals("<")) {
                depth++;
            } else if (text.equals(">")) {
                depth--;
            } else if (depth > 0) {
                continue;
            } else if (text.equals("@")) {
                at = pastAnnotation(tokens, at, type.to()) - 1;
            } else if (text.equals("[")) {
                brackets++;
            } else if (text.equals(".") || token.kind() instanceof TokenKind.Identifier
                    || token.kind() instanceof TokenKind.Keyword) {
                name.append(text);
            }
        }
        return name + "[]".repeat(brackets);
    }

    /** The index past an annotation written in a type, from its {@code @}: past its name, and its arguments. */
    private int pastAnnotation(final TokenStream tokens, final int at, final int end) {
        int past = following(tokens, at, end);
        while (past < end && tokens.get(past).kind() instanceof TokenKind.Identifier) {
            past = following(tokens, past, end);
            if (past < end && tokens.text(tokens.get(past)).equals(".")) {
                past = following(tokens, past, end);
            } else {
                break;
            }
        }
        if (past < end && tokens.text(tokens.get(past)).equals("(")) {
            int depth = 0;
            for (; past < end; past++) {
                final String text = tokens.text(tokens.get(past));
                if (text.equals("(")) {
                    depth++;
                } else if (text.equals(")") && --depth == 0) {
                    return past + 1;
                }
            }
        }
        return past;
    }

    private int brackets(final Lexed lexed, final TokenRange range) {
        int brackets = 0;
        for (int at = range.from(); at < range.to(); at++) {
            if (lexed.tokens().text(lexed.tokens().get(at)).equals("[")) {
                brackets++;
            }
        }
        return brackets;
    }

    /** The index of the first token after one that is no comment, or the end. */
    private int following(final TokenStream tokens, final int at, final int end) {
        int next = at + 1;
        while (next < end && comment(tokens.get(next))) {
            next++;
        }
        return next;
    }

    /** The text of the first token after one that is no comment, or nothing at the end. */
    private String next(final TokenStream tokens, final int at, final int end) {
        final int next = following(tokens, at, end);
        return next < end ? tokens.text(tokens.get(next)) : "";
    }

    private List<String> qualified(final Parameters parameters) {
        return parameters.items().stream().map(this::qualified).toList();
    }

    /** A type of the class file by its qualified name and its dimensions: {@code java.util.Map.Entry[]}. */
    private String qualified(final TypeUse type) {
        return switch (type) {
            case TypeUse.Primitive primitive -> keyword(primitive.type());
            case TypeUse.Named named -> qualified(named.name());
            case TypeUse.Parameterized parameterized -> qualified(parameterized.name());
            case TypeUse.Variable variable -> variable.name();
            case TypeUse.Array array -> qualified(array.component()) + "[]";
        };
    }

    /** A class by its qualified name, as a source writes it: {@code java.util.Map.Entry} for {@code Map$Entry}. */
    private String qualified(final ClassName name) {
        return name.binaryName().replace('$', '.');
    }

    private String keyword(final PrimitiveType type) {
        return switch (type) {
            case PrimitiveType.Boolean _ -> "boolean";
            case PrimitiveType.Byte _ -> "byte";
            case PrimitiveType.Char _ -> "char";
            case PrimitiveType.Short _ -> "short";
            case PrimitiveType.Int _ -> "int";
            case PrimitiveType.Long _ -> "long";
            case PrimitiveType.Float _ -> "float";
            case PrimitiveType.Double _ -> "double";
        };
    }

    private boolean generated(final Origin origin) {
        return origin instanceof Origin.Generated;
    }

    private boolean comment(final Token token) {
        return token.kind() instanceof TokenKind.LineComment || token.kind() instanceof TokenKind.BlockComment
                || token.kind() instanceof TokenKind.DocComment;
    }

    /**
     * A part's tokens without the comments after it, which its range takes in up to the next part's start - and, for
     * an enum constant, without the comma or the semicolon after it.
     */
    private TokenRange trimmed(final Lexed lexed, final TokenRange range, final boolean separated) {
        int to = range.to();
        while (to > range.from()) {
            final Token last = lexed.tokens().get(to - 1);
            final String text = lexed.tokens().text(last);
            if (!(comment(last) || separated && (text.equals(",") || text.equals(";")))) {
                break;
            }
            to--;
        }
        return new TokenRange(range.from(), to);
    }

    /** The code the tokens of a range are written with, copied out of the file, and where it is there. */
    private CodeBlock block(final Lexed lexed, final TokenRange range) {
        final int start = lexed.tokens().get(range.from()).start();
        final int end = lexed.tokens().get(range.to() - 1).end();
        return new CodeBlock(lexed.tokens().source().text().substring(start, end), lexed.offset() + start,
                lexed.lineOf(start));
    }

    /** The code of the ranges, in the order written, each once. */
    private CodeBlocks blocks(final Lexed lexed, final List<TokenRange> ranges) {
        return new CodeBlocks(ranges.stream()
                .filter(range -> !range.isEmpty())
                .distinct()
                .sorted(Comparator.comparingInt(TokenRange::from))
                .map(range -> block(lexed, range))
                .toList());
    }

    private Optional<CodeBlock> doc(final Lexed lexed, final Optional<DocComment> doc) {
        return doc.map(comment -> block(lexed, new TokenRange(comment.token(), comment.token() + 1)));
    }

    private String fileName(final JavaSource source) {
        final String path = Optional.ofNullable(source.file().getPath()).orElse("");
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * One reading of a unit's source: the unit, its outline, the file's tokens, and, as they are come to, the tokens of
     * each member type's body and where each class of the unit is declared.
     */
    private static final class Reading implements Mutable {

        private final JavaUnit unit;

        private final Outline outline;

        private final Lexed file;

        private final Map<NestedBody, Lexed> bodies = new IdentityHashMap<>();

        private final Map<ClassName, Found> found = new HashMap<>();

        Reading(final JavaUnit unit, final Outline outline, final Lexed file) {
            this.unit = unit;
            this.outline = outline;
            this.file = file;
        }
    }

    /** A reading of the unit's outlined source. */
    private Reading reading(final JavaUnit unit, final Outline outline) {
        return new Reading(unit, outline, lexed(outline.tokens(), 0, 1));
    }

    /** The tokens a member type's body was read into, at its block's place in the file: lexed once a reading. */
    private Lexed lexed(final Reading reading, final NestedBody body) {
        return reading.bodies.computeIfAbsent(body, read -> lexed(read.tokens(), read.block().offset(),
                read.block().line()));
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

    /**
     * Tokens a class's parts were read from, and where their text starts in the file - its first character's offset and
     * line - with where each of the text's lines starts: the file's own tokens, or a member type's body's, lexed on its
     * own from a block copied out of the file.
     */
    private record Lexed(TokenStream tokens, int offset, int line, List<Integer> starts) {

        /** The line of the file a character of the text is on. */
        int lineOf(final int at) {
            int low = 0;
            int high = starts.size() - 1;
            while (low < high) {
                final int middle = (low + high + 1) / 2;
                if (starts.get(middle) <= at) {
                    low = middle;
                } else {
                    high = middle - 1;
                }
            }
            return line + low;
        }
    }

    /** A body's parts, in the tokens they were read from. */
    private record Body(Lexed lexed, BodyParts parts) {
    }

    /** Where a class of the unit is declared, as far as the outline read the source. */
    private sealed interface Found {
    }

    /**
     * Declared there, in the tokens the header was read from: with its header, and its body's parts when the outline
     * read those too; and whether it is an inner class, whose constructors javac gives its enclosing object first.
     */
    private record Declared(Lexed lexed, TypeHeader header, TokenRange declaration, Optional<Body> body,
                            boolean inner) implements Found {
    }

    /** Declared somewhere inside these blocks, in code the outline left unread. */
    private record Inside(CodeBlocks blocks) implements Found {
    }

    /** Declared nowhere in the source. */
    private record Missing() implements Found {
    }

    /** The class a class is nested in, the object it is there, and the value that holds it: nested or enclosed. */
    private record Nesting(ClassName in, ObjectKind object, Scalar value) {
    }

    /** A method or a constructor as written: where, its parameters' types erased to the names written, and its body. */
    private record Candidate(TokenRange range, List<String> parameters, Optional<TokenRange> body, BodyPart part) {
    }
}
