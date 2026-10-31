package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.fixtures.ARecord;
import com.homing_labs.cq.lang.java.fixtures.AnAnnotation;
import com.homing_labs.cq.lang.java.fixtures.AnEnum;
import com.homing_labs.cq.lang.java.fixtures.AnInterface;
import com.homing_labs.cq.lang.java.fixtures.Nesting;
import com.homing_labs.cq.lang.java.fixtures.Plain;
import com.homing_labs.cq.lang.java.fixtures.Switching;
import com.homing_labs.cq.lang.java.model.Annotation;
import com.homing_labs.cq.lang.java.model.Annotations;
import com.homing_labs.cq.lang.java.model.AppliedAnnotation;
import com.homing_labs.cq.lang.java.model.Arity;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.ClassNames;
import com.homing_labs.cq.lang.java.model.ConstructorSignature;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.Element;
import com.homing_labs.cq.lang.java.model.ElementValue;
import com.homing_labs.cq.lang.java.model.ElementValues;
import com.homing_labs.cq.lang.java.model.Elements;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Enclosure;
import com.homing_labs.cq.lang.java.model.Entry;
import com.homing_labs.cq.lang.java.model.EnumConstantValue;
import com.homing_labs.cq.lang.java.model.Extensibility;
import com.homing_labs.cq.lang.java.model.FieldValue;
import com.homing_labs.cq.lang.java.model.MethodSignature;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.Mutability;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.Origin;
import com.homing_labs.cq.lang.java.model.Overridability;
import com.homing_labs.cq.lang.java.model.ParameterAnnotation;
import com.homing_labs.cq.lang.java.model.ParameterAnnotations;
import com.homing_labs.cq.lang.java.model.Parameters;
import com.homing_labs.cq.lang.java.model.Persistence;
import com.homing_labs.cq.lang.java.model.PrimitiveType;
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.Retention;
import com.homing_labs.cq.lang.java.model.Synchronisation;
import com.homing_labs.cq.lang.java.model.TypeArgument;
import com.homing_labs.cq.lang.java.model.TypeArguments;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.model.TypeParameters;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.TypeUses;
import com.homing_labs.cq.lang.java.model.Visibility;
import com.homing_labs.cq.lang.java.model.WithSource;
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
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.RetentionPolicy;
import java.lang.classfile.ClassFile;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.RecordComponentInfo;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFileAttributesTest {

    private static final ClassName NESTING = new ClassName(Nesting.class.getName());

    private static final Annotations NONE = new Annotations(List.of());

    private final Attributes nesting = ClassFileAttributes.INSTANCE.of(Nesting.class);

    @Test
    void aPlainClassHasItsSuperclassAndItsImpliedConstructorAndNothingElse() {
        final Attributes plain = ClassFileAttributes.INSTANCE.of(Plain.class);
        assertEquals(ClassKind.Class.INSTANCE, plain.kind());
        assertEquals(new Entry.One(ClassProperty.Visibility.INSTANCE,
                        new WithSource<>(Visibility.Public.INSTANCE, Optional.empty())),
                plain.entry(ClassProperty.Visibility.INSTANCE));
        assertEquals(List.of(Extensibility.Open.INSTANCE),
                values(plain, ClassProperty.Extensibility.INSTANCE, Extensibility.class));
        assertEquals(new Entry.ZeroOrOne(Superclass.INSTANCE,
                        Optional.of(new WithSource<>(named(Object.class), Optional.empty()))),
                plain.entry(Superclass.INSTANCE));
        assertEquals(List.of(new ConstructorValue(new Parameters(List.of()), Optional.empty(),
                        Visibility.Public.INSTANCE, Arity.Fixed.INSTANCE, Origin.Declared.INSTANCE,
                        new Annotations(List.of()), new ParameterAnnotations(List.of()))),
                values(plain, Constructor.INSTANCE, ConstructorValue.class));
        assertEquals(new Entry.ZeroOrOne(StaticInitialiser.INSTANCE, Optional.empty()),
                plain.entry(StaticInitialiser.INSTANCE));
        for (final ObjectKind none : List.of(StaticField.INSTANCE, Field.INSTANCE, Method.Implementation.INSTANCE,
                StaticMethod.Implementation.INSTANCE, StaticMemberClass.INSTANCE, MemberClass.INSTANCE,
                ClassAnnotation.INSTANCE, TypeParameter.INSTANCE)) {
            assertEquals(List.of(), plain.entry(none).all(), none.toString());
        }
    }

    @Test
    void aMemberClassIsStaticOrInnerAndALocalOneIsNoMember() {
        assertEquals(List.of(new ClassName(NESTING.binaryName() + "$StaticNested")),
                values(nesting, StaticMemberClass.INSTANCE, NestedClass.class).stream().map(NestedClass::name)
                        .toList());
        assertEquals(List.of(new ClassName(NESTING.binaryName() + "$Member")),
                values(nesting, MemberClass.INSTANCE, NestedClass.class).stream().map(NestedClass::name).toList());
    }

    @Test
    void aLocalOrAnonymousClassIsTheClassWhoseCodeDeclaresIt() {
        final Enclosure inMethod = new Enclosure.InMethod("localAndAnonymous", new Parameters(List.of()));
        assertEquals(List.of(new ClassName(NESTING.binaryName() + "$1Local")),
                values(nesting, LocalClass.INSTANCE, EnclosedClass.class).stream().map(EnclosedClass::name).toList());
        assertEquals(List.of(new ClassName(NESTING.binaryName() + "$1")),
                values(nesting, AnonymousClass.INSTANCE, EnclosedClass.class).stream().map(EnclosedClass::name)
                        .toList());
        assertEquals(List.of(inMethod, inMethod), Stream.of(LocalClass.INSTANCE, AnonymousClass.INSTANCE)
                .flatMap(kind -> values(nesting, kind, EnclosedClass.class).stream())
                .map(EnclosedClass::enclosure).toList());
    }

    @Test
    void whereALocalOrAnonymousClassIsDeclaredIsKeptAndADeeperOneIsNotItsEnclosingClasss() {
        final Attributes enclosing = ClassFileAttributes.INSTANCE.of(Enclosing.class);
        final String name = Enclosing.class.getName();
        assertEquals(List.of(new EnclosedClass(new ClassName(name + "$1InConstructor"),
                        new Enclosure.InConstructor(new Parameters(List.of())),
                        values(enclosing, LocalClass.INSTANCE, EnclosedClass.class).getFirst().attributes())),
                values(enclosing, LocalClass.INSTANCE, EnclosedClass.class));
        final List<EnclosedClass> anonymous = values(enclosing, AnonymousClass.INSTANCE, EnclosedClass.class);
        assertEquals(List.of(new ClassName(name + "$1")), anonymous.stream().map(EnclosedClass::name).toList(),
                "the anonymous class in the anonymous class is not this class's");
        assertEquals(Enclosure.InInitialiser.INSTANCE, anonymous.getFirst().enclosure());
        assertEquals(List.of(new ClassName(name + "$1$1")),
                values(anonymous.getFirst().attributes(), AnonymousClass.INSTANCE, EnclosedClass.class).stream()
                        .map(EnclosedClass::name).toList());
    }

    @Test
    void aClassTheCompilerGeneratedIsNoneOfTheClasss() {
        final List<Class<?>> generated = Arrays.stream(Switching.class.getNestMembers())
                .filter(Class::isSynthetic)
                .toList();
        assertEquals(1, generated.size(), "javac writes a class of its own to hold the switch map");
        assertTrue(generated.getFirst().isAnonymousClass(), "which the JDK takes for an anonymous class");
        assertEquals(List.of(new ClassName(Switching.class.getName() + "$1")),
                values(ClassFileAttributes.INSTANCE.of(Switching.class), AnonymousClass.INSTANCE, EnclosedClass.class)
                        .stream().map(EnclosedClass::name).toList(),
                "only the anonymous class the source declares");
    }

    /** A class with local and anonymous classes outside its methods, and one anonymous class in another. */
    @SuppressWarnings("unused")
    private static class Enclosing {

        final Object field = new Object() {
            final Object inner = new Object() {
            };
        };

        Enclosing() {
            class InConstructor {
            }
            new InConstructor();
        }
    }

    @Test
    void aMemberClassHoldsItsOwnAttributes() {
        final Attributes member = values(nesting, MemberClass.INSTANCE, NestedClass.class).getFirst().attributes();
        assertEquals(ClassKind.Class.INSTANCE, member.kind());
        assertEquals(List.of(new TypeUse.Named(NESTING)),
                values(member, Constructor.INSTANCE, ConstructorValue.class).getFirst().parameters().items(),
                "an inner class takes its outer instance first");
    }

    @Test
    void aMemberClassHasThePropertiesItWasDeclaredWith() {
        final Attributes hidden = ClassFileAttributes.INSTANCE.of(Hidden.class);
        assertEquals(List.of(Visibility.Private.INSTANCE),
                values(hidden, ClassProperty.Visibility.INSTANCE, Visibility.class),
                "a private class is package-private in its own flags");
        assertEquals(List.of(Extensibility.Final.INSTANCE),
                values(hidden, ClassProperty.Extensibility.INSTANCE, Extensibility.class));
        assertEquals(List.of(hidden), values(ClassFileAttributes.INSTANCE.of(ClassFileAttributesTest.class),
                StaticMemberClass.INSTANCE, NestedClass.class).stream()
                .filter(nested -> nested.name().binaryName().equals(Hidden.class.getName()))
                .map(NestedClass::attributes)
                .toList());
    }

    /** A class whose own class file cannot say it is private. */
    private static final class Hidden {
    }

    @Test
    void aSealedClassIsSealed() {
        assertEquals(List.of(Extensibility.Sealed.INSTANCE), values(ClassFileAttributes.INSTANCE.of(AnEnum.class),
                ClassProperty.Extensibility.INSTANCE, Extensibility.class), "an enum with a constant body is sealed");
    }

    @Test
    void eachModifierOfAMemberIsClassifiedByWhatItGoverns() {
        final Attributes classified = ClassFileAttributes.INSTANCE.of(Classified.class);
        assertEquals(List.of(
                        new FieldValue("constant", new TypeUse.Named(new ClassName("java.lang.String")),
                                Optional.empty(), Visibility.Protected.INSTANCE, Mutability.Final.INSTANCE,
                                Persistence.Transient.INSTANCE, Origin.Declared.INSTANCE, NONE),
                        new FieldValue("flag", new TypeUse.Primitive(PrimitiveType.Boolean.INSTANCE),
                                Optional.empty(), Visibility.Package.INSTANCE, Mutability.Volatile.INSTANCE,
                                Persistence.Persistent.INSTANCE, Origin.Declared.INSTANCE, NONE),
                        new FieldValue("count", new TypeUse.Primitive(PrimitiveType.Int.INSTANCE),
                                Optional.empty(), Visibility.Private.INSTANCE, Mutability.Mutable.INSTANCE,
                                Persistence.Persistent.INSTANCE, Origin.Declared.INSTANCE, NONE)),
                values(classified, Field.INSTANCE, FieldValue.class));
        assertEquals(List.of(
                        List.of(Overridability.Final.INSTANCE, Synchronisation.Synchronised.INSTANCE,
                                Arity.Fixed.INSTANCE),
                        List.of(Overridability.Overridable.INSTANCE, Synchronisation.Unsynchronised.INSTANCE,
                                Arity.Variable.INSTANCE)),
                values(classified, Method.Implementation.INSTANCE, MethodValue.class).stream()
                        .map(method -> List.of(method.overridability(), method.synchronisation(), method.arity()))
                        .toList());
        assertEquals(List.of(Arity.Variable.INSTANCE), values(classified, Constructor.INSTANCE, ConstructorValue.class)
                .stream().map(ConstructorValue::arity).toList());
    }

    /** A class with a member for each of the modifiers a member can have. */
    @SuppressWarnings("unused")
    private static class Classified {

        protected final transient String constant = "constant";

        volatile boolean flag;

        private int count;

        Classified(final String... names) {
            count = names.length;
        }

        final synchronized void lock() {
            count++;
        }

        void many(final int... values) {
            count += values.length;
        }
    }

    @Test
    void aClassKeepsItsAnnotationsWithTheirValuesAndWhereEachIsKept() {
        final ClassName marked = new ClassName(Marked.class.getName());
        final ClassName kept = new ClassName(Kept.class.getName());
        assertEquals(List.of(
                        new AppliedAnnotation(new Annotation(marked, new Elements(List.of(
                                new Element("number", new ElementValue.OfPrimitive(PrimitiveType.Int.INSTANCE, "1")),
                                new Element("text", new ElementValue.OfString("one")),
                                new Element("state", new ElementValue.OfEnum(
                                        new ClassName(Thread.State.class.getName()), "RUNNABLE")),
                                new Element("type", new ElementValue.OfClass(Optional.of(new TypeUse.Array(
                                        new TypeUse.Named(new ClassName(String.class.getName())))))),
                                new Element("held", new ElementValue.OfAnnotation(new Annotation(
                                        new ClassName(java.lang.annotation.Retention.class.getName()),
                                        new Elements(List.of(new Element("value", new ElementValue.OfEnum(
                                                new ClassName(RetentionPolicy.class.getName()), "RUNTIME"))))))),
                                new Element("tags", new ElementValue.OfArray(new ElementValues(List.of(
                                        new ElementValue.OfString("a"), new ElementValue.OfString("b")))))))),
                                Retention.AtRuntime.INSTANCE),
                        new AppliedAnnotation(new Annotation(kept, new Elements(List.of())),
                                Retention.InClassFile.INSTANCE)),
                values(ClassFileAttributes.INSTANCE.of(Annotated.class), ClassAnnotation.INSTANCE,
                        AppliedAnnotation.class));
    }

    @Test
    void aMemberKeepsItsAnnotationsAndThoseOnItsParameters() {
        final Attributes annotated = ClassFileAttributes.INSTANCE.of(Annotated.class);
        final AppliedAnnotation kept = new AppliedAnnotation(
                new Annotation(new ClassName(Kept.class.getName()), new Elements(List.of())),
                Retention.InClassFile.INSTANCE);
        assertEquals(List.of(new Annotations(List.of(kept))), values(annotated, Field.INSTANCE, FieldValue.class)
                .stream().map(FieldValue::annotations).toList());
        final MethodValue method = values(annotated, Method.Implementation.INSTANCE, MethodValue.class).getFirst();
        assertEquals(List.of(new ClassName(Marked.class.getName())), method.annotations().items().stream()
                .map(applied -> applied.annotation().type()).toList());
        assertEquals(new ParameterAnnotations(List.of(new ParameterAnnotation(1, kept))),
                method.parameterAnnotations());
    }

    @Test
    void anAnnotationTypeKeepsTheDefaultsOfItsElements() {
        final List<MethodValue> elements = values(ClassFileAttributes.INSTANCE.of(Marked.class),
                Method.Declaration.INSTANCE, MethodValue.class);
        assertEquals(List.of(
                        Optional.of(new ElementValue.OfPrimitive(PrimitiveType.Int.INSTANCE, "7")),
                        Optional.of(new ElementValue.OfString("")),
                        Optional.of(new ElementValue.OfEnum(new ClassName(Thread.State.class.getName()), "NEW")),
                        Optional.of(new ElementValue.OfClass(Optional.empty())),
                        Optional.of(new ElementValue.OfAnnotation(new Annotation(
                                new ClassName(java.lang.annotation.Retention.class.getName()),
                                new Elements(List.of(new Element("value", new ElementValue.OfEnum(
                                        new ClassName(RetentionPolicy.class.getName()), "CLASS"))))))),
                        Optional.of(new ElementValue.OfArray(new ElementValues(List.of())))),
                elements.stream().map(MethodValue::defaultValue).toList());
    }

    /** An annotation type with an element of each kind of value, each with a default. */
    @java.lang.annotation.Retention(RetentionPolicy.RUNTIME)
    private @interface Marked {

        int number() default 7;

        String text() default "";

        Thread.State state() default Thread.State.NEW;

        Class<?> type() default void.class;

        java.lang.annotation.Retention held() default @java.lang.annotation.Retention(RetentionPolicy.CLASS);

        String[] tags() default {};
    }

    /** An annotation kept in the class file alone. */
    @java.lang.annotation.Retention(RetentionPolicy.CLASS)
    private @interface Kept {
    }

    /** A class annotated, with an annotated field, method and parameter. */
    @SuppressWarnings("unused")
    @Marked(number = 1, text = "one", state = Thread.State.RUNNABLE, type = String[].class,
            held = @java.lang.annotation.Retention(RetentionPolicy.RUNTIME), tags = {"a", "b"})
    @Kept
    private static class Annotated {

        @Kept
        private int count;

        @Marked
        void count(final int by, @Kept final String why) {
            count += by;
        }
    }

    @Test
    void aMethodIsKeptUnderItsKind() {
        final MethodValue method = values(nesting, Method.Implementation.INSTANCE, MethodValue.class).stream()
                .filter(each -> each.name().equals("localAndAnonymous"))
                .findFirst().orElseThrow();
        assertEquals(Optional.of(new TypeUse.Named(new ClassName("java.lang.Object"))), method.result());
        assertEquals(Origin.Declared.INSTANCE, method.origin());
        assertEquals(Visibility.Public.INSTANCE, method.visibility());
    }

    @Test
    void dependenciesAreKeptByWhereTheyAreNamed() {
        assertTrue(values(nesting, Dependency.Code.INSTANCE, ClassName.class)
                .contains(new ClassName("java.util.List")));
    }

    @Test
    void everyKindOfClassIsRead() {
        assertEquals(List.of(ClassKind.Class.INSTANCE, ClassKind.Interface.INSTANCE, ClassKind.Enum.INSTANCE,
                        ClassKind.Record.INSTANCE, ClassKind.Annotation.INSTANCE),
                List.of(Plain.class, AnInterface.class, AnEnum.class, ARecord.class, AnAnnotation.class).stream()
                        .map(type -> ClassFileAttributes.INSTANCE.of(type).kind())
                        .toList());
        final Attributes anEnum = ClassFileAttributes.INSTANCE.of(AnEnum.class);
        assertEquals(List.of("values", "valueOf"),
                values(anEnum, StaticMethod.Implementation.INSTANCE, MethodValue.class).stream()
                        .map(MethodValue::name).filter(name -> !name.startsWith("$")).toList());
        assertTrue(anEnum.entry(StaticInitialiser.INSTANCE) instanceof Entry.One, "an enum has exactly one");
    }

    @Test
    void anEnumsConstantsAreAKindOfTheirOwnInTheOrderTheyAreDeclared() {
        final Attributes anEnum = ClassFileAttributes.INSTANCE.of(AnEnum.class);
        assertEquals(List.of(new EnumConstantValue("PLAIN", NONE), new EnumConstantValue("WITH_BODY", NONE)),
                values(anEnum, EnumConstant.INSTANCE, EnumConstantValue.class));
        assertEquals(List.of("$VALUES"), values(anEnum, StaticField.INSTANCE, FieldValue.class).stream()
                        .map(FieldValue::name).toList(),
                "javac's array of the constants is a static field, and the only one");
        assertEquals(Origin.Generated.INSTANCE,
                values(anEnum, StaticField.INSTANCE, FieldValue.class).getFirst().origin());
    }

    @Test
    void aRecordsComponentsAreAKindOfTheirOwnBesideTheMembersDerivedFromThem() {
        final Attributes pair = ClassFileAttributes.INSTANCE.of(Pair.class);
        final AppliedAnnotation marked = new AppliedAnnotation(
                new Annotation(new ClassName(Marked.class.getName()), new Elements(List.of())),
                Retention.AtRuntime.INSTANCE);
        assertEquals(List.of(
                        new RecordComponentValue("left", new TypeUse.Primitive(PrimitiveType.Int.INSTANCE),
                                Optional.empty(), new Annotations(List.of(marked))),
                        new RecordComponentValue("right", new TypeUse.Named(new ClassName(String.class.getName())),
                                Optional.empty(), NONE)),
                values(pair, RecordComponent.INSTANCE, RecordComponentValue.class));
        assertEquals(List.of("left", "right"), values(pair, Field.INSTANCE, FieldValue.class).stream()
                .map(FieldValue::name).toList(), "each component has its field");
        assertEquals(List.of(), values(ClassFileAttributes.INSTANCE.of(Empty.class), RecordComponent.INSTANCE,
                RecordComponentValue.class), "a record can have no components");
    }

    /** A record with an annotated component. */
    private record Pair(@Marked int left, String right) {
    }

    /** A record with no components. */
    private record Empty() {
    }

    @Test
    void aGenericClassKeepsItsTypeParametersAndItsSupertypesAsDeclared() {
        final Attributes generic = ClassFileAttributes.INSTANCE.of(Generic.class);
        assertEquals(List.of(
                        new TypeParameterValue("T", new TypeUses(List.of(
                                parameterized(Comparable.class, new TypeArgument.Super(variable("T"))),
                                named(Serializable.class)))),
                        new TypeParameterValue("E", new TypeUses(List.of(named(Exception.class))))),
                values(generic, TypeParameter.INSTANCE, TypeParameterValue.class));
        assertEquals(List.of(parameterized(AbstractList.class, exact(variable("T")))),
                values(generic, Superclass.INSTANCE, TypeUse.class));
        assertEquals(List.of(parameterized(Supplier.class,
                        exact(parameterized(List.class, new TypeArgument.Extends(variable("T")))))),
                values(generic, Superinterface.INSTANCE, TypeUse.class));
        assertEquals(List.of(parameterized(Enum.class, exact(named(AnEnum.class)))),
                values(ClassFileAttributes.INSTANCE.of(AnEnum.class), Superclass.INSTANCE, TypeUse.class),
                "a class that is not generic has a signature when a supertype is");
    }

    @Test
    void aFieldKeepsTheGenericTypeItIsDeclaredWithBesideTheTypeItRunsAs() {
        final List<FieldValue> fields = values(ClassFileAttributes.INSTANCE.of(Generic.class), Field.INSTANCE,
                FieldValue.class);
        assertEquals(List.of(
                        List.of(named(List.class), Optional.of(parameterized(List.class, exact(named(String.class))))),
                        List.of(new TypeUse.Array(named(Comparable.class)),
                                Optional.of(new TypeUse.Array(variable("T")))),
                        List.of(named(Map.class), Optional.of(parameterized(Map.class, exact(named(String.class)),
                                new TypeArgument.Super(named(Integer.class))))),
                        List.of(named(Generic.Inner.class), Optional.of(new TypeUse.Parameterized(
                                new ClassName(Generic.Inner.class.getName()),
                                new TypeArguments(List.of(exact(named(String.class)))),
                                Optional.of((TypeUse.Parameterized) parameterized(Generic.class,
                                        exact(variable("T")), exact(variable("E"))))))),
                        List.of(new TypeUse.Primitive(PrimitiveType.Int.INSTANCE), Optional.empty())),
                fields.stream().map(field -> List.of(field.type(), field.signature())).toList(),
                "a type variable runs as its first bound, and an inner class of a generic class is owned by it");
        final Attributes boxed = ClassFileAttributes.INSTANCE.of(Boxed.class);
        assertEquals(List.of(Optional.of(variable("V")), Optional.of(parameterized(List.class, exact(variable("V"))))),
                values(boxed, RecordComponent.INSTANCE, RecordComponentValue.class).stream()
                        .map(RecordComponentValue::signature).toList());
    }

    @Test
    void aMethodOrConstructorKeepsItsGenericSignatureBesideWhatItRunsAs() {
        final Attributes generic = ClassFileAttributes.INSTANCE.of(Generic.class);
        final ConstructorValue constructor = values(generic, Constructor.INSTANCE, ConstructorValue.class).getFirst();
        assertEquals(new Parameters(List.of(named(Function.class))), constructor.parameters());
        assertEquals(Optional.of(new ConstructorSignature(
                        new TypeParameters(List.of(
                                new TypeParameterValue("R", new TypeUses(List.of(named(Object.class)))))),
                        new Parameters(List.of(parameterized(Function.class, new TypeArgument.Super(variable("T")),
                                exact(variable("R"))))),
                        new TypeUses(List.of()))),
                constructor.signature());
        final MethodValue first = values(generic, Method.Implementation.INSTANCE, MethodValue.class).stream()
                .filter(method -> method.name().equals("first")).findFirst().orElseThrow();
        assertEquals(Optional.of(named(Comparable.class)), first.result());
        assertEquals(new ClassNames(List.of(new ClassName(Throwable.class.getName()),
                new ClassName(Exception.class.getName()))), first.thrown());
        assertEquals(Optional.of(new MethodSignature(
                        new TypeParameters(List.of(new TypeParameterValue("X",
                                new TypeUses(List.of(named(Throwable.class)))))),
                        new Parameters(List.of()), Optional.of(variable("T")),
                        new TypeUses(List.of(variable("X"), variable("E"))))),
                first.signature());
        final ConstructorValue inner = values(values(generic, MemberClass.INSTANCE, NestedClass.class).getFirst()
                .attributes(), Constructor.INSTANCE, ConstructorValue.class).getFirst();
        assertEquals(List.of(named(Generic.class), named(Object.class), named(List.class)),
                inner.parameters().items(), "an inner class's constructor runs with its outer instance first");
        assertEquals(List.of(variable("U"), parameterized(List.class, exact(variable("T")))),
                inner.signature().orElseThrow().parameters().items(), "its signature leaves the outer instance out");
    }

    /** A generic class, with bounded type parameters, generic supertypes, and generic members. */
    @SuppressWarnings("unused")
    private abstract static class Generic<T extends Comparable<? super T> & Serializable, E extends Exception>
            extends AbstractList<T> implements Supplier<List<? extends T>> {

        private List<String> names;

        private T[] items;

        private Map<String, ? super Integer> sink;

        private Inner<String> inner;

        private int plain;

        <R> Generic(final Function<? super T, R> mapping) {
        }

        <X extends Throwable> T first() throws X, E {
            return null;
        }

        class Inner<U> {

            Inner(final U only, final List<T> all) {
            }
        }
    }

    /** A generic record. */
    private record Boxed<V>(V value, List<V> values) {
    }

    @Test
    void aClassIsReadAsARecordExactlyWhenTheJdkTakesItForOne(@TempDir final Path classes) throws Exception {
        final ClassDesc record = ClassDesc.of("java.lang.Record");
        writeWithRecordAttribute(classes, "Final", record, ClassFile.ACC_FINAL);
        writeWithRecordAttribute(classes, "Open", record, 0);
        writeWithRecordAttribute(classes, "Abstract", record, ClassFile.ACC_ABSTRACT);
        writeWithRecordAttribute(classes, "Pretender", ConstantDescs.CD_Object, ClassFile.ACC_FINAL);
        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()},
                getClass().getClassLoader())) {
            final List<Class<?>> types = new ArrayList<>();
            for (final String simpleName : List.of("Final", "Open", "Abstract", "Pretender")) {
                types.add(Class.forName("handbuilt." + simpleName, false, loader));
            }
            assertEquals(List.of(true, false, false, false), types.stream().map(Class::isRecord).toList(),
                    "the JDK takes only the final class extending Record for a record");
            final List<Attributes> read = types.stream().map(ClassFileAttributes.INSTANCE::of).toList();
            assertEquals(List.of(ClassKind.Record.INSTANCE, ClassKind.Class.INSTANCE, ClassKind.AbstractClass.INSTANCE,
                    ClassKind.Class.INSTANCE), read.stream().map(Attributes::kind).toList());
            assertEquals(List.of(new RecordComponentValue("x", new TypeUse.Primitive(PrimitiveType.Int.INSTANCE),
                    Optional.empty(), NONE)), values(read.getFirst(), RecordComponent.INSTANCE,
                    RecordComponentValue.class));
        }
    }

    /**
     * Writes a class file javac would never write: a class with a field {@code x}, its accessor, and a {@code Record}
     * attribute naming it as a component, whatever the class's superclass and flags.
     */
    private void writeWithRecordAttribute(final Path classes, final String simpleName, final ClassDesc superclass,
                                          final int flags) throws IOException {
        final ClassDesc self = ClassDesc.of("handbuilt." + simpleName);
        final byte[] classFile = ClassFile.of().build(self, builder -> builder
                .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER | flags)
                .withSuperclass(superclass)
                .withField("x", ConstantDescs.CD_int, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL)
                .withMethodBody(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void, ClassFile.ACC_PUBLIC, code -> code
                        .aload(0).invokespecial(superclass, ConstantDescs.INIT_NAME, ConstantDescs.MTD_void).return_())
                .withMethodBody("x", MethodTypeDesc.of(ConstantDescs.CD_int), ClassFile.ACC_PUBLIC, code -> code
                        .aload(0).getfield(self, "x", ConstantDescs.CD_int).ireturn())
                .with(RecordAttribute.of(RecordComponentInfo.of("x", ConstantDescs.CD_int, List.of()))));
        final Path file = classes.resolve("handbuilt").resolve(simpleName + ".class");
        Files.createDirectories(file.getParent());
        Files.write(file, classFile);
    }

    @Test
    void theReaderHoldsToTheOntology() {
        assertEquals(List.of(), new OntologyEnforcer().enforce(ClassFileAttributes.class));
    }

    private TypeUse named(final Class<?> type) {
        return new TypeUse.Named(new ClassName(type.getName()));
    }

    private TypeUse variable(final String name) {
        return new TypeUse.Variable(name);
    }

    private TypeUse parameterized(final Class<?> type, final TypeArgument... arguments) {
        return new TypeUse.Parameterized(new ClassName(type.getName()), new TypeArguments(List.of(arguments)),
                Optional.empty());
    }

    private TypeArgument exact(final TypeUse type) {
        return new TypeArgument.Exact(type);
    }

    private <V extends hue.captains.singapura.tao.ontology.Scalar> List<V> values(final Attributes attributes,
                                                                                 final ObjectKind object,
                                                                                 final Class<V> type) {
        return attributes.all(object, type).stream().map(WithSource::object).toList();
    }
}
