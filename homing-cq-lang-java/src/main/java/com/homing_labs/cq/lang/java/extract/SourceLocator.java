package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.extract.SourceReading.Declared;
import com.homing_labs.cq.lang.java.extract.SourceReading.Found;
import com.homing_labs.cq.lang.java.extract.SourceReading.Inside;
import com.homing_labs.cq.lang.java.extract.SourceReading.Missing;
import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.model.AppliedAnnotation;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassInitialisation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Entries;
import com.homing_labs.cq.lang.java.model.Entry;
import com.homing_labs.cq.lang.java.model.EnumConstantValue;
import com.homing_labs.cq.lang.java.model.Extensibility;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.InitialisationStep;
import com.homing_labs.cq.lang.java.model.InitialisationSteps;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.Visibility;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import com.homing_labs.cq.lang.java.outline.JavaOutliner;
import com.homing_labs.cq.lang.java.outline.Outline;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The source of each value a unit's attributes hold: where the unit's source file writes it, as blocks of code copied
 * out of the file, and what the file says of it there, by the kind of value it is. The class files say what a class
 * has, and the source's outline where each part of it is written; the two are matched by name, and a method or a
 * constructor by its type parameters and the types of its parameters too, as its signature declares them, matched by
 * the names they are written with.
 *
 * <p>The outline reads a top-level type down to the parts of its body, and its member types' bodies so too, one level
 * down; a type nested deeper is read down to its header, and a method's body is left as tokens. What is written in
 * those is written within them, not exactly. What the compiler implies or generates is written nowhere. javac puts
 * some things together from several parts of the source: how a class is initialised, in steps, from its enum constants,
 * its static blocks and its static fields' initial values that are no constants; and each constructor that hands over
 * to no other runs the instance initialisers and the instance fields' initial values too.</p>
 *
 * <p>Where each class is declared is found by a {@link SourceReading}; what a header writes is read by
 * {@link HeaderSources}, what a body writes by {@link MemberSources}, and what javac puts together by
 * {@link Initialisers}.</p>
 */
public record SourceLocator() implements StatelessFunctionalObject {

    public static final SourceLocator INSTANCE = new SourceLocator();

    /** The unit's source, outlined; nothing for a unit read without one. */
    public Optional<Outline> outline(final JavaUnit unit) {
        return unit.source().map(source -> JavaOutliner.INSTANCE.outline(
                JavaLexer.INSTANCE.scan(new SourceText(fileName(source), source.text()))));
    }

    /**
     * The unit's attributes, each value with its source processed - those of the classes nested in it too - from its
     * source, as {@link #outline} outlines it.
     */
    public Attributes sourced(final JavaUnit unit, final Outline outline) {
        return sourced(new SourceReading(unit, outline), unit.identity().name(), unit.attributes());
    }

    /**
     * How the unit's top-level class is initialised, with its source: the steps its body writes, in order - from its
     * source, as {@link #outline} outlines it.
     */
    public WithSource<ClassInitialisation> initialisation(final JavaUnit unit, final Outline outline) {
        return initialisation(new SourceReading(unit, outline), unit.identity().name(), unit.initialisation());
    }

    /** Where the unit's top-level class is declared: from its documentation comment to its closing brace. */
    public Optional<CodeBlock> declaration(final JavaUnit unit) {
        return outline(unit).flatMap(outline -> new SourceReading(unit, outline).found(unit.identity().name())
                instanceof Declared declared ? Optional.of(declared.lexed().block(declared.declaration()))
                : Optional.<CodeBlock>empty());
    }

    /** The attributes of a class of the unit, each value with its source. */
    private Attributes sourced(final SourceReading reading, final ClassName inClass, final Attributes attributes) {
        final Found where = reading.found(inClass);
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
    private WithSource<? extends Scalar> sourced(final SourceReading reading, final ClassName inClass,
                                                 final Found where, final ObjectKind object, final Scalar value) {
        final MemberSources members = MemberSources.INSTANCE;
        final HeaderSources headers = HeaderSources.INSTANCE;
        return switch (value) {
            case NestedClass nested -> new WithSource<>(
                    new NestedClass(nested.name(), sourced(reading, nested.name(), nested.attributes()),
                            initialisation(reading, nested.name(), nested.initialisation())),
                    inBody(where, (declared, body) -> members.nestedClass(body, inClass, nested)));
            case EnclosedClass enclosed -> new WithSource<>(new EnclosedClass(enclosed.name(), enclosed.enclosure(),
                    sourced(reading, enclosed.name(), enclosed.attributes()),
                    initialisation(reading, enclosed.name(), enclosed.initialisation())),
                    inBody(where, (declared, body) -> new ProcessedSource.Within<>(
                            reading.enclosing(inClass, declared, body, enclosed.enclosure()))));
            case FieldValue field ->
                    new WithSource<>(field, inBody(where, (declared, body) -> members.field(body, field)));
            case EnumConstantValue constant ->
                    new WithSource<>(constant, inBody(where, (declared, body) -> members.enumConstant(body, constant)));
            case MethodValue method ->
                    new WithSource<>(method, inBody(where, (declared, body) -> members.method(body, method)));
            case ConstructorValue constructor -> new WithSource<>(constructor,
                    inBody(where, (declared, body) -> members.constructor(declared, body, constructor)));
            case Visibility visibility -> new WithSource<>(visibility, inHeader(where, headers::visibility));
            case Extensibility extensibility -> new WithSource<>(extensibility,
                    inHeader(where, declared -> headers.extensibility(declared, extensibility)));
            case AppliedAnnotation annotation ->
                    new WithSource<>(annotation, inHeader(where, declared -> headers.annotation(declared, annotation)));
            case TypeParameterValue parameter -> new WithSource<>(parameter,
                    inHeader(where, declared -> headers.typeParameter(declared, parameter)));
            case RecordComponentValue component -> new WithSource<>(component,
                    inHeader(where, declared -> headers.recordComponent(declared, component)));
            case TypeUse type ->
                    new WithSource<>(type, inHeader(where, declared -> headers.supertype(declared, object, type)));
            case ClassName dependency -> new WithSource<>(dependency, new ProcessedSource.Unplaced<>());
            default -> throw new IllegalArgumentException(value.getClass().getSimpleName() + " is held by no object");
        };
    }

    /**
     * The source of a member of a class: read from the class's body, or within the class when its body is not read, or
     * within what holds the class, or nowhere when the class is declared nowhere.
     */
    private <T extends Scalar> ProcessedSource<T> inBody(
            final Found where, final BiFunction<Declared, ReadBody, ProcessedSource<T>> read) {
        return switch (where) {
            case Declared declared -> declared.body().map(body -> read.apply(declared, body))
                    .orElseGet(() -> new ProcessedSource.Within<>(declared.lexed().blocks(
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

    /**
     * How a class of the unit is initialised, with its source: the steps its body writes - nothing, when no code runs,
     * or when all that runs the compiler wrote, as a class's that only sets up its {@code assert}s.
     */
    private WithSource<ClassInitialisation> initialisation(final SourceReading reading, final ClassName inClass,
                                                           final WithSource<ClassInitialisation> read) {
        final ProcessedSource<ClassInitialisation> source = switch (read.object()) {
            case ClassInitialisation.None _ -> new ProcessedSource.Unwritten<>();
            case ClassInitialisation.Code _ -> inBody(reading.found(inClass),
                    (declared, body) -> steps(reading, inClass, declared, body));
        };
        return new WithSource<>(read.object(), source);
    }

    private ProcessedSource<ClassInitialisation> steps(final SourceReading reading, final ClassName inClass,
                                                       final Declared declared, final ReadBody body) {
        final List<InitialisationStep> steps = Initialisers.INSTANCE.staticInitialisers(body.lexed(),
                declared.header(), body.parts(), Initialisers.INSTANCE.constants(reading.unit(), inClass));
        return steps.isEmpty() ? new ProcessedSource.Unwritten<>()
                : new ProcessedSource.OfInitialisation(new InitialisationSteps(steps));
    }

    private String fileName(final JavaSource source) {
        final String path = Optional.ofNullable(source.file().getPath()).orElse("");
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
