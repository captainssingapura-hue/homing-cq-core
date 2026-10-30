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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaxonomyTest {

    @Test
    void theTaxonomyHoldsTheseKinds() {
        assertEquals(Set.of(ClassKind.Class.class, ClassKind.AbstractClass.class, ClassKind.Interface.class,
                        ClassKind.Enum.class, ClassKind.Record.class, ClassKind.Annotation.class,
                        Subordinate.ClassObject.class, Subordinate.Instance.class,
                        Constructor.class, StaticInitialiser.class, Superclass.class, Superinterface.class,
                        Dependency.Signature.class, Dependency.Annotation.class, Dependency.Code.class,
                        Dependency.LocalVariable.class, Dependency.Nesting.class, Dependency.PermittedSubclass.class,
                        Dependency.InlinedConstant.class,
                        Field.class, Method.Declaration.class, Method.Implementation.class,
                        Method.DefaultImplementation.class, Method.NativeImplementation.class, MemberClass.class),
                kinds(ObjectKind.class));
    }

    @Test
    void everyKindOfClassHasItsMemberOf() {
        assertEquals(simpleNames(ClassKind.class.getPermittedSubclasses()),
                simpleNames(MemberOf.class.getDeclaredClasses()));
    }

    @Test
    void everyMemberOfHasOneForEachSubordinate() {
        for (final Class<?> place : MemberOf.class.getDeclaredClasses()) {
            assertEquals(simpleNames(Subordinate.class.getPermittedSubclasses()),
                    simpleNames(place.getDeclaredClasses()), place.getName());
        }
    }

    @Test
    void aMemberOfASubordinateIsNoTopLevelMemberOfItsClass() {
        for (final Class<?> place : MemberOf.class.getDeclaredClasses()) {
            for (final Class<?> subordinate : place.getDeclaredClasses()) {
                assertFalse(place.isAssignableFrom(subordinate), subordinate.getName());
            }
        }
    }

    @Test
    void theTopLevelMembersOfEachKindOfClass() {
        assertEquals(withDependencies(true, Map.of(Subordinate.ClassObject.class, One.class,
                        Subordinate.Instance.class, One.class, StaticInitialiser.class, ZeroOrOne.class,
                        Superinterface.class, ZeroOrMore.class, Constructor.class, OneOrMore.class,
                        Superclass.class, ZeroOrOne.class)),
                members(MemberOf.Class.class));
        assertEquals(withDependencies(true, Map.of(Subordinate.ClassObject.class, One.class,
                        Subordinate.Instance.class, One.class, StaticInitialiser.class, ZeroOrOne.class,
                        Superinterface.class, ZeroOrMore.class, Constructor.class, OneOrMore.class,
                        Superclass.class, One.class)),
                members(MemberOf.AbstractClass.class));
        assertEquals(withDependencies(true, Map.of(Subordinate.ClassObject.class, One.class,
                        Subordinate.Instance.class, One.class, StaticInitialiser.class, ZeroOrOne.class,
                        Superinterface.class, ZeroOrMore.class)),
                members(MemberOf.Interface.class));
        assertEquals(withDependencies(true, Map.of(Subordinate.ClassObject.class, One.class,
                        Subordinate.Instance.class, One.class, StaticInitialiser.class, One.class,
                        Superinterface.class, ZeroOrMore.class, Constructor.class, OneOrMore.class,
                        Superclass.class, One.class)),
                members(MemberOf.Enum.class));
        assertEquals(withDependencies(false, Map.of(Subordinate.ClassObject.class, One.class,
                        Subordinate.Instance.class, One.class, StaticInitialiser.class, ZeroOrOne.class,
                        Superinterface.class, ZeroOrMore.class, Constructor.class, OneOrMore.class,
                        Superclass.class, One.class)),
                members(MemberOf.Record.class));
        assertEquals(withDependencies(false, Map.of(Subordinate.ClassObject.class, One.class,
                        Subordinate.Instance.class, One.class, StaticInitialiser.class, ZeroOrOne.class,
                        Superinterface.class, One.class)),
                members(MemberOf.Annotation.class));
    }

    /**
     * A kind of class's other top-level members, with any number of each dependency - a permitted subclass too, if it
     * can be sealed.
     */
    private Map<Class<?>, Class<?>> withDependencies(final boolean sealable, final Map<Class<?>, Class<?>> others) {
        final Map<Class<?>, Class<?>> members = new HashMap<>(others);
        Stream.of(Dependency.Signature.class, Dependency.Annotation.class, Dependency.Code.class,
                        Dependency.LocalVariable.class, Dependency.Nesting.class, Dependency.InlinedConstant.class)
                .forEach(dependency -> members.put(dependency, ZeroOrMore.class));
        if (sealable) {
            members.put(Dependency.PermittedSubclass.class, ZeroOrMore.class);
        }
        return members;
    }

    @Test
    void theMembersOfEachSubordinate() {
        final Map<Class<?>, Class<?>> anyOfEach = Map.of(Field.class, ZeroOrMore.class,
                Method.Implementation.class, ZeroOrMore.class, MemberClass.class, ZeroOrMore.class);
        final Map<Class<?>, Class<?>> withNatives = Map.of(Field.class, ZeroOrMore.class,
                Method.Implementation.class, ZeroOrMore.class, Method.NativeImplementation.class, ZeroOrMore.class,
                MemberClass.class, ZeroOrMore.class);
        final Map<Class<?>, Class<?>> withDeclarationsAndNatives = Map.of(Field.class, ZeroOrMore.class,
                Method.Declaration.class, ZeroOrMore.class, Method.Implementation.class, ZeroOrMore.class,
                Method.NativeImplementation.class, ZeroOrMore.class, MemberClass.class, ZeroOrMore.class);
        assertEquals(Map.ofEntries(
                        entry(MemberOf.Class.ClassObject.class, withNatives),
                        entry(MemberOf.Class.Instance.class, withNatives),
                        entry(MemberOf.AbstractClass.ClassObject.class, withNatives),
                        entry(MemberOf.AbstractClass.Instance.class, withDeclarationsAndNatives),
                        entry(MemberOf.Interface.ClassObject.class, anyOfEach),
                        entry(MemberOf.Interface.Instance.class, Map.of(Method.Declaration.class, ZeroOrMore.class,
                                Method.Implementation.class, ZeroOrMore.class,
                                Method.DefaultImplementation.class, ZeroOrMore.class)),
                        entry(MemberOf.Enum.ClassObject.class, Map.of(Field.class, OneOrMore.class,
                                Method.Implementation.class, OneOrMore.class,
                                Method.NativeImplementation.class, ZeroOrMore.class, MemberClass.class,
                                ZeroOrMore.class)),
                        entry(MemberOf.Enum.Instance.class, withDeclarationsAndNatives),
                        entry(MemberOf.Record.ClassObject.class, anyOfEach),
                        entry(MemberOf.Record.Instance.class, Map.of(Field.class, ZeroOrMore.class,
                                Method.Implementation.class, OneOrMore.class, MemberClass.class, ZeroOrMore.class)),
                        entry(MemberOf.Annotation.ClassObject.class, anyOfEach),
                        entry(MemberOf.Annotation.Instance.class, Map.of(Method.Declaration.class, ZeroOrMore.class))),
                Arrays.stream(MemberOf.class.getDeclaredClasses())
                        .flatMap(place -> Arrays.stream(place.getDeclaredClasses()))
                        .collect(Collectors.toMap(Function.identity(), this::members)));
    }

    @Test
    void theTaxonomyReadsWhatEachPlaceHoldsFromItsTypes() {
        for (final ClassKind kind : List.of(ClassKind.Class.INSTANCE, ClassKind.AbstractClass.INSTANCE,
                ClassKind.Interface.INSTANCE, ClassKind.Enum.INSTANCE, ClassKind.Record.INSTANCE,
                ClassKind.Annotation.INSTANCE)) {
            final Places places = Taxonomy.INSTANCE.places(kind);
            assertEquals(kind.getClass().getSimpleName(), places.top().getSimpleName());
            assertEquals(List.of(places.top(), places.top()),
                    List.of(places.classObject().getEnclosingClass(), places.instance().getEnclosingClass()));
            for (final Class<? extends MemberOf> place : places.all()) {
                assertEquals(members(place), Taxonomy.INSTANCE.placements(kind).stream()
                        .filter(placement -> placement.place() == place)
                        .collect(Collectors.toMap(Placement::object, placement -> placement.cardinality().getClass())),
                        place.getName());
            }
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
    void everyPlaceTakesACardinality() {
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
        assertEquals(Subordinate.ClassObject.INSTANCE, new Subordinate.ClassObject());
        assertEquals(Subordinate.Instance.INSTANCE, new Subordinate.Instance());
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
        assertEquals(Field.INSTANCE, new Field());
        assertEquals(Method.Declaration.INSTANCE, new Method.Declaration());
        assertEquals(Method.Implementation.INSTANCE, new Method.Implementation());
        assertEquals(Method.DefaultImplementation.INSTANCE, new Method.DefaultImplementation());
        assertEquals(Method.NativeImplementation.INSTANCE, new Method.NativeImplementation());
        assertEquals(MemberClass.INSTANCE, new MemberClass());
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
        types.addAll(List.of(Places.class, Placement.class, Taxonomy.class));
        final List<?> violations = types.stream()
                .flatMap(type -> new OntologyEnforcer().enforce(type).stream())
                .toList();
        assertEquals(List.of(), violations);
    }

    /** The kinds that belong to a place, each with the cardinality it has there. */
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
