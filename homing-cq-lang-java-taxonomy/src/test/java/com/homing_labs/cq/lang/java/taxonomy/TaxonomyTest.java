package com.homing_labs.cq.lang.java.taxonomy;

import com.homing_labs.cq.lang.java.taxonomy.Cardinality.One;
import com.homing_labs.cq.lang.java.taxonomy.Cardinality.OneOrMore;
import com.homing_labs.cq.lang.java.taxonomy.Cardinality.ZeroOrMore;
import com.homing_labs.cq.lang.java.taxonomy.Cardinality.ZeroOrOne;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxonomyTest {

    @Test
    void theTaxonomyHoldsTheseKinds() {
        assertEquals(Set.of(ClassKind.Class.class, ClassKind.AbstractClass.class, ClassKind.Interface.class,
                        ClassKind.Enum.class, ClassKind.Record.class, ClassKind.Annotation.class,
                        ClassProperty.Visibility.class, ClassProperty.Extensibility.class, ClassAnnotation.class,
                        TypeParameter.class, Constructor.class, StaticInitialiser.class, Superclass.class,
                        Superinterface.class,
                        Dependency.Signature.class, Dependency.Annotation.class, Dependency.Code.class,
                        Dependency.LocalVariable.class, Dependency.Nesting.class, Dependency.PermittedSubclass.class,
                        Dependency.InlinedConstant.class,
                        StaticField.class, EnumConstant.class, Field.class, RecordComponent.class,
                        StaticMethod.Implementation.class, StaticMethod.NativeImplementation.class,
                        Method.Declaration.class, Method.Implementation.class,
                        Method.DefaultImplementation.class, Method.NativeImplementation.class,
                        StaticMemberClass.class, MemberClass.class, LocalClass.class, AnonymousClass.class),
                kinds(ObjectKind.class));
    }

    @Test
    void everyKindOfClassHasItsMemberOf() {
        assertEquals(simpleNames(ClassKind.class.getPermittedSubclasses()),
                simpleNames(MemberOf.class.getDeclaredClasses()));
    }

    @Test
    void theMembersOfEachKindOfClass() {
        assertEquals(withDependencies(true, Map.ofEntries(
                        entry(Constructor.class, OneOrMore.class),
                        entry(StaticInitialiser.class, ZeroOrOne.class),
                        entry(Superclass.class, ZeroOrOne.class),
                        entry(Superinterface.class, ZeroOrMore.class),
                        entry(StaticField.class, ZeroOrMore.class),
                        entry(Field.class, ZeroOrMore.class),
                        entry(StaticMethod.Implementation.class, ZeroOrMore.class),
                        entry(StaticMethod.NativeImplementation.class, ZeroOrMore.class),
                        entry(Method.Implementation.class, ZeroOrMore.class),
                        entry(Method.NativeImplementation.class, ZeroOrMore.class),
                        entry(StaticMemberClass.class, ZeroOrMore.class),
                        entry(MemberClass.class, ZeroOrMore.class),
                        entry(TypeParameter.class, ZeroOrMore.class))),
                members(MemberOf.Class.class));
        assertEquals(withDependencies(true, Map.ofEntries(
                        entry(Constructor.class, OneOrMore.class),
                        entry(StaticInitialiser.class, ZeroOrOne.class),
                        entry(Superclass.class, One.class),
                        entry(Superinterface.class, ZeroOrMore.class),
                        entry(StaticField.class, ZeroOrMore.class),
                        entry(Field.class, ZeroOrMore.class),
                        entry(StaticMethod.Implementation.class, ZeroOrMore.class),
                        entry(StaticMethod.NativeImplementation.class, ZeroOrMore.class),
                        entry(Method.Declaration.class, ZeroOrMore.class),
                        entry(Method.Implementation.class, ZeroOrMore.class),
                        entry(Method.NativeImplementation.class, ZeroOrMore.class),
                        entry(StaticMemberClass.class, ZeroOrMore.class),
                        entry(MemberClass.class, ZeroOrMore.class),
                        entry(TypeParameter.class, ZeroOrMore.class))),
                members(MemberOf.AbstractClass.class));
        assertEquals(withDependencies(true, Map.ofEntries(
                        entry(StaticInitialiser.class, ZeroOrOne.class),
                        entry(Superinterface.class, ZeroOrMore.class),
                        entry(StaticField.class, ZeroOrMore.class),
                        entry(StaticMethod.Implementation.class, ZeroOrMore.class),
                        entry(Method.Declaration.class, ZeroOrMore.class),
                        entry(Method.Implementation.class, ZeroOrMore.class),
                        entry(Method.DefaultImplementation.class, ZeroOrMore.class),
                        entry(StaticMemberClass.class, ZeroOrMore.class),
                        entry(TypeParameter.class, ZeroOrMore.class))),
                members(MemberOf.Interface.class));
        assertEquals(withDependencies(true, Map.ofEntries(
                        entry(Constructor.class, OneOrMore.class),
                        entry(StaticInitialiser.class, One.class),
                        entry(Superclass.class, One.class),
                        entry(Superinterface.class, ZeroOrMore.class),
                        entry(StaticField.class, OneOrMore.class),
                        entry(EnumConstant.class, ZeroOrMore.class),
                        entry(Field.class, ZeroOrMore.class),
                        entry(StaticMethod.Implementation.class, OneOrMore.class),
                        entry(StaticMethod.NativeImplementation.class, ZeroOrMore.class),
                        entry(Method.Declaration.class, ZeroOrMore.class),
                        entry(Method.Implementation.class, ZeroOrMore.class),
                        entry(Method.NativeImplementation.class, ZeroOrMore.class),
                        entry(StaticMemberClass.class, ZeroOrMore.class),
                        entry(MemberClass.class, ZeroOrMore.class))),
                members(MemberOf.Enum.class));
        assertEquals(withDependencies(false, Map.ofEntries(
                        entry(Constructor.class, OneOrMore.class),
                        entry(StaticInitialiser.class, ZeroOrOne.class),
                        entry(Superclass.class, One.class),
                        entry(Superinterface.class, ZeroOrMore.class),
                        entry(StaticField.class, ZeroOrMore.class),
                        entry(Field.class, ZeroOrMore.class),
                        entry(RecordComponent.class, ZeroOrMore.class),
                        entry(StaticMethod.Implementation.class, ZeroOrMore.class),
                        entry(Method.Implementation.class, OneOrMore.class),
                        entry(StaticMemberClass.class, ZeroOrMore.class),
                        entry(MemberClass.class, ZeroOrMore.class),
                        entry(TypeParameter.class, ZeroOrMore.class))),
                members(MemberOf.Record.class));
        assertEquals(withDependencies(false, Map.ofEntries(
                        entry(StaticInitialiser.class, ZeroOrOne.class),
                        entry(Superinterface.class, One.class),
                        entry(StaticField.class, ZeroOrMore.class),
                        entry(StaticMethod.Implementation.class, ZeroOrMore.class),
                        entry(Method.Declaration.class, ZeroOrMore.class),
                        entry(StaticMemberClass.class, ZeroOrMore.class))),
                members(MemberOf.Annotation.class));
    }

    /**
     * A kind of class's other members, with exactly one of each of its properties, any number of annotations, of local
     * and anonymous classes, and of each dependency - a permitted subclass too, if it can be sealed.
     */
    private Map<Class<?>, Class<?>> withDependencies(final boolean sealable, final Map<Class<?>, Class<?>> others) {
        final Map<Class<?>, Class<?>> members = new HashMap<>(others);
        members.put(ClassProperty.Visibility.class, One.class);
        members.put(ClassProperty.Extensibility.class, One.class);
        members.put(ClassAnnotation.class, ZeroOrMore.class);
        members.put(LocalClass.class, ZeroOrMore.class);
        members.put(AnonymousClass.class, ZeroOrMore.class);
        Stream.of(Dependency.Signature.class, Dependency.Annotation.class, Dependency.Code.class,
                        Dependency.LocalVariable.class, Dependency.Nesting.class, Dependency.InlinedConstant.class)
                .forEach(dependency -> members.put(dependency, ZeroOrMore.class));
        if (sealable) {
            members.put(Dependency.PermittedSubclass.class, ZeroOrMore.class);
        }
        return members;
    }

    @Test
    void theTaxonomyReadsWhatEachKindOfClassHoldsFromItsTypes() {
        for (final ClassKind kind : List.of(ClassKind.Class.INSTANCE, ClassKind.AbstractClass.INSTANCE,
                ClassKind.Interface.INSTANCE, ClassKind.Enum.INSTANCE, ClassKind.Record.INSTANCE,
                ClassKind.Annotation.INSTANCE)) {
            final Class<? extends MemberOf> place = Taxonomy.INSTANCE.place(kind);
            assertEquals(kind.getClass().getSimpleName(), place.getSimpleName());
            assertEquals(members(place), Taxonomy.INSTANCE.placements(kind).stream()
                    .collect(Collectors.toMap(placement -> placement.object().getClass(),
                            placement -> placement.cardinality().getClass())),
                    place.getName());
        }
    }

    @Test
    void theTaxonomyListsEveryObjectOnce() {
        assertEquals(kinds(ObjectKind.class), Set.copyOf(Taxonomy.INSTANCE.objects()));
        assertEquals(kinds(ObjectKind.class).size(), Taxonomy.INSTANCE.objects().size());
    }

    @Test
    void aCardinalityAllowsTheCountsItNames() {
        assertEquals(List.of(false, true, false), allows(One.INSTANCE));
        assertEquals(List.of(true, true, false), allows(ZeroOrOne.INSTANCE));
        assertEquals(List.of(false, true, true), allows(OneOrMore.INSTANCE));
        assertEquals(List.of(true, true, true), allows(ZeroOrMore.INSTANCE));
    }

    /** Whether a cardinality allows none, one and two. */
    private List<Boolean> allows(final Cardinality cardinality) {
        return List.of(cardinality.allows(0), cardinality.allows(1), cardinality.allows(2));
    }

    @Test
    void everyKindOfClassTakesACardinality() {
        for (final Class<?> place : MemberOf.class.getPermittedSubclasses()) {
            assertEquals(List.of(Cardinality.class),
                    Arrays.stream(place.getTypeParameters()).flatMap(p -> Arrays.stream(p.getBounds())).toList(),
                    place.getName());
        }
    }

    @Test
    void anyTwoInstancesOfAKindAreEqual() {
        assertEquals(ClassKind.Class.INSTANCE, new ClassKind.Class());
        assertEquals(ClassKind.AbstractClass.INSTANCE, new ClassKind.AbstractClass());
        assertEquals(ClassKind.Interface.INSTANCE, new ClassKind.Interface());
        assertEquals(ClassKind.Enum.INSTANCE, new ClassKind.Enum());
        assertEquals(ClassKind.Record.INSTANCE, new ClassKind.Record());
        assertEquals(ClassKind.Annotation.INSTANCE, new ClassKind.Annotation());
        assertEquals(ClassProperty.Visibility.INSTANCE, new ClassProperty.Visibility());
        assertEquals(ClassProperty.Extensibility.INSTANCE, new ClassProperty.Extensibility());
        assertEquals(Constructor.INSTANCE, new Constructor());
        assertEquals(StaticInitialiser.INSTANCE, new StaticInitialiser());
        assertEquals(Superclass.INSTANCE, new Superclass());
        assertEquals(Superinterface.INSTANCE, new Superinterface());
        assertEquals(Dependency.Signature.INSTANCE, new Dependency.Signature());
        assertEquals(Dependency.Annotation.INSTANCE, new Dependency.Annotation());
        assertEquals(Dependency.Code.INSTANCE, new Dependency.Code());
        assertEquals(Dependency.LocalVariable.INSTANCE, new Dependency.LocalVariable());
        assertEquals(Dependency.Nesting.INSTANCE, new Dependency.Nesting());
        assertEquals(Dependency.PermittedSubclass.INSTANCE, new Dependency.PermittedSubclass());
        assertEquals(Dependency.InlinedConstant.INSTANCE, new Dependency.InlinedConstant());
        assertEquals(StaticField.INSTANCE, new StaticField());
        assertEquals(EnumConstant.INSTANCE, new EnumConstant());
        assertEquals(RecordComponent.INSTANCE, new RecordComponent());
        assertEquals(Field.INSTANCE, new Field());
        assertEquals(StaticMethod.Implementation.INSTANCE, new StaticMethod.Implementation());
        assertEquals(StaticMethod.NativeImplementation.INSTANCE, new StaticMethod.NativeImplementation());
        assertEquals(Method.Declaration.INSTANCE, new Method.Declaration());
        assertEquals(Method.Implementation.INSTANCE, new Method.Implementation());
        assertEquals(Method.DefaultImplementation.INSTANCE, new Method.DefaultImplementation());
        assertEquals(Method.NativeImplementation.INSTANCE, new Method.NativeImplementation());
        assertEquals(StaticMemberClass.INSTANCE, new StaticMemberClass());
        assertEquals(MemberClass.INSTANCE, new MemberClass());
        assertEquals(ClassAnnotation.INSTANCE, new ClassAnnotation());
        assertEquals(TypeParameter.INSTANCE, new TypeParameter());
        assertEquals(LocalClass.INSTANCE, new LocalClass());
        assertEquals(AnonymousClass.INSTANCE, new AnonymousClass());
        assertEquals(One.INSTANCE, new One());
        assertEquals(ZeroOrOne.INSTANCE, new ZeroOrOne());
        assertEquals(OneOrMore.INSTANCE, new OneOrMore());
        assertEquals(ZeroOrMore.INSTANCE, new ZeroOrMore());
    }

    @Test
    void theTaxonomyHoldsToTheOntology() {
        final Set<Class<?>> types = new HashSet<>(hierarchy(ObjectKind.class));
        types.addAll(hierarchy(MemberOf.class));
        types.addAll(hierarchy(Cardinality.class));
        types.addAll(List.of(Placement.class, Taxonomy.class));
        final List<?> violations = types.stream()
                .flatMap(type -> new OntologyEnforcer().enforce(type).stream())
                .toList();
        assertEquals(List.of(), violations);
    }

    /** The kinds that are members of a kind of class, each with the cardinality it has there. */
    private Map<Class<?>, Class<?>> members(final Class<?> place) {
        final Map<Class<?>, Class<?>> members = new HashMap<>();
        for (final Class<?> kind : kinds(ObjectKind.class)) {
            for (final Type type : kind.getGenericInterfaces()) {
                if (type instanceof ParameterizedType parameterized && parameterized.getRawType() == place) {
                    members.put(kind, (Class<?>) parameterized.getActualTypeArguments()[0]);
                }
            }
        }
        return members;
    }

    /** The kinds a sealed type permits, and those they permit in turn, down to the ones that permit none. */
    private Set<Class<?>> kinds(final Class<?> type) {
        if (!type.isSealed()) {
            return Set.of(type);
        }
        return Arrays.stream(type.getPermittedSubclasses())
                .flatMap(permitted -> kinds(permitted).stream())
                .collect(Collectors.toSet());
    }

    /** A sealed type with every type below it. */
    private Set<Class<?>> hierarchy(final Class<?> type) {
        final Set<Class<?>> types = new HashSet<>(Set.of(type));
        if (type.isSealed()) {
            Arrays.stream(type.getPermittedSubclasses()).forEach(permitted -> types.addAll(hierarchy(permitted)));
        }
        return types;
    }

    private Set<String> simpleNames(final Class<?>[] types) {
        return Arrays.stream(types).map(Class::getSimpleName).collect(Collectors.toSet());
    }
}
