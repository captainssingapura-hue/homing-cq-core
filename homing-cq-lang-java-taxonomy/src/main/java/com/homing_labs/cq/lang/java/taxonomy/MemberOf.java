package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Which kinds of class a kind of object belongs to, and how many of it each has. There is one interface for each
 * {@link ClassKind}, named after it, and every member of a class belongs to the class itself: a static member and an
 * instance member are kinds of their own - {@link StaticField} and {@link Field}, say - not one kind in two places.
 * Only a class's own members are its members; an inherited one is a member of the supertype that has it. A property of
 * the class itself, a {@link ClassProperty}, belongs to it the same way.
 *
 * <p>A kind implements the interface of every kind of class it can belong to, with the {@link Cardinality} it has
 * there: a kind a class has exactly one of implements {@code MemberOf.Class<Cardinality.One>}. The compiler holds a
 * kind to one cardinality in each kind of class, since a type cannot inherit one generic interface with two arguments.
 * For the same reason this interface itself takes no cardinality.</p>
 */
public sealed interface MemberOf extends Stateless {

    /** A member of a {@link ClassKind.Class}. */
    non-sealed interface Class<C extends Cardinality> extends MemberOf {
    }

    /** A member of a {@link ClassKind.AbstractClass}. */
    non-sealed interface AbstractClass<C extends Cardinality> extends MemberOf {
    }

    /** A member of a {@link ClassKind.Interface}. */
    non-sealed interface Interface<C extends Cardinality> extends MemberOf {
    }

    /** A member of a {@link ClassKind.Enum}. */
    non-sealed interface Enum<C extends Cardinality> extends MemberOf {
    }

    /** A member of a {@link ClassKind.Record}. */
    non-sealed interface Record<C extends Cardinality> extends MemberOf {
    }

    /** A member of a {@link ClassKind.Annotation}. */
    non-sealed interface Annotation<C extends Cardinality> extends MemberOf {
    }
}
