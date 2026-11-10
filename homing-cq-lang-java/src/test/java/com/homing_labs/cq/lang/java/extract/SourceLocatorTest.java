package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.fixtures.Plain;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassInitialisation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.CodeBlocks;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Entry;
import com.homing_labs.cq.lang.java.model.EnumConstantValue;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.InitialisationStep;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.Origin;
import com.homing_labs.cq.lang.java.model.PrimitiveType;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.model.WrittenParameters;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import com.homing_labs.cq.lang.java.outline.Outline;
import com.homing_labs.cq.lang.java.taxonomy.AnonymousClass;
import com.homing_labs.cq.lang.java.taxonomy.ClassAnnotation;
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
import com.homing_labs.cq.lang.java.taxonomy.StaticMemberClass;
import com.homing_labs.cq.lang.java.taxonomy.StaticMethod;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.taxonomy.Superinterface;
import com.homing_labs.cq.lang.java.taxonomy.TypeParameter;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.unit.JavaUnits;
import hue.captains.singapura.tao.ontology.Scalar;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each value of classes written for it - a class, an enum and a record in this module's tests - and of the fixtures,
 * given its source when its unit is read: a block of code, and what the source says of the value there.
 */
class SourceLocatorTest {

    private static final String HERE = "com.homing_labs.cq.lang.java.extract.";

    private static final TypeUse INT = new TypeUse.Primitive(PrimitiveType.Int.INSTANCE);

    private final List<JavaUnit> units = JavaUnitReader.INSTANCE.read(Placed.class);

    @Test
    void aClassIsDeclaredFromItsDocumentationCommentToItsClosingBrace() {
        final CodeBlock declaration = SourceLocator.INSTANCE.declaration(unit(Placed.class)).orElseThrow();
        assertTrue(declaration.text().startsWith("/** A class with an object of each kind"), declaration.text());
        assertTrue(squashed(declaration).endsWith("class Deeper { int hidden; } } }"), declaration.text());
        assertEquals(8, declaration.line());
    }

    @Test
    void everyBlockIsCopiedFromWhereItIsInTheFile() {
        final String text = unit(Placed.class).source().orElseThrow().text();
        final List<CodeBlock> blocks = blocks(unit(Placed.class));
        assertTrue(blocks.size() > 40, blocks.size() + " blocks");
        for (final CodeBlock block : blocks) {
            assertEquals(block.text(), text.substring(block.offset(), block.offset() + block.text().length()));
            assertEquals(text.substring(0, block.offset()).split("\n", -1).length, block.line(), block.text());
        }
    }

    @Test
    void theHeaderHoldsTheClassesAnnotationsTypeParametersAndSupertypes() {
        final ProcessedSource.OfAnnotation annotation = assertInstanceOf(ProcessedSource.OfAnnotation.class,
                sourceOf(Placed.class, ClassAnnotation.INSTANCE, any -> true));
        assertEquals("@Deprecated", annotation.block().text());
        assertEquals(Optional.empty(), annotation.arguments());
        assertEquals("public", written(sourceOf(Placed.class, ClassProperty.Visibility.INSTANCE, any -> true)));
        final ProcessedSource.OfTypeParameter parameter = assertInstanceOf(ProcessedSource.OfTypeParameter.class,
                sourceOf(Placed.class, TypeParameter.INSTANCE, any -> true));
        assertEquals("T extends Comparable<T>", parameter.block().text());
        assertEquals(List.of("Comparable<T>"), texts(parameter.bounds()));
        assertEquals("AbstractList<T>", written(sourceOf(Placed.class, Superclass.INSTANCE, any -> true)));
        assertEquals("RandomAccess", written(sourceOf(Placed.class, Superinterface.INSTANCE, any -> true)));
        assertInstanceOf(ProcessedSource.Unwritten.class, sourceOf(Placed.class,
                ClassProperty.Extensibility.INSTANCE, any -> true), "an open class says nothing of it");
    }

    @Test
    void aFieldIsItsDeclarationWithItsOwnDeclaratorAndInitialValue() {
        final ProcessedSource.OfField constant = field(Placed.class, StaticField.INSTANCE, "CONSTANT");
        assertEquals("static final int CONSTANT = 1 + 2;", constant.block().text(), "the comment after it left out");
        assertEquals("CONSTANT = 1 + 2", constant.declarator().text());
        assertEquals(Optional.of("1 + 2"), constant.initialValue().map(CodeBlock::text));
        final ProcessedSource.OfField first = field(Placed.class, Field.INSTANCE, "first");
        assertEquals("int first = 1, second, third = 3;", first.block().text());
        assertEquals("first = 1", first.declarator().text());
        final ProcessedSource.OfField second = field(Placed.class, Field.INSTANCE, "second");
        assertEquals(first.block(), second.block(), "a declaration shared");
        assertEquals("second", second.declarator().text());
        assertEquals(Optional.empty(), second.initialValue());
        assertEquals("private final List<T> items;", field(Placed.class, Field.INSTANCE, "items").block().text());
    }

    @Test
    void overloadsAreToldApartByTheTypesOfTheirParameters() {
        assertEquals("void overloaded(final int value) { }",
                squashed(method(Placed.class, Method.Implementation.INSTANCE, overloaded(INT)).block()));
        assertEquals("void overloaded(final String value) { }", squashed(method(Placed.class,
                Method.Implementation.INSTANCE, overloaded(named("java.lang.String"))).block()));
        assertEquals("void overloaded(final Map.Entry<String, T>[] entries) { }", squashed(method(Placed.class,
                Method.Implementation.INSTANCE, overloaded(new TypeUse.Array(named("java.util.Map$Entry"))))
                .block()));
        final ProcessedSource.OfMethod variable = method(Placed.class, Method.Implementation.INSTANCE,
                overloaded(named("java.lang.Comparable"), new TypeUse.Array(INT)));
        assertEquals("void overloaded(final T value, final int... rest) { }", squashed(variable.block()));
        assertEquals(List.of("value", "rest"), names(variable.parameters()),
                "the names the class file does not keep");
        assertEquals(List.of("final T value", "final int... rest"), variable.parameters().items().stream()
                .map(parameter -> parameter.block().text()).toList());
        final ProcessedSource.OfMethod count = method(Placed.class, StaticMethod.Implementation.INSTANCE,
                any -> true);
        assertEquals("private static int count() { return 0; }", squashed(count.block()));
        assertEquals(Optional.of("{ return 0; }"), count.body().map(this::squashed));
    }

    @Test
    void overloadsWrittenAlikeAreToldApartByTheTypeParametersTheirSignaturesDeclare() {
        assertEquals("/** Written as the next is: only the bound of its own T tells them apart. */"
                        + " <T extends Number> String picked(final T value) { return new Object() { }.toString(); }",
                squashed(method(Placed.class, Method.Implementation.INSTANCE,
                        takes("picked", named("java.lang.Number"))).block()));
        assertEquals("<T extends CharSequence> String picked(final T value) { return new Object() { }.toString(); }",
                squashed(method(Placed.class, Method.Implementation.INSTANCE,
                        takes("picked", named("java.lang.CharSequence"))).block()));
        assertEquals("void put(final T value) { // the class's T }", squashed(method(Placed.class,
                Method.Implementation.INSTANCE, takes("put", named("java.lang.Comparable"))).block()));
        assertEquals("<T> void put(final T value) { // a T of its own }", squashed(method(Placed.class,
                Method.Implementation.INSTANCE, takes("put", named("java.lang.Object"))).block()));
    }

    @Test
    void anAnonymousClassIsWithinTheOverloadItsClassFileNames() {
        assertEquals(List.of("/** Written as the next is: only the bound of its own T tells them apart. */"
                + " <T extends Number> String picked"), heads(within(sourceOf(Placed.class,
                AnonymousClass.INSTANCE, value -> ((EnclosedClass) value).name().equals(name("Placed$1"))))));
        assertEquals(List.of("<T extends CharSequence> String picked"), heads(within(sourceOf(Placed.class,
                AnonymousClass.INSTANCE, value -> ((EnclosedClass) value).name().equals(name("Placed$2"))))));
    }

    @Test
    void aMethodTheCompilerGeneratedIsWrittenNowhere() {
        final ProcessedSource.OfMethod get = method(Placed.class, Method.Implementation.INSTANCE,
                value -> value instanceof MethodValue method && method.name().equals("get")
                        && method.origin() instanceof Origin.Declared);
        assertEquals("@Override public T get(final int index) { return items.get(index); }", squashed(get.block()));
        assertEquals(List.of("index"), names(get.parameters()));
        assertInstanceOf(ProcessedSource.Unwritten.class, sourceOf(Placed.class, Method.Implementation.INSTANCE,
                value -> value instanceof MethodValue method && method.name().equals("get")
                        && method.origin() instanceof Origin.Generated), "javac's bridge to AbstractList.get");
    }

    @Test
    void aConstructorRunsTheInstanceInitialisersUnlessItHandsOverToAnother() {
        final ProcessedSource.OfConstructor handing = constructor(Placed.class, Constructor.INSTANCE, parameters(0));
        assertEquals("/** Hands over to the other. */ public Placed() { this(List.of()); }",
                squashed(handing.block()));
        assertEquals(Optional.of("/** Hands over to the other. */"), handing.doc().map(CodeBlock::text));
        assertEquals(List.of(), texts(handing.initialisers()));
        final ProcessedSource.OfConstructor taking = constructor(Placed.class, Constructor.INSTANCE, parameters(1));
        assertEquals("public Placed(final List<T> items) { this.items = items; }", squashed(taking.block()));
        assertEquals(List.of("items"), names(taking.parameters()));
        assertEquals(List.of("first = 1", "third = 3", "{ second = 2; }"), texts(taking.initialisers()));
    }

    @Test
    void aClassIsInitialisedInStepsTheStaticBlocksAndTheInitialValuesThatAreNoConstants() {
        final List<InitialisationStep> steps = steps(Placed.class, new ClassName(Placed.class.getName()));
        assertEquals(List.of("counted = count()", "BOXED = 3", "COPIED = counted", "static { counted++; }"),
                texts(steps));
        assertEquals(List.of(InitialisationStep.InitialValue.class, InitialisationStep.InitialValue.class,
                        InitialisationStep.InitialValue.class, InitialisationStep.StaticBlock.class),
                steps.stream().map(Object::getClass).toList());
        final InitialisationStep.InitialValue counted = (InitialisationStep.InitialValue) steps.getFirst();
        assertEquals("counted", counted.field());
        assertEquals("count()", counted.value().text());
        assertEquals(ClassInitialisation.Code.INSTANCE, unit(Placed.class).initialisation().object());
    }

    @Test
    void aClassThatRunsNoCodeOrOnlyTheCompilersHasNoStepsWrittenAndOneNotReadHasThemWithin() {
        assertEquals(new WithSource<>(ClassInitialisation.None.INSTANCE, new ProcessedSource.Unwritten<>()),
                initialisation(Placed.class, name("Placed$Member")), "no code runs");
        assertEquals(new WithSource<>(ClassInitialisation.Code.INSTANCE, new ProcessedSource.Unwritten<>()),
                initialisation(PlacedInitialisation.class, name("PlacedInitialisation")),
                "javac sets up its assert, and the source writes nothing");
        final WithSource<ClassInitialisation> counter = initialisation(PlacedInitialisation.class,
                name("PlacedInitialisation$1Counter"));
        assertEquals(ClassInitialisation.Code.INSTANCE, counter.object());
        final List<String> within = within(counter.source());
        assertEquals(1, within.size());
        assertTrue(within.getFirst().startsWith("/** A local class whose static field's value is computed. */"
                + " Object counter()"), within.getFirst());
        assertEquals(new WithSource<>(ClassInitialisation.Code.INSTANCE, new ProcessedSource.Unavailable<>()),
                JavaUnits.INSTANCE.of(Placed.class, Optional.empty()).initialisation(), "read without its source");
    }

    @Test
    void aMemberTypesBodyIsReadOneLevelDown() {
        final ProcessedSource.OfNestedClass member = assertInstanceOf(ProcessedSource.OfNestedClass.class,
                sourceOf(Placed.class, StaticMemberClass.INSTANCE, any -> true));
        assertEquals("static class Member { int inside; }", squashed(member.block()));
        assertEquals("static class Member", member.header().text());
        assertEquals(Optional.of("{ int inside; }"), member.body().map(this::squashed));
        assertEquals("int inside;", field(Placed.class, name("Placed$Member"), Field.INSTANCE, "inside").block()
                .text());
    }

    @Test
    void anInnerClassesConstructorTakesTheEnclosingObjectFirstInTheClassFile() {
        final ProcessedSource.OfConstructor constructor = constructor(Placed.class, name("Placed$Inner"),
                Constructor.INSTANCE, any -> true);
        assertEquals("Inner(final int count) { this.count = count; }", squashed(constructor.block()));
        assertEquals(List.of("count"), names(constructor.parameters()));
        assertEquals(List.of("made = LIMIT + count()"), texts(steps(Placed.class, name("Placed$Inner"))),
                "its own constants are left out");
    }

    @Test
    void aTypeNestedTwoLevelsDownIsWrittenThereButWhatItsBodyHoldsOnlyWithinIt() {
        final String deeper = "class Deeper { int hidden; }";
        assertEquals(deeper, squashed(assertInstanceOf(ProcessedSource.OfNestedClass.class, sourceOf(Placed.class,
                name("Placed$Inner"), MemberClass.INSTANCE, any -> true)).block()));
        assertEquals(List.of(deeper), within(sourceOf(Placed.class, name("Placed$Inner$Deeper"), Field.INSTANCE,
                field("hidden"))));
    }

    @Test
    void aLocalClassAndWhatItHoldsAreWithinTheMethodWhoseCodeDeclaresIt() {
        final List<String> local = within(sourceOf(Placed.class, LocalClass.INSTANCE, any -> true));
        assertEquals(1, local.size());
        assertTrue(local.getFirst().startsWith("Runnable local() { class Local"), local.getFirst());
        assertEquals(local, within(sourceOf(Placed.class, name("Placed$1Local"), Method.Implementation.INSTANCE,
                value -> ((MethodValue) value).name().equals("run"))));
    }

    @Test
    void anEnumMakesItsConstantsWhenItIsInitialisedAndItsConstructorsTakeTwoParametersMore() {
        final String two = "TWO(\"two\") { @Override String label() { return \"second\"; } }";
        final ProcessedSource.OfEnumConstant one = constant("ONE");
        assertEquals("ONE", one.block().text());
        assertEquals(Optional.empty(), one.arguments());
        final ProcessedSource.OfEnumConstant second = constant("TWO");
        assertEquals(two, squashed(second.block()));
        assertEquals(Optional.of("(\"two\")"), second.arguments().map(CodeBlock::text));
        assertTrue(second.body().isPresent());
        final List<InitialisationStep> made = steps(PlacedEnum.class, new ClassName(PlacedEnum.class.getName()));
        assertEquals(List.of("ONE", two), texts(made));
        assertEquals(List.of("ONE", "TWO"), made.stream()
                .map(step -> ((InitialisationStep.EnumConstant) step).name()).toList());
        assertEquals("PlacedEnum() { this(\"one\"); }", squashed(constructor(PlacedEnum.class,
                Constructor.INSTANCE, parameters(2)).block()));
        final ProcessedSource.OfConstructor labelled = constructor(PlacedEnum.class, Constructor.INSTANCE,
                parameters(3));
        assertEquals("PlacedEnum(final String label) { this.label = label; }", squashed(labelled.block()));
        assertEquals(List.of("label"), names(labelled.parameters()));
        for (final String method : List.of("values", "valueOf")) {
            assertEquals("enum PlacedEnum", implied(sourceOf(PlacedEnum.class, StaticMethod.Implementation.INSTANCE,
                    value -> ((MethodValue) value).name().equals(method))), method + ", implied by the enum's header");
        }
        assertEquals(List.of(two), within(sourceOf(PlacedEnum.class, AnonymousClass.INSTANCE, any -> true)),
                "a constant's body is a class made in the static initialiser");
    }

    @Test
    void aRecordsComponentsCompactConstructorAndAccessorAreWrittenAndWhatTheLanguageDeclaresIsImplied() {
        assertEquals("int count", written(sourceOf(PlacedRecord.class, RecordComponent.INSTANCE,
                value -> ((RecordComponentValue) value).name().equals("count"))));
        assertEquals("String... names", written(sourceOf(PlacedRecord.class, RecordComponent.INSTANCE,
                value -> ((RecordComponentValue) value).name().equals("names"))));
        final ProcessedSource.OfConstructor compact = constructor(PlacedRecord.class, Constructor.INSTANCE,
                any -> true);
        assertEquals("PlacedRecord { names = names.clone(); }", squashed(compact.block()));
        assertEquals(List.of("count", "names"), names(compact.parameters()), "its components, as its parameters");
        assertEquals("public String[] names() { return names.clone(); }", written(sourceOf(PlacedRecord.class,
                Method.Implementation.INSTANCE, value -> ((MethodValue) value).name().equals("names"))),
                "an accessor written is written");
        assertEquals("int count", implied(sourceOf(PlacedRecord.class, Method.Implementation.INSTANCE,
                value -> ((MethodValue) value).name().equals("count"))),
                "an accessor not written, implied by its component");
        for (final String method : List.of("equals", "hashCode", "toString")) {
            assertEquals("record PlacedRecord(int count, String... names)", implied(sourceOf(PlacedRecord.class,
                    Method.Implementation.INSTANCE, value -> ((MethodValue) value).name().equals(method))),
                    method + ", implied by the record's header");
        }
        assertEquals("int count", implied(sourceOf(PlacedRecord.class, Field.INSTANCE, field("count"))),
                "the field its component implies");
        assertInstanceOf(ProcessedSource.Unwritten.class, sourceOf(PlacedRecord.class,
                ClassProperty.Extensibility.INSTANCE, any -> true), "a record is final without saying so");
    }

    @Test
    void aRecordWhoseBodyWritesNothingHasEveryMemberImpliedByItsDeclaration() {
        final String header = "record BareRecord(String name)";
        assertEquals(header, implied(sourceOf(BareRecord.class, Constructor.INSTANCE, any -> true)),
                "its canonical constructor");
        assertEquals("String name", implied(sourceOf(BareRecord.class, Method.Implementation.INSTANCE,
                value -> ((MethodValue) value).name().equals("name"))), "its accessor");
        assertEquals("String name", implied(sourceOf(BareRecord.class, Field.INSTANCE, field("name"))), "its field");
        for (final String method : List.of("equals", "hashCode", "toString")) {
            assertEquals(header, implied(sourceOf(BareRecord.class, Method.Implementation.INSTANCE,
                    value -> ((MethodValue) value).name().equals(method))), method);
        }
    }

    @Test
    void aDependencyIsNotPlacedYet() {
        assertInstanceOf(ProcessedSource.Unplaced.class, sourceOf(Placed.class, Dependency.Signature.INSTANCE,
                any -> true));
    }

    @Test
    void aUnitThatSharesItsFileIsFoundByItsNameAndAnAnonymousClassWithinItsMethod() {
        final List<JavaUnit> fixtures = JavaUnitReader.INSTANCE.read(Plain.class);
        final JavaUnit second = fixtures.stream().filter(unit -> unit.type().getSimpleName().equals("SecondInFile"))
                .findFirst().orElseThrow();
        assertEquals("/** The second top-level class of TwoInOneFile.java: its own unit, sharing that file. */"
                        + " class SecondInFile { }",
                squashed(SourceLocator.INSTANCE.declaration(second).orElseThrow()));
        final JavaUnit nesting = fixtures.stream().filter(unit -> unit.type().getSimpleName().equals("Nesting"))
                .findFirst().orElseThrow();
        final List<String> within = within(nesting.attributes().entry(AnonymousClass.INSTANCE).all().getFirst()
                .source());
        assertEquals(1, within.size());
        assertTrue(within.getFirst().startsWith("/** Declares a local class and an anonymous class. */ public Object"
                + " localAndAnonymous()"), within.getFirst());
    }

    @Test
    void aUnitReadWithoutItsSourceHasEveryValuesSourceUnavailable() {
        final JavaUnit unit = JavaUnits.INSTANCE.of(Placed.class, Optional.empty());
        assertEquals(Optional.empty(), SourceLocator.INSTANCE.outline(unit));
        assertEquals(Optional.empty(), SourceLocator.INSTANCE.declaration(unit));
        assertTrue(values(unit.attributes()).allMatch(value -> value.source() instanceof ProcessedSource.Unavailable),
                "nothing is known of where any is written");
    }

    /**
     * Over this module's classes, its tests' and the outline's: each field, method and constructor written in a
     * top-level class, or in a member class of one, is the source of exactly one in its class file, and every other one
     * in the class file is implied by what its class writes, or, generated, written nowhere.
     */
    @Test
    void everyMemberWrittenIsTheSourceOfExactlyOneInTheClassFile() {
        final List<JavaUnit> read = Stream.of(JavaUnit.class, Outline.class, Placed.class)
                .flatMap(type -> JavaUnitReader.INSTANCE.read(type).stream())
                .filter(unit -> unit.source().isPresent())
                .toList();
        assertTrue(read.size() > 100, read.size() + " units read with their sources");
        final List<String> wrong = new ArrayList<>();
        int members = 0;
        for (final JavaUnit unit : read) {
            final Outline outline = SourceLocator.INSTANCE.outline(unit).orElseThrow();
            final List<BodyPart> parts = outline.types().stream()
                    .filter(type -> type.header().name().text().equals(unit.type().getSimpleName()))
                    .findFirst().orElseThrow().parts().items();
            oneToOne(unit.identity().name(), unit.attributes(), parts, wrong);
            for (final Entry entry : unit.attributes().entries()) {
                for (final WithSource<? extends Scalar> value : entry.all()) {
                    if (value.object() instanceof NestedClass nested) {
                        final String simpleName = nested.name().binaryName()
                                .substring(unit.identity().name().binaryName().length() + 1);
                        final List<BodyPart> nestedParts = parts.stream()
                                .flatMap(part -> part instanceof BodyPart.TypeDeclaration type
                                        && type.header().name().text().equals(simpleName)
                                        ? type.nested().stream() : Stream.empty())
                                .findFirst().orElseThrow().parts().items();
                        oneToOne(nested.name(), nested.attributes(), nestedParts, wrong);
                        members++;
                    }
                }
            }
        }
        assertTrue(members > 10, members + " member classes");
        assertEquals(List.of(), wrong);
    }

    /** Each written field, method and constructor of a class is the source of exactly one of its class file's. */
    private void oneToOne(final ClassName inClass, final Attributes attributes, final List<BodyPart> parts,
                          final List<String> wrong) {
        final Set<CodeBlock> methods = new HashSet<>();
        final Set<CodeBlock> constructors = new HashSet<>();
        int fields = 0;
        for (final Entry entry : attributes.entries()) {
            for (final WithSource<? extends Scalar> value : entry.all()) {
                final String what = inClass.binaryName() + " " + value.object();
                switch (value.source()) {
                    case ProcessedSource.OfMethod method when !methods.add(method.block()) ->
                            wrong.add(what + " shares its block with another");
                    case ProcessedSource.OfConstructor constructor when !constructors.add(constructor.block()) ->
                            wrong.add(what + " shares its block with another");
                    case ProcessedSource.OfField _ -> fields++;
                    case ProcessedSource.OfMethod _, ProcessedSource.OfConstructor _ -> {
                    }
                    case ProcessedSource<?> other when value.object() instanceof MethodValue
                            || value.object() instanceof ConstructorValue || value.object() instanceof FieldValue -> {
                        if (!(other instanceof ProcessedSource.Unwritten || other instanceof ProcessedSource.Implied)) {
                            wrong.add(what + " is " + other);
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        final long declarators = parts.stream().mapToLong(part -> part instanceof BodyPart.Field field
                ? field.declarators().items().size() : 0).sum();
        final long writtenMethods = parts.stream().filter(part -> part instanceof BodyPart.Method).count();
        final long writtenConstructors = parts.stream().filter(part -> part instanceof BodyPart.Constructor
                || part instanceof BodyPart.CompactConstructor).count();
        if (methods.size() != writtenMethods || constructors.size() != writtenConstructors || fields != declarators) {
            wrong.add(inClass.binaryName() + ": " + methods.size() + " of " + writtenMethods + " methods, "
                    + constructors.size() + " of " + writtenConstructors + " constructors and " + fields + " of "
                    + declarators + " fields are written");
        }
    }

    private JavaUnit unit(final Class<?> type) {
        return units.stream().filter(unit -> unit.type() == type).findFirst().orElseThrow();
    }

    private ClassName name(final String simpleName) {
        return new ClassName(HERE + simpleName);
    }

    /** The source of the first value of the object that passes the test, in the unit's top-level class. */
    private ProcessedSource<?> sourceOf(final Class<?> type, final ObjectKind object, final Predicate<Scalar> wanted) {
        return sourceOf(type, new ClassName(type.getName()), object, wanted);
    }

    /** The source of the first value of the object that passes the test, in a class of the unit. */
    private ProcessedSource<?> sourceOf(final Class<?> type, final ClassName inClass, final ObjectKind object,
                                        final Predicate<Scalar> wanted) {
        return attributes(unit(type).attributes(), new ClassName(type.getName()), inClass)
                .orElseThrow().entry(object).all().stream()
                .filter(value -> wanted.test(value.object()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no such " + object + " in " + inClass))
                .source();
    }

    /** The attributes of a class of the unit, nested in it at any depth. */
    private Optional<Attributes> attributes(final Attributes attributes, final ClassName name, final ClassName wanted) {
        if (name.equals(wanted)) {
            return Optional.of(attributes);
        }
        return attributes.entries().stream()
                .flatMap(entry -> entry.all().stream())
                .flatMap(value -> switch (value.object()) {
                    case NestedClass nested -> attributes(nested.attributes(), nested.name(), wanted).stream();
                    case EnclosedClass enclosed -> attributes(enclosed.attributes(), enclosed.name(), wanted).stream();
                    default -> Stream.empty();
                })
                .findFirst();
    }

    /** Every value of a class and of the classes nested in it. */
    private Stream<WithSource<? extends Scalar>> values(final Attributes attributes) {
        return attributes.entries().stream().flatMap(entry -> entry.all().stream())
                .flatMap(value -> Stream.concat(Stream.of(value), switch (value.object()) {
                    case NestedClass nested -> values(nested.attributes());
                    case EnclosedClass enclosed -> values(enclosed.attributes());
                    default -> Stream.empty();
                }));
    }

    /**
     * Every block a unit's classes are written in: where each of their values is, and how each of them - the top-level
     * class and those nested in it - is initialised.
     */
    private List<CodeBlock> blocks(final JavaUnit unit) {
        final Stream<ProcessedSource<?>> initialisations = Stream.concat(Stream.of(unit.initialisation()),
                values(unit.attributes()).flatMap(value -> switch (value.object()) {
                    case NestedClass nested -> Stream.of(nested.initialisation());
                    case EnclosedClass enclosed -> Stream.of(enclosed.initialisation());
                    default -> Stream.empty();
                })).map(WithSource::source);
        return Stream.concat(initialisations, values(unit.attributes()).<ProcessedSource<?>>map(WithSource::source))
                .flatMap(source -> switch (source) {
                    case ProcessedSource.Written<?> written -> Stream.of(written.block());
                    case ProcessedSource.OfInitialisation initialisation -> initialisation.steps().items().stream()
                            .flatMap(step -> step instanceof InitialisationStep.InitialValue initial
                                    ? Stream.of(initial.block(), initial.value()) : Stream.of(step.block()));
                    case ProcessedSource.Within<?> within -> within.blocks().items().stream();
                    default -> Stream.empty();
                }).toList();
    }

    /** How a class of a unit is initialised: its top-level class, or a class nested in it at any depth. */
    private WithSource<ClassInitialisation> initialisation(final Class<?> type, final ClassName inClass) {
        final JavaUnit unit = unit(type);
        return inClass.equals(unit.identity().name()) ? unit.initialisation() : values(unit.attributes())
                .flatMap(value -> switch (value.object()) {
                    case NestedClass nested when nested.name().equals(inClass) -> Stream.of(nested.initialisation());
                    case EnclosedClass enclosed when enclosed.name().equals(inClass) ->
                            Stream.of(enclosed.initialisation());
                    default -> Stream.empty();
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("no class " + inClass + " in " + type.getName()));
    }

    /** The steps a class of a unit is initialised in, as written. */
    private List<InitialisationStep> steps(final Class<?> type, final ClassName inClass) {
        return assertInstanceOf(ProcessedSource.OfInitialisation.class, initialisation(type, inClass).source())
                .steps().items();
    }

    /** The steps' blocks, each on a line. */
    private List<String> texts(final List<InitialisationStep> steps) {
        return steps.stream().map(step -> squashed(step.block())).toList();
    }

    private ProcessedSource.OfField field(final Class<?> type, final ObjectKind object, final String name) {
        return field(type, new ClassName(type.getName()), object, name);
    }

    private ProcessedSource.OfField field(final Class<?> type, final ClassName inClass, final ObjectKind object,
                                          final String name) {
        return assertInstanceOf(ProcessedSource.OfField.class, sourceOf(type, inClass, object, field(name)));
    }

    private ProcessedSource.OfMethod method(final Class<?> type, final ObjectKind object,
                                            final Predicate<Scalar> wanted) {
        return assertInstanceOf(ProcessedSource.OfMethod.class, sourceOf(type, object, wanted));
    }

    private ProcessedSource.OfConstructor constructor(final Class<?> type, final ObjectKind object,
                                                      final Predicate<Scalar> wanted) {
        return constructor(type, new ClassName(type.getName()), object, wanted);
    }

    private ProcessedSource.OfConstructor constructor(final Class<?> type, final ClassName inClass,
                                                      final ObjectKind object, final Predicate<Scalar> wanted) {
        return assertInstanceOf(ProcessedSource.OfConstructor.class, sourceOf(type, inClass, object, wanted));
    }

    private ProcessedSource.OfEnumConstant constant(final String name) {
        return assertInstanceOf(ProcessedSource.OfEnumConstant.class, sourceOf(PlacedEnum.class,
                EnumConstant.INSTANCE, value -> ((EnumConstantValue) value).name().equals(name)));
    }

    private Predicate<Scalar> field(final String name) {
        return value -> value instanceof FieldValue field && field.name().equals(name);
    }

    private Predicate<Scalar> overloaded(final TypeUse... parameters) {
        return takes("overloaded", parameters);
    }

    /** A method by its name and its parameters' types, as its descriptor has them. */
    private Predicate<Scalar> takes(final String name, final TypeUse... parameters) {
        return value -> value instanceof MethodValue method && method.name().equals(name)
                && method.parameters().items().equals(List.of(parameters));
    }

    /** A constructor by how many parameters its class file gives it. */
    private Predicate<Scalar> parameters(final int count) {
        return value -> ((ConstructorValue) value).parameters().items().size() == count;
    }

    private TypeUse named(final String binaryName) {
        return new TypeUse.Named(new ClassName(binaryName));
    }

    /** The block a value is written in, as written. */
    private String written(final ProcessedSource<?> source) {
        return assertInstanceOf(ProcessedSource.Written.class, source).block().text();
    }

    /** The code a value is implied by, as written. */
    private String implied(final ProcessedSource<?> source) {
        return assertInstanceOf(ProcessedSource.Implied.class, source).by().text();
    }

    /** The blocks a value is written within, each on a line. */
    private List<String> within(final ProcessedSource<?> source) {
        return assertInstanceOf(ProcessedSource.Within.class, source).blocks().items().stream()
                .map(this::squashed).toList();
    }

    /** How blocks start: each up to its opening parenthesis. */
    private List<String> heads(final List<String> blocks) {
        return blocks.stream().map(block -> block.substring(0, block.indexOf('('))).toList();
    }

    private List<String> names(final WrittenParameters parameters) {
        return parameters.items().stream().map(parameter -> parameter.name()).toList();
    }

    private List<String> texts(final CodeBlocks blocks) {
        return blocks.items().stream().map(this::squashed).toList();
    }

    private String squashed(final CodeBlock block) {
        return block.text().replaceAll("\\s+", " ");
    }
}
