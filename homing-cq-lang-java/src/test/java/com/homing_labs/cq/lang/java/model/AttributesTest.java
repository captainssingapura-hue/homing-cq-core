package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.extract.ClassFileAttributes;
import com.homing_labs.cq.lang.java.fixtures.Plain;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Placement;
import com.homing_labs.cq.lang.java.taxonomy.StaticInitialiser;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.taxonomy.Taxonomy;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributesTest {

    private static final WithSource<? extends Scalar> OBJECT =
            new WithSource<>(new ClassName("java.lang.Object"), new ProcessedSource.Unavailable<>());

    private static final WithSource<? extends Scalar> OBJECT_TYPE =
            new WithSource<>(new TypeUse.Named(new ClassName("java.lang.Object")), new ProcessedSource.Unavailable<>());

    private static final Annotations NONE = new Annotations(List.of());

    private static final ParameterAnnotations NONE_ON_PARAMETERS = new ParameterAnnotations(List.of());

    private static final WithSource<? extends Scalar> CONSTRUCTOR = new WithSource<>(new ConstructorValue(
            new Parameters(List.of()), new ClassNames(List.of()), Optional.empty(), Visibility.Public.INSTANCE,
            Arity.Fixed.INSTANCE, Origin.Declared.INSTANCE, NONE, NONE_ON_PARAMETERS),
            new ProcessedSource.Unavailable<>());

    /** A value of each type an object of the taxonomy can hold, but a nested class. */
    private static final Map<Class<? extends Scalar>, Scalar> SAMPLES = Map.of(
            ClassName.class, OBJECT.object(),
            TypeUse.class, OBJECT_TYPE.object(),
            Visibility.class, Visibility.Public.INSTANCE,
            Extensibility.class, Extensibility.Open.INSTANCE,
            ConstructorValue.class, CONSTRUCTOR.object(),
            InitialiserValue.class, new InitialiserValue(Origin.Declared.INSTANCE),
            FieldValue.class, new FieldValue("field", new TypeUse.Named(new ClassName("java.lang.Object")),
                    Optional.empty(), Visibility.Public.INSTANCE, Mutability.Mutable.INSTANCE,
                    Persistence.Persistent.INSTANCE, Origin.Declared.INSTANCE, NONE),
            MethodValue.class, new MethodValue("method", new Parameters(List.of()), Optional.empty(),
                    new ClassNames(List.of()), Optional.empty(), Visibility.Public.INSTANCE,
                    Overridability.Overridable.INSTANCE, Synchronisation.Unsynchronised.INSTANCE, Arity.Fixed.INSTANCE,
                    Origin.Declared.INSTANCE, NONE, NONE_ON_PARAMETERS, Optional.empty()));

    private final Attributes plain = ClassFileAttributes.INSTANCE.of(Plain.class);

    @Test
    void theAttributesHaveAnEntryForEachObjectTheTaxonomyGivesTheirKindInItsOrder() {
        for (final ClassKind kind : List.of(ClassKind.Class.INSTANCE, ClassKind.Interface.INSTANCE,
                ClassKind.Annotation.INSTANCE)) {
            final Attributes attributes = Entries.INSTANCE.of(kind, required(kind));
            assertEquals(Taxonomy.INSTANCE.placements(kind).stream().map(Placement::object).toList(),
                    attributes.entries().stream().map(Entry::object).toList());
        }
    }

    @Test
    void eachEntryIsShapedByItsObjectsCardinality() {
        assertEquals(Entry.ZeroOrOne.class, plain.entry(Superclass.INSTANCE).getClass());
        assertEquals(Entry.Many.class, plain.entry(Constructor.INSTANCE).getClass());
        final Attributes anEnum = Entries.INSTANCE.of(ClassKind.Enum.INSTANCE, required(ClassKind.Enum.INSTANCE));
        assertEquals(Entry.One.class, anEnum.entry(Superclass.INSTANCE).getClass());
    }

    @Test
    void tooFewOrTooManyOfAnObjectAreRefused() {
        final IllegalArgumentException none = assertThrows(IllegalArgumentException.class,
                () -> Entries.INSTANCE.of(ClassKind.Class.INSTANCE, Map.of()));
        assertEquals("A class has exactly one ClassProperty.Visibility, not 0", none.getMessage());
        final Map<ObjectKind, List<WithSource<? extends Scalar>>> twoSuperclasses = new LinkedHashMap<>(
                required(ClassKind.Class.INSTANCE));
        twoSuperclasses.put(Superclass.INSTANCE, List.of(OBJECT_TYPE, OBJECT_TYPE));
        final IllegalArgumentException two = assertThrows(IllegalArgumentException.class,
                () -> Entries.INSTANCE.of(ClassKind.Class.INSTANCE, twoSuperclasses));
        assertEquals("A class has at most one Superclass, not 2", two.getMessage());
    }

    @Test
    void anObjectTheKindCannotHaveIsRefused() {
        final Map<ObjectKind, List<WithSource<? extends Scalar>>> found = new LinkedHashMap<>(
                required(ClassKind.Interface.INSTANCE));
        found.put(Constructor.INSTANCE, List.of(CONSTRUCTOR));
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> Entries.INSTANCE.of(ClassKind.Interface.INSTANCE, found));
        assertEquals("An interface has no Constructor", refused.getMessage());
        assertThrows(IllegalArgumentException.class, () -> plain.entry(com.homing_labs.cq.lang.java.taxonomy
                .Method.DefaultImplementation.INSTANCE));
    }

    @Test
    void aValueOfAnotherTypeIsRefused() {
        final Map<ObjectKind, Entry> entries = new LinkedHashMap<>(plain.byObject());
        entries.put(Field.INSTANCE, new Entry.Many(Field.INSTANCE, new Values(List.of(OBJECT))));
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new Attributes(ClassKind.Class.INSTANCE, entries));
        assertEquals("Field holds FieldValue, not ClassName", refused.getMessage());
        assertThrows(IllegalArgumentException.class, () -> plain.all(Field.INSTANCE, ClassName.class));
    }

    @Test
    void anEntryMissingOrShapedAgainstItsCardinalityIsRefused() {
        final Map<ObjectKind, Entry> missing = new LinkedHashMap<>(plain.byObject());
        missing.remove(StaticInitialiser.INSTANCE);
        assertEquals("The attributes of a class have no entry for StaticInitialiser",
                assertThrows(IllegalArgumentException.class,
                        () -> new Attributes(ClassKind.Class.INSTANCE, missing)).getMessage());
        final Map<ObjectKind, Entry> misshaped = new LinkedHashMap<>(plain.byObject());
        misshaped.put(Superclass.INSTANCE, new Entry.Many(Superclass.INSTANCE, new Values(List.of(OBJECT_TYPE))));
        assertEquals("The entry for Superclass is Many, but a class has at most one Superclass",
                assertThrows(IllegalArgumentException.class,
                        () -> new Attributes(ClassKind.Class.INSTANCE, misshaped)).getMessage());
    }

    @Test
    void theHolderHoldsToTheOntology() {
        for (final Class<?> type : List.of(Attributes.class, Entry.One.class, Entry.ZeroOrOne.class,
                Entry.Many.class, Values.class, Entries.class, ValueTypes.class, NestedClass.class, FieldValue.class,
                MethodValue.class, ConstructorValue.class, InitialiserValue.class, WithSource.class,
                EnclosedClass.class, Enclosure.InMethod.class, Enclosure.InConstructor.class,
                Enclosure.InInitialiser.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }

    /** The least a class of a kind must have: one of each object it has exactly one, or at least one, of. */
    private Map<ObjectKind, List<WithSource<? extends Scalar>>> required(final ClassKind kind) {
        final Map<ObjectKind, List<WithSource<? extends Scalar>>> found = new LinkedHashMap<>();
        for (final Placement placement : Taxonomy.INSTANCE.placements(kind)) {
            if (!placement.cardinality().allows(0)) {
                found.put(placement.object(), List.of(new WithSource<>(
                        SAMPLES.get(ValueTypes.INSTANCE.of(placement.object())), new ProcessedSource.Unavailable<>())));
            }
        }
        return found;
    }
}
