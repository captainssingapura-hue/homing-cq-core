package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.def.SourcePlace;
import com.homing_labs.cq.lang.java.def.SourceSpan;
import com.homing_labs.cq.lang.java.def.SourceSpans;
import com.homing_labs.cq.lang.java.fixtures.Plain;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Entry;
import com.homing_labs.cq.lang.java.model.EnumConstantValue;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.Origin;
import com.homing_labs.cq.lang.java.model.PrimitiveType;
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.Outline;
import com.homing_labs.cq.lang.java.taxonomy.AnonymousClass;
import com.homing_labs.cq.lang.java.taxonomy.ClassAnnotation;
import com.homing_labs.cq.lang.java.taxonomy.ClassProperty;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.Dependency;
import com.homing_labs.cq.lang.java.taxonomy.EnumConstant;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import com.homing_labs.cq.lang.java.taxonomy.LocalClass;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Places the objects of classes written for it - a class, an enum and a record in this module's tests - in their
 * sources, and those of the fixtures', and of this module's and the outline's own classes.
 */
class SourceLocatorTest {

    private static final String HERE = "com.homing_labs.cq.lang.java.extract.";

    private static final TypeUse INT = new TypeUse.Primitive(PrimitiveType.Int.INSTANCE);

    private final List<JavaUnit> units = JavaUnitReader.INSTANCE.read(Placed.class);

    @Test
    void aClassIsWrittenFromItsDocumentationCommentToItsClosingBrace() {
        final List<String> placed = written(Placed.class, SourceLocator.INSTANCE.ofClass(unit(Placed.class),
                outline(Placed.class), name("Placed")));
        assertEquals(1, placed.size());
        assertTrue(placed.getFirst().startsWith("/** A class with an object of each kind"), placed.getFirst());
        assertTrue(placed.getFirst().endsWith("class Deeper { int hidden; } } }"), placed.getFirst());
    }

    @Test
    void theHeaderHoldsTheClassesAnnotationsTypeParametersAndSupertypes() {
        assertEquals(List.of("@Deprecated"), written(Placed.class, placeOf(Placed.class, ClassAnnotation.INSTANCE,
                any -> true)));
        assertEquals(List.of("public"), written(Placed.class, placeOf(Placed.class,
                ClassProperty.Visibility.INSTANCE, any -> true)));
        assertEquals(List.of("T extends Comparable<T>"), written(Placed.class, placeOf(Placed.class,
                TypeParameter.INSTANCE, any -> true)));
        assertEquals(List.of("AbstractList<T>"), written(Placed.class, placeOf(Placed.class, Superclass.INSTANCE,
                any -> true)));
        assertEquals(List.of("RandomAccess"), written(Placed.class, placeOf(Placed.class, Superinterface.INSTANCE,
                any -> true)));
        assertEquals(SourcePlace.Unwritten.INSTANCE, placeOf(Placed.class, ClassProperty.Extensibility.INSTANCE,
                any -> true), "an open class says nothing of it");
    }

    @Test
    void aFieldIsItsDeclarationOrItsTypeAndItsOwnDeclaratorWhenItSharesOne() {
        assertEquals(List.of("static final int CONSTANT = 1 + 2;"), written(Placed.class,
                placeOf(Placed.class, StaticField.INSTANCE, field("CONSTANT"))), "the comment after it is left out");
        assertEquals(List.of("int", "first = 1"), written(Placed.class, placeOf(Placed.class, Field.INSTANCE,
                field("first"))));
        assertEquals(List.of("int", "second"), written(Placed.class, placeOf(Placed.class, Field.INSTANCE,
                field("second"))));
        assertEquals(List.of("private final List<T> items;"), written(Placed.class, placeOf(Placed.class,
                Field.INSTANCE, field("items"))));
    }

    @Test
    void overloadsAreToldApartByTheTypesOfTheirParameters() {
        assertEquals(List.of("void overloaded(final int value) { }"), written(Placed.class,
                placeOf(Placed.class, Method.Implementation.INSTANCE, overloaded(INT))));
        assertEquals(List.of("void overloaded(final String value) { }"), written(Placed.class,
                placeOf(Placed.class, Method.Implementation.INSTANCE, overloaded(named("java.lang.String")))));
        assertEquals(List.of("void overloaded(final Map.Entry<String, T>[] entries) { }"), written(Placed.class,
                placeOf(Placed.class, Method.Implementation.INSTANCE,
                        overloaded(new TypeUse.Array(named("java.util.Map$Entry"))))));
        assertEquals(List.of("void overloaded(final T value, final int... rest) { }"), written(Placed.class,
                placeOf(Placed.class, Method.Implementation.INSTANCE,
                        overloaded(named("java.lang.Comparable"), new TypeUse.Array(INT)))));
        assertEquals(List.of("private static int count() { return 0; }"), written(Placed.class,
                placeOf(Placed.class, StaticMethod.Implementation.INSTANCE, any -> true)));
    }

    @Test
    void aMethodTheCompilerGeneratedIsWrittenNowhere() {
        assertEquals(List.of("@Override public T get(final int index) { return items.get(index); }"),
                written(Placed.class, placeOf(Placed.class, Method.Implementation.INSTANCE,
                        value -> value instanceof MethodValue method && method.name().equals("get")
                                && method.origin() instanceof Origin.Declared)));
        assertEquals(SourcePlace.Unwritten.INSTANCE, placeOf(Placed.class, Method.Implementation.INSTANCE,
                value -> value instanceof MethodValue method && method.name().equals("get")
                        && method.origin() instanceof Origin.Generated), "javac's bridge to AbstractList.get");
    }

    @Test
    void aConstructorRunsTheInstanceInitialisersUnlessItHandsOverToAnother() {
        assertEquals(List.of("/** Hands over to the other. */ public Placed() { this(List.of()); }"),
                written(Placed.class, placeOf(Placed.class, Constructor.INSTANCE, parameters(0))));
        assertEquals(List.of("first = 1", "third = 3", "{ second = 2; }",
                        "public Placed(final List<T> items) { this.items = items; }"),
                written(Placed.class, placeOf(Placed.class, Constructor.INSTANCE, parameters(1))));
    }

    @Test
    void theStaticInitialiserIsTheStaticBlocksAndTheValuesThatAreNoConstants() {
        assertEquals(List.of("counted = count()", "BOXED = 3", "COPIED = counted", "static { counted++; }"),
                written(Placed.class, placeOf(Placed.class, StaticInitialiser.INSTANCE, any -> true)));
    }

    @Test
    void aMemberTypesBodyIsReadOneLevelDown() {
        final List<String> member = List.of("static class Member { int inside; }");
        assertEquals(member, written(Placed.class, placeOf(Placed.class, StaticMemberClass.INSTANCE, any -> true)));
        assertEquals(member, written(Placed.class, SourceLocator.INSTANCE.ofClass(unit(Placed.class),
                outline(Placed.class), name("Placed$Member"))));
        assertEquals(List.of("int inside;"), written(Placed.class, placeOf(Placed.class, name("Placed$Member"),
                Field.INSTANCE, field("inside"))));
    }

    @Test
    void anInnerClassesConstructorTakesTheEnclosingObjectFirstInTheClassFile() {
        assertEquals(List.of("Inner(final int count) { this.count = count; }"), written(Placed.class,
                placeOf(Placed.class, name("Placed$Inner"), Constructor.INSTANCE, any -> true)));
        assertEquals(List.of("made = LIMIT + count()"), written(Placed.class, placeOf(Placed.class,
                name("Placed$Inner"), StaticInitialiser.INSTANCE, any -> true)), "its own constants are left out");
    }

    @Test
    void aTypeNestedTwoLevelsDownIsWrittenThereButWhatItsBodyHoldsOnlyWithinIt() {
        final List<String> deeper = List.of("class Deeper { int hidden; }");
        assertEquals(deeper, written(Placed.class, SourceLocator.INSTANCE.ofClass(unit(Placed.class),
                outline(Placed.class), name("Placed$Inner$Deeper"))));
        assertEquals(deeper, within(Placed.class, placeOf(Placed.class, name("Placed$Inner$Deeper"),
                Field.INSTANCE, field("hidden"))));
    }

    @Test
    void aLocalClassIsWithinTheMethodWhoseCodeDeclaresIt() {
        final SourcePlace local = placeOf(Placed.class, LocalClass.INSTANCE, any -> true);
        assertEquals(1, within(Placed.class, local).size());
        assertTrue(within(Placed.class, local).getFirst().startsWith("Runnable local() { class Local"));
        assertEquals(local, SourceLocator.INSTANCE.ofClass(unit(Placed.class), outline(Placed.class),
                name("Placed$1Local")));
    }

    @Test
    void anEnumMakesItsConstantsInItsStaticInitialiserAndItsConstructorsTakeTwoParametersMore() {
        final String two = "TWO(\"two\") { @Override String label() { return \"second\"; } }";
        assertEquals(List.of("ONE"), written(PlacedEnum.class, placeOf(PlacedEnum.class, EnumConstant.INSTANCE,
                value -> value instanceof EnumConstantValue constant && constant.name().equals("ONE"))));
        assertEquals(List.of(two), written(PlacedEnum.class, placeOf(PlacedEnum.class, EnumConstant.INSTANCE,
                value -> value instanceof EnumConstantValue constant && constant.name().equals("TWO"))));
        assertEquals(List.of("ONE", two), written(PlacedEnum.class, placeOf(PlacedEnum.class,
                StaticInitialiser.INSTANCE, any -> true)));
        assertEquals(List.of("PlacedEnum() { this(\"one\"); }"), written(PlacedEnum.class,
                placeOf(PlacedEnum.class, Constructor.INSTANCE, parameters(2))));
        assertEquals(List.of("PlacedEnum(final String label) { this.label = label; }"), written(PlacedEnum.class,
                placeOf(PlacedEnum.class, Constructor.INSTANCE, parameters(3))));
        assertEquals(SourcePlace.Unwritten.INSTANCE, placeOf(PlacedEnum.class, StaticMethod.Implementation.INSTANCE,
                value -> value instanceof MethodValue method && method.name().equals("values")));
        assertEquals(List.of(two), within(PlacedEnum.class, placeOf(PlacedEnum.class, AnonymousClass.INSTANCE,
                any -> true)), "a constant's body is a class made in the static initialiser");
    }

    @Test
    void aRecordsComponentsAndCompactConstructorAreWrittenAndWhatJavacAddsIsNot() {
        assertEquals(List.of("int count"), written(PlacedRecord.class, placeOf(PlacedRecord.class,
                RecordComponent.INSTANCE, value -> ((RecordComponentValue) value).name().equals("count"))));
        assertEquals(List.of("String... names"), written(PlacedRecord.class, placeOf(PlacedRecord.class,
                RecordComponent.INSTANCE, value -> ((RecordComponentValue) value).name().equals("names"))));
        assertEquals(List.of("PlacedRecord { names = names.clone(); }"), written(PlacedRecord.class,
                placeOf(PlacedRecord.class, Constructor.INSTANCE, any -> true)));
        for (final String implied : List.of("count", "names", "equals", "hashCode", "toString")) {
            assertEquals(SourcePlace.Unwritten.INSTANCE, placeOf(PlacedRecord.class, Method.Implementation.INSTANCE,
                    value -> ((MethodValue) value).name().equals(implied)), implied);
        }
        assertEquals(SourcePlace.Unwritten.INSTANCE, placeOf(PlacedRecord.class, Field.INSTANCE,
                field("count")), "the field javac derives from the component");
        assertEquals(SourcePlace.Unwritten.INSTANCE, placeOf(PlacedRecord.class,
                ClassProperty.Extensibility.INSTANCE, any -> true), "a record is final without saying so");
    }

    @Test
    void aDependencyIsNotPlacedYet() {
        assertEquals(SourcePlace.Unplaced.INSTANCE, placeOf(Placed.class, Dependency.Signature.INSTANCE,
                any -> true));
    }

    @Test
    void aUnitThatSharesItsFileIsFoundByItsNameAndAnAnonymousClassWithinItsMethod() {
        final List<JavaUnit> fixtures = JavaUnitReader.INSTANCE.read(Plain.class);
        final JavaUnit second = fixtures.stream().filter(unit -> unit.type().getSimpleName().equals("SecondInFile"))
                .findFirst().orElseThrow();
        assertEquals(List.of("/** The second top-level class of TwoInOneFile.java: its own unit, sharing that file. */"
                        + " class SecondInFile { }"),
                texts(second, ((SourcePlace.Written) SourceLocator.INSTANCE.ofClass(second,
                        SourceLocator.INSTANCE.outline(second).orElseThrow(), second.identity().name())).spans()));
        final JavaUnit nesting = fixtures.stream().filter(unit -> unit.type().getSimpleName().equals("Nesting"))
                .findFirst().orElseThrow();
        final SourcePlace anonymous = SourceLocator.INSTANCE.ofClass(nesting,
                SourceLocator.INSTANCE.outline(nesting).orElseThrow(),
                new ClassName(nesting.identity().name().binaryName() + "$1"));
        final List<String> within = texts(nesting, ((SourcePlace.Within) anonymous).spans());
        assertEquals(1, within.size());
        assertTrue(within.getFirst().startsWith("/** Declares a local class and an anonymous class. */ public Object"
                + " localAndAnonymous()"), within.getFirst());
    }

    @Test
    void aUnitReadWithoutItsSourceHasNoOutline() {
        assertEquals(Optional.empty(), SourceLocator.INSTANCE.outline(JavaUnits.INSTANCE.of(Placed.class,
                Optional.empty())));
    }

    /**
     * Over this module's classes, its tests' and the outline's: each field, method and constructor written in a
     * top-level class, or in a member class of one, is the place of exactly one in its class file, and every other one
     * in the class file - implied or generated - is written nowhere.
     */
    @Test
    void everyMemberWrittenIsThePlaceOfExactlyOneInTheClassFile() {
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
            oneToOne(unit, outline, unit.identity().name(), unit.attributes(), parts, wrong);
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
                        oneToOne(unit, outline, nested.name(), nested.attributes(), nestedParts, wrong);
                        members++;
                    }
                }
            }
        }
        assertTrue(members > 10, members + " member classes");
        assertEquals(List.of(), wrong);
    }

    /** Each written field, method and constructor of a class is the place of exactly one of its class file's. */
    private void oneToOne(final JavaUnit unit, final Outline outline, final ClassName inClass,
                          final Attributes attributes, final List<BodyPart> parts, final List<String> wrong) {
        final Set<SourceSpan> methods = new HashSet<>();
        final Set<SourceSpans> constructors = new HashSet<>();
        int fields = 0;
        for (final Entry entry : attributes.entries()) {
            for (final WithSource<? extends Scalar> value : entry.all()) {
                final SourcePlace place = SourceLocator.INSTANCE.of(unit, outline, inClass, entry.object(),
                        value.object());
                final String what = inClass.binaryName() + " " + value.object();
                switch (value.object()) {
                    case MethodValue _ when place instanceof SourcePlace.Written written -> {
                        final List<SourceSpan> spans = written.spans().items();
                        if (spans.size() != 1 || !methods.add(spans.getFirst())) {
                            wrong.add(what + " is written in " + written.spans());
                        }
                    }
                    case ConstructorValue _ when place instanceof SourcePlace.Written written -> {
                        if (!constructors.add(written.spans())) {
                            wrong.add(what + " shares its places with another");
                        }
                    }
                    case FieldValue _ when place instanceof SourcePlace.Written -> fields++;
                    case MethodValue _, ConstructorValue _, FieldValue _ -> {
                        if (!(place instanceof SourcePlace.Unwritten)) {
                            wrong.add(what + " is " + place);
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
                    + declarators + " fields are placed");
        }
    }

    private JavaUnit unit(final Class<?> type) {
        return units.stream().filter(unit -> unit.type() == type).findFirst().orElseThrow();
    }

    private Outline outline(final Class<?> type) {
        return SourceLocator.INSTANCE.outline(unit(type)).orElseThrow();
    }

    private ClassName name(final String simpleName) {
        return new ClassName(HERE + simpleName);
    }

    /** Where the first value of the object that passes the test, in the unit's top-level class, is written. */
    private SourcePlace placeOf(final Class<?> type, final ObjectKind object, final Predicate<Scalar> wanted) {
        return placeOf(type, new ClassName(type.getName()), object, wanted);
    }

    /** Where the first value of the object that passes the test, in a class of the unit, is written. */
    private SourcePlace placeOf(final Class<?> type, final ClassName inClass, final ObjectKind object,
                                final Predicate<Scalar> wanted) {
        final Scalar value = attributes(unit(type).attributes(), new ClassName(type.getName()), inClass)
                .orElseThrow().entry(object).all().stream()
                .map(WithSource::object)
                .filter(wanted)
                .findFirst().orElseThrow(() -> new AssertionError("no such " + object + " in " + inClass));
        return SourceLocator.INSTANCE.of(unit(type), outline(type), inClass, object, value);
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

    private Predicate<Scalar> field(final String name) {
        return value -> value instanceof FieldValue field && field.name().equals(name);
    }

    private Predicate<Scalar> overloaded(final TypeUse... parameters) {
        return value -> value instanceof MethodValue method && method.name().equals("overloaded")
                && method.parameters().items().equals(List.of(parameters));
    }

    /** A constructor by how many parameters its class file gives it. */
    private Predicate<Scalar> parameters(final int count) {
        return value -> ((ConstructorValue) value).parameters().items().size() == count;
    }

    private TypeUse named(final String binaryName) {
        return new TypeUse.Named(new ClassName(binaryName));
    }

    /** The texts a place says the value is written in, each on a line. */
    private List<String> written(final Class<?> type, final SourcePlace place) {
        if (place instanceof SourcePlace.Written written) {
            return texts(unit(type), written.spans());
        }
        return fail("written nowhere exactly: " + place);
    }

    /** The texts a place says the value is written within, each on a line. */
    private List<String> within(final Class<?> type, final SourcePlace place) {
        if (place instanceof SourcePlace.Within within) {
            return texts(unit(type), within.spans());
        }
        return fail("not within: " + place);
    }

    private List<String> texts(final JavaUnit unit, final SourceSpans spans) {
        final String text = unit.source().orElseThrow().text();
        return spans.items().stream()
                .map(span -> text.substring(span.start(), span.end()).replaceAll("\\s+", " "))
                .toList();
    }
}
