package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.def.SourcePlace;
import com.homing_labs.cq.lang.java.def.SourceSpan;
import com.homing_labs.cq.lang.java.def.SourceSpans;
import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.lexer.Token;
import com.homing_labs.cq.lang.java.lexer.TokenKind;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import com.homing_labs.cq.lang.java.model.AppliedAnnotation;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.ConstructorSignature;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Enclosure;
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
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.BodyParts;
import com.homing_labs.cq.lang.java.outline.ClauseWord;
import com.homing_labs.cq.lang.java.outline.Declarator;
import com.homing_labs.cq.lang.java.outline.JavaOutliner;
import com.homing_labs.cq.lang.java.outline.ModifierItem;
import com.homing_labs.cq.lang.java.outline.Outline;
import com.homing_labs.cq.lang.java.outline.Parameter;
import com.homing_labs.cq.lang.java.outline.TokenRange;
import com.homing_labs.cq.lang.java.outline.TypeHeader;
import com.homing_labs.cq.lang.java.outline.TypeKeyword;
import com.homing_labs.cq.lang.java.taxonomy.AnonymousClass;
import com.homing_labs.cq.lang.java.taxonomy.ClassAnnotation;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ClassProperty;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.Dependency;
import com.homing_labs.cq.lang.java.taxonomy.EnumConstant;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import com.homing_labs.cq.lang.java.taxonomy.LocalClass;
import com.homing_labs.cq.lang.java.taxonomy.MemberClass;
import com.homing_labs.cq.lang.java.taxonomy.Method;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.RecordComponent;
import com.homing_labs.cq.lang.java.taxonomy.StaticField;
import com.homing_labs.cq.lang.java.taxonomy.StaticInitialiser;
import com.homing_labs.cq.lang.java.taxonomy.StaticMemberClass;
import com.homing_labs.cq.lang.java.taxonomy.StaticMethod;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.taxonomy.Superinterface;
import com.homing_labs.cq.lang.java.taxonomy.TypeParameter;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Where the classes of a unit, and the values their attributes hold, are written in the unit's source file. The class
 * files say what a class has, and the source's outline where each part of it is written; the two are matched by name,
 * and a method or a constructor by the types of its parameters too, erased as the class file has them and matched by
 * the names they are written with.
 *
 * <p>The outline reads a top-level type down to the parts of its body, and no further: a member type's header is read
 * but not its body, and a method's body is left as tokens. What is written in those is placed within them, not
 * exactly. What the compiler implies or generates is written nowhere. javac puts some objects together from several
 * parts of the source: the static initialiser from every static block and every static field's initial value that is
 * no constant, and each constructor that hands over to no other runs the instance initialisers and the instance
 * fields' initial values too; all of them are its places.</p>
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

    /** Where a class of the unit - its top-level class, or one nested in it - is declared in its outlined source. */
    public SourcePlace ofClass(final JavaUnit unit, final Outline outline, final ClassName name) {
        return switch (found(unit, outline, name)) {
            case Declared declared -> new SourcePlace.Written(spans(outline, List.of(declared.declaration())));
            case Inside inside -> new SourcePlace.Within(spans(outline, inside.ranges()));
            case Missing _ -> SourcePlace.Unwritten.INSTANCE;
        };
    }

    /**
     * Where a value that a class of the unit holds, as one of the taxonomy's objects, is written in the unit's outlined
     * source.
     */
    public SourcePlace of(final JavaUnit unit, final Outline outline, final ClassName inClass, final ObjectKind object,
                          final Scalar value) {
        return switch (found(unit, outline, inClass)) {
            case Declared declared -> of(unit, outline, inClass, declared, object, value);
            case Inside inside -> new SourcePlace.Within(spans(outline, inside.ranges()));
            case Missing _ -> SourcePlace.Unwritten.INSTANCE;
        };
    }

    private SourcePlace of(final JavaUnit unit, final Outline outline, final ClassName inClass,
                           final Declared declared, final ObjectKind object, final Scalar value) {
        final TypeHeader header = declared.header();
        return switch (object) {
            case ClassKind kind -> throw new IllegalArgumentException(kind.getClass().getSimpleName()
                    + " is a kind of class, and no object of one");
            case ClassProperty.Visibility _ -> place(outline, words(header, "public", "protected", "private"));
            case ClassProperty.Extensibility _ -> place(outline, extensibility(header, (Extensibility) value));
            case ClassAnnotation _ ->
                    place(outline, annotations(header, ((AppliedAnnotation) value).annotation().type()));
            case TypeParameter _ -> place(outline, header.typeParameters().items().stream()
                    .filter(parameter -> parameter.name().text().equals(((TypeParameterValue) value).name()))
                    .map(parameter -> parameter.range())
                    .toList());
            case Superclass _ -> place(outline, header.superclass().stream()
                    .filter(type -> fits(erased(outline, type.range(), 0), qualified((TypeUse) value)))
                    .map(type -> type.range())
                    .toList());
            case Superinterface _ -> place(outline, header.superinterfaces().stream()
                    .filter(type -> fits(erased(outline, type.range(), 0), qualified((TypeUse) value)))
                    .map(type -> type.range())
                    .toList());
            case RecordComponent _ -> place(outline, header.components().stream()
                    .flatMap(components -> components.items().stream())
                    .filter(component -> component.name().text().equals(((RecordComponentValue) value).name()))
                    .map(component -> component.range())
                    .toList());
            case Dependency _ -> SourcePlace.Unplaced.INSTANCE;
            case Constructor _, StaticInitialiser _, StaticField _, Field _, EnumConstant _, StaticMethod _, Method _,
                 StaticMemberClass _, MemberClass _, LocalClass _, AnonymousClass _ -> declared.parts()
                    .map(parts -> member(unit, outline, inClass, declared, parts, value))
                    .orElseGet(() -> new SourcePlace.Within(spans(outline, List.of(declared.declaration()))));
        };
    }

    /** Where a member of a class whose body's parts the outline read is written. */
    private SourcePlace member(final JavaUnit unit, final Outline outline, final ClassName inClass,
                               final Declared declared, final BodyParts parts, final Scalar value) {
        return switch (value) {
            case FieldValue field -> generated(field.origin()) ? SourcePlace.Unwritten.INSTANCE
                    : place(outline, field(outline, parts, field.name()));
            case EnumConstantValue constant -> place(outline, parts.of(BodyPart.EnumConstant.class).stream()
                    .filter(part -> part.name().text().equals(constant.name()))
                    .map(part -> trimmed(outline, part.range(), true))
                    .toList());
            case MethodValue method -> generated(method.origin()) ? SourcePlace.Unwritten.INSTANCE
                    : place(outline, ranges(matching(methods(outline, parts, method.name()),
                    qualified(method.signature().map(MethodSignature::parameters).orElse(method.parameters())))));
            case ConstructorValue constructor -> generated(constructor.origin()) ? SourcePlace.Unwritten.INSTANCE
                    : constructor(outline, declared, parts, constructor);
            case InitialiserValue initialiser -> generated(initialiser.origin()) ? SourcePlace.Unwritten.INSTANCE
                    : place(outline, staticInitialisers(outline, declared.header(), parts, constants(unit)));
            case NestedClass nested -> memberType(outline, inClass, parts, nested.name())
                    .<SourcePlace>map(type -> new SourcePlace.Written(spans(outline, List.of(type.declaration()))))
                    .orElse(SourcePlace.Unwritten.INSTANCE);
            case EnclosedClass enclosed -> new SourcePlace.Within(
                    spans(outline, enclosing(unit, outline, declared, parts, enclosed.enclosure())));
            default ->
                    throw new IllegalArgumentException(value.getClass().getSimpleName() + " is no member of a class");
        };
    }

    /** Where a class of the unit is declared in the source, as far as the outline read it. */
    private Found found(final JavaUnit unit, final Outline outline, final ClassName name) {
        final ClassName top = unit.identity().name();
        if (name.equals(top)) {
            return outline.types().stream()
                    .filter(type -> type.header().name().text().equals(top.simpleName()))
                    .findFirst()
                    .<Found>map(type -> new Declared(type.header(), Optional.of(type.parts()),
                            trimmed(outline, type.range(), false)))
                    .orElseGet(Missing::new);
        }
        return nesting(top, unit.attributes(), name)
                .map(nesting -> nested(unit, outline, nesting, found(unit, outline, nesting.in())))
                .orElseGet(Missing::new);
    }

    /** Where a class nested in another is declared, from where the other is. */
    private Found nested(final JavaUnit unit, final Outline outline, final Nesting nesting, final Found in) {
        return switch (in) {
            case Declared declared when declared.parts().isPresent() -> switch (nesting.value()) {
                case NestedClass nested -> {
                    final Optional<Declared> type = memberType(outline, nesting.in(), declared.parts().get(),
                            nested.name());
                    yield type.isPresent() ? type.get() : new Missing();
                }
                case EnclosedClass enclosed ->
                        new Inside(enclosing(unit, outline, declared, declared.parts().get(), enclosed.enclosure()));
                default -> new Missing();
            };
            case Declared declared -> new Inside(List.of(declared.declaration()));
            case Inside inside -> inside;
            case Missing missing -> missing;
        };
    }

    /** The class of the unit a class is nested in, and the value that holds it there. */
    private Optional<Nesting> nesting(final ClassName in, final Attributes attributes, final ClassName name) {
        for (final Entry entry : attributes.entries()) {
            for (final WithSource<? extends Scalar> held : entry.all()) {
                final Optional<Nesting> found = switch (held.object()) {
                    case NestedClass nested -> nested.name().equals(name) ? Optional.of(new Nesting(in, nested))
                            : nesting(nested.name(), nested.attributes(), name);
                    case EnclosedClass enclosed -> enclosed.name().equals(name) ? Optional.of(new Nesting(in, enclosed))
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

    /** A member type of a class, as the class's body declares it: its header is read, but not its body. */
    private Optional<Declared> memberType(final Outline outline, final ClassName inClass, final BodyParts parts,
                                          final ClassName name) {
        final String simpleName = name.binaryName().substring(inClass.binaryName().length() + 1);
        return parts.of(BodyPart.TypeDeclaration.class).stream()
                .filter(type -> type.header().name().text().equals(simpleName))
                .findFirst()
                .map(type -> new Declared(type.header(), Optional.empty(), trimmed(outline, type.range(), false)));
    }

    /**
     * Where a local or an anonymous class is declared: in the method, the constructor or the initialisers whose code
     * declares it, or, when none of those is found, anywhere in the class.
     */
    private List<TokenRange> enclosing(final JavaUnit unit, final Outline outline, final Declared declared,
                                       final BodyParts parts, final Enclosure enclosure) {
        final List<TokenRange> found = switch (enclosure) {
            case Enclosure.InMethod method when method.name().startsWith(LAMBDA) ->
                    inLambda(unit, outline, declared.header(), parts, method.name());
            case Enclosure.InMethod method ->
                    ranges(resembling(methods(outline, parts, method.name()), qualified(method.parameters())));
            case Enclosure.InConstructor constructor -> ranges(resembling(
                    constructors(outline, declared.header(), parts),
                    writtenParameters(declared.header(), constructor.parameters())));
            case Enclosure.InInitialiser _ -> initialisers(outline, parts);
        };
        return found.isEmpty() ? List.of(declared.declaration()) : found;
    }

    /**
     * Where javac's method for a lambda was written: {@code lambda$name$0} is in the method of that name,
     * {@code lambda$new$0} in a constructor or an instance initialiser, {@code lambda$static$0} in a static one.
     */
    private List<TokenRange> inLambda(final JavaUnit unit, final Outline outline, final TypeHeader header,
                                      final BodyParts parts, final String name) {
        final String in = name.substring(LAMBDA.length(), Math.max(LAMBDA.length(), name.lastIndexOf('$')));
        return switch (in) {
            case "new" -> Stream.concat(ranges(constructors(outline, header, parts)).stream(),
                    instanceInitialisers(outline, header, parts).stream()).toList();
            case "static" -> staticInitialisers(outline, header, parts, constants(unit));
            default -> ranges(methods(outline, parts, in));
        };
    }

    /**
     * The field of a body with the name: its declaration, or, when the declaration declares more than one, its
     * modifiers and type, and the field's own declarator.
     */
    private List<TokenRange> field(final Outline outline, final BodyParts parts, final String name) {
        for (final BodyPart.Field field : parts.of(BodyPart.Field.class)) {
            for (final Declarator declarator : field.declarators().items()) {
                if (declarator.name().text().equals(name)) {
                    return field.declarators().items().size() == 1 ? List.of(trimmed(outline, field.range(), false))
                            : List.of(new TokenRange(field.range().from(), field.type().range().to()),
                            declarator.range());
                }
            }
        }
        return List.of();
    }

    /** Where a constructor is written: itself, and, unless it hands over to another, the instance initialisers. */
    private SourcePlace constructor(final Outline outline, final Declared declared, final BodyParts parts,
                                    final ConstructorValue constructor) {
        final List<Candidate> found = matching(constructors(outline, declared.header(), parts),
                constructor.signature().map(ConstructorSignature::parameters).map(this::qualified)
                        .orElseGet(() -> writtenParameters(declared.header(), constructor.parameters())));
        final List<TokenRange> ranges = new ArrayList<>(ranges(found));
        if (found.stream().anyMatch(candidate -> !delegates(outline, candidate))) {
            ranges.addAll(instanceInitialisers(outline, declared.header(), parts));
        }
        return place(outline, ranges);
    }

    /** The methods of a body with the name, each with its parameters' types as written. */
    private List<Candidate> methods(final Outline outline, final BodyParts parts, final String name) {
        return parts.of(BodyPart.Method.class).stream()
                .filter(method -> method.name().text().equals(name))
                .map(method -> new Candidate(trimmed(outline, method.range(), false),
                        writtenTypes(outline, method.parameters().formal()), method.body()))
                .toList();
    }

    /** The constructors of a body - a record's compact one taking its components - each with its parameters' types. */
    private List<Candidate> constructors(final Outline outline, final TypeHeader header, final BodyParts parts) {
        final List<Candidate> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.Constructor constructor -> found.add(new Candidate(
                        trimmed(outline, constructor.range(), false),
                        writtenTypes(outline, constructor.parameters().formal()), constructor.body()));
                case BodyPart.CompactConstructor compact -> found.add(new Candidate(
                        trimmed(outline, compact.range(), false),
                        header.components().stream().flatMap(components -> components.items().stream())
                                .map(component -> erased(outline, component.type().range(),
                                        component.variable() ? 1 : 0))
                                .toList(),
                        Optional.empty()));
                default -> {
                }
            }
        }
        return found;
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
    private boolean delegates(final Outline outline, final Candidate candidate) {
        final TokenRange body = candidate.body().orElse(new TokenRange(0, 0));
        int depth = 0;
        for (int at = body.from(); at < body.to(); at++) {
            final Token token = outline.tokens().get(at);
            if (comment(token)) {
                continue;
            }
            switch (outline.tokens().text(token)) {
                case "{" -> depth++;
                case "}" -> depth--;
                case "this" -> {
                    if (depth == 1 && next(outline.tokens(), at, body.to()).equals("(")) {
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
    private List<TokenRange> instanceInitialisers(final Outline outline, final TypeHeader header,
                                                  final BodyParts parts) {
        final List<TokenRange> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.Initializer initializer when !initializer.isStatic() ->
                        found.add(trimmed(outline, initializer.range(), false));
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
    private List<TokenRange> staticInitialisers(final Outline outline, final TypeHeader header,
                                                final BodyParts parts, final Set<String> constants) {
        final List<TokenRange> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.EnumConstant constant -> found.add(trimmed(outline, constant.range(), true));
                case BodyPart.Initializer initializer when initializer.isStatic() ->
                        found.add(trimmed(outline, initializer.range(), false));
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
     * The fields of the unit's top-level class that are constants (JLS 4.12.4): the class file holds their values, and
     * no initialiser computes them. Only the class file can say which they are - the names in a value's expression
     * may be any field's - so it is read again for them, through the class's loader.
     */
    private Set<String> constants(final JavaUnit unit) {
        final ClassLoader loader = Optional.ofNullable(unit.type().getClassLoader())
                .orElseGet(ClassLoader::getSystemClassLoader);
        final String classFile = unit.type().getName().replace('.', '/') + ".class";
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
    private List<TokenRange> initialisers(final Outline outline, final BodyParts parts) {
        final List<TokenRange> found = new ArrayList<>();
        for (final BodyPart part : parts.items()) {
            switch (part) {
                case BodyPart.EnumConstant constant when constant.arguments().isPresent()
                        || constant.body().isPresent() -> found.add(trimmed(outline, constant.range(), true));
                case BodyPart.Initializer initializer -> found.add(trimmed(outline, initializer.range(), false));
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

    /** What a header writes of how its class can be extended: final, sealed and what it permits, or non-sealed. */
    private List<TokenRange> extensibility(final TypeHeader header, final Extensibility extensibility) {
        return switch (extensibility) {
            case Extensibility.Final _ -> words(header, "final");
            case Extensibility.Open _ -> words(header, "non-sealed");
            case Extensibility.Sealed _ -> Stream.concat(words(header, "sealed").stream(),
                    header.clauses().items().stream()
                            .filter(clause -> clause.word().equals(ClauseWord.Permits.INSTANCE))
                            .map(clause -> clause.range())).toList();
        };
    }

    /** The annotations of a header of the type, by the names it is written with. */
    private List<TokenRange> annotations(final TypeHeader header, final ClassName type) {
        return header.modifiers().annotations().stream()
                .filter(annotation -> fits(annotation.name().text(), qualified(type)))
                .map(ModifierItem.Annotation::range)
                .toList();
    }

    /** The types of the parameters a constructor is written with: an enum's take its name and ordinal first. */
    private List<String> writtenParameters(final TypeHeader header, final Parameters parameters) {
        final List<String> all = qualified(parameters);
        return header.keyword() instanceof TypeKeyword.Enum && all.size() >= 2 ? all.subList(2, all.size()) : all;
    }

    /** The types of formal parameters as written, each erased to its names and its dimensions. */
    private List<String> writtenTypes(final Outline outline, final List<Parameter.Formal> parameters) {
        return parameters.stream()
                .map(parameter -> erased(outline, parameter.type().range(),
                        brackets(outline, parameter.dimensions()) + (parameter.variable() ? 1 : 0)))
                .toList();
    }

    /**
     * A type as written, erased to the names it is written with and its dimensions: {@code java.util.Map.Entry[]} for
     * {@code java.util.Map.@A Entry<K, V>[]}. A type variable stays as it is named.
     */
    private String erased(final Outline outline, final TokenRange type, final int dimensions) {
        final TokenStream tokens = outline.tokens();
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

    private int brackets(final Outline outline, final TokenRange range) {
        int brackets = 0;
        for (int at = range.from(); at < range.to(); at++) {
            if (outline.tokens().text(outline.tokens().get(at)).equals("[")) {
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
    private TokenRange trimmed(final Outline outline, final TokenRange range, final boolean separated) {
        int to = range.to();
        while (to > range.from()) {
            final Token last = outline.tokens().get(to - 1);
            final String text = outline.tokens().text(last);
            if (!(comment(last) || separated && (text.equals(",") || text.equals(";")))) {
                break;
            }
            to--;
        }
        return new TokenRange(range.from(), to);
    }

    /** Written in the ranges, or nowhere when there are none. */
    private SourcePlace place(final Outline outline, final List<TokenRange> ranges) {
        final SourceSpans spans = spans(outline, ranges);
        return spans.items().isEmpty() ? SourcePlace.Unwritten.INSTANCE : new SourcePlace.Written(spans);
    }

    /** The text the ranges of tokens are written in, in the order written. */
    private SourceSpans spans(final Outline outline, final List<TokenRange> ranges) {
        return new SourceSpans(ranges.stream()
                .filter(range -> !range.isEmpty())
                .sorted(Comparator.comparingInt(TokenRange::from))
                .map(range -> new SourceSpan(outline.tokens().get(range.from()).start(),
                        outline.tokens().get(range.to() - 1).end()))
                .distinct()
                .toList());
    }

    private String fileName(final JavaSource source) {
        final String path = Optional.ofNullable(source.file().getPath()).orElse("");
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Where a class of the unit is declared, as far as the outline read the source. */
    private sealed interface Found {
    }

    /** Declared there, with its header read, and its body's parts when the outline read those too. */
    private record Declared(TypeHeader header, Optional<BodyParts> parts, TokenRange declaration) implements Found {
    }

    /** Declared somewhere inside these, in tokens the outline left unread. */
    private record Inside(List<TokenRange> ranges) implements Found {
    }

    /** Declared nowhere in the source. */
    private record Missing() implements Found {
    }

    /** The class a class is nested in, and the value that holds it there: a nested or an enclosed class. */
    private record Nesting(ClassName in, Scalar value) {
    }

    /** A method or a constructor as written: where, its parameters' types erased to the names written, and its body. */
    private record Candidate(TokenRange range, List<String> parameters, Optional<TokenRange> body) {
    }
}
