package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The taxonomy read from its own types: the objects it declares, and which of them each kind of class can have, with
 * how many. The types are the one statement of it; this only reads them.
 */
public record Taxonomy() implements StatelessFunctionalObject {

    public static final Taxonomy INSTANCE = new Taxonomy();

    /** Every object of the taxonomy, in the order it declares them. */
    public List<Class<? extends ObjectKind>> objects() {
        return leaves(ObjectKind.class).stream()
                .<Class<? extends ObjectKind>>map(object -> object.asSubclass(ObjectKind.class))
                .toList();
    }

    /** The interface an object implements to be a member of a kind of class. */
    public Class<? extends MemberOf> place(final ClassKind kind) {
        return switch (kind) {
            case ClassKind.Class _ -> MemberOf.Class.class;
            case ClassKind.AbstractClass _ -> MemberOf.AbstractClass.class;
            case ClassKind.Interface _ -> MemberOf.Interface.class;
            case ClassKind.Enum _ -> MemberOf.Enum.class;
            case ClassKind.Record _ -> MemberOf.Record.class;
            case ClassKind.Annotation _ -> MemberOf.Annotation.class;
        };
    }

    /** Every object a class of this kind can have, with how many, in the order the taxonomy declares them. */
    public List<Placement> placements(final ClassKind kind) {
        final Class<? extends MemberOf> place = place(kind);
        return objects().stream()
                .flatMap(object -> cardinality(object, place)
                        .map(cardinality -> new Placement(make(object, ObjectKind.class), cardinality)).stream())
                .toList();
    }

    /** How many of an object a kind of class has, if the object can be a member of it at all. */
    private Optional<Cardinality> cardinality(final Class<?> object, final Class<?> place) {
        return Arrays.stream(object.getGenericInterfaces())
                .flatMap(type -> type instanceof ParameterizedType generic && generic.getRawType() == place
                        ? Stream.of((Class<?>) generic.getActualTypeArguments()[0]) : Stream.empty())
                .map(type -> make(type, Cardinality.class))
                .findFirst();
    }

    /** An instance of a stateless type of the taxonomy: any one is as good as another. */
    private <T> T make(final Class<?> type, final Class<T> of) {
        try {
            return type.asSubclass(of).getDeclaredConstructor().newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(type.getName() + " is not a " + of.getSimpleName()
                    + " the taxonomy can make", e);
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
