package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The taxonomy read from its own types: the objects it declares, where each kind of class keeps its members, and what
 * each of those places holds, with how many. The types are the one statement of it; this only reads them.
 */
public record Taxonomy() implements StatelessFunctionalObject {

    public static final Taxonomy INSTANCE = new Taxonomy();

    /** Every object of the taxonomy, in the order it declares them. */
    public List<Class<? extends ObjectKind>> objects() {
        return leaves(ObjectKind.class).stream()
                .<Class<? extends ObjectKind>>map(object -> object.asSubclass(ObjectKind.class))
                .toList();
    }

    /** Where a kind of class keeps its members. */
    public Places places(final ClassKind kind) {
        return switch (kind) {
            case ClassKind.Class _ -> new Places(MemberOf.Class.class,
                    MemberOf.Class.ClassObject.class, MemberOf.Class.Instance.class);
            case ClassKind.AbstractClass _ -> new Places(MemberOf.AbstractClass.class,
                    MemberOf.AbstractClass.ClassObject.class, MemberOf.AbstractClass.Instance.class);
            case ClassKind.Interface _ -> new Places(MemberOf.Interface.class,
                    MemberOf.Interface.ClassObject.class, MemberOf.Interface.Instance.class);
            case ClassKind.Enum _ -> new Places(MemberOf.Enum.class,
                    MemberOf.Enum.ClassObject.class, MemberOf.Enum.Instance.class);
            case ClassKind.Record _ -> new Places(MemberOf.Record.class,
                    MemberOf.Record.ClassObject.class, MemberOf.Record.Instance.class);
            case ClassKind.Annotation _ -> new Places(MemberOf.Annotation.class,
                    MemberOf.Annotation.ClassObject.class, MemberOf.Annotation.Instance.class);
        };
    }

    /**
     * Every object a class of this kind can have, in each place it can have it, with how many: its top-level members
     * first, then its class object's, then its instances', each place's objects in the order the taxonomy declares
     * them.
     */
    public List<Placement> placements(final ClassKind kind) {
        final List<Class<? extends ObjectKind>> objects = objects();
        return places(kind).all().stream()
                .flatMap(place -> objects.stream().flatMap(object -> cardinality(object, place)
                        .map(cardinality -> new Placement(place, object, cardinality)).stream()))
                .toList();
    }

    /** How many of an object a place has, if the object can be there at all. */
    private Optional<Cardinality> cardinality(final Class<?> object, final Class<?> place) {
        return Arrays.stream(object.getGenericInterfaces())
                .flatMap(type -> type instanceof ParameterizedType generic && generic.getRawType() == place
                        ? Stream.of((Class<?>) generic.getActualTypeArguments()[0]) : Stream.empty())
                .map(this::cardinality)
                .findFirst();
    }

    private Cardinality cardinality(final Class<?> type) {
        try {
            return type.asSubclass(Cardinality.class).getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(type.getName() + " is not a cardinality the taxonomy can make", e);
        }
    }

    /** The types a sealed type permits, and those they permit in turn, in order, down to the ones that permit none. */
    private List<Class<?>> leaves(final Class<?> type) {
        if (!type.isSealed()) {
            return List.of(type);
        }
        return Arrays.stream(type.getPermittedSubclasses()).flatMap(permitted -> leaves(permitted).stream()).toList();
    }
}
