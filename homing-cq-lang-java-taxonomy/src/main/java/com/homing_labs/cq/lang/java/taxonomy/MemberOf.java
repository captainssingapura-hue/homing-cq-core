package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Where a kind of object belongs in a kind of class, and how many of it are there. There is one interface for each
 * {@link ClassKind}, named after it, for the top-level members of that kind of class: its class object, its instances,
 * its constructors, its static initialiser, its superclass and its superinterfaces. Within it is one for each
 * {@link Subordinate}, for the members of that subordinate - a static member of an enum, say, is a member of
 * {@link MemberOf.Enum.ClassObject}. The two levels stay apart: a member of a class object is not a top-level member of
 * its class. Only a class's own members are its members; an inherited one is a member of the supertype that has it.
 *
 * <p>A kind implements the interface of every place it can belong to, with the {@link Cardinality} it has there: a kind
 * a class has exactly one of implements {@code MemberOf.Class<Cardinality.One>}. Belonging to several places takes one
 * interface each, and the compiler holds a kind to one cardinality in each place, since a type cannot inherit one
 * generic interface with two arguments. For the same reason this interface itself takes no cardinality.</p>
 */
public sealed interface MemberOf extends Stateless {

    /** A top-level member of a {@link ClassKind.Class}. */
    non-sealed interface Class<C extends Cardinality> extends MemberOf {

        /** A member of its class object: a static member. */
        non-sealed interface ClassObject<C extends Cardinality> extends MemberOf {
        }

        /** A member of its instances. */
        non-sealed interface Instance<C extends Cardinality> extends MemberOf {
        }
    }

    /** A top-level member of a {@link ClassKind.AbstractClass}. */
    non-sealed interface AbstractClass<C extends Cardinality> extends MemberOf {

        /** A member of its class object: a static member. */
        non-sealed interface ClassObject<C extends Cardinality> extends MemberOf {
        }

        /** A member of its instances: one it has of its own, not one of its subclasses. */
        non-sealed interface Instance<C extends Cardinality> extends MemberOf {
        }
    }

    /** A top-level member of a {@link ClassKind.Interface}. */
    non-sealed interface Interface<C extends Cardinality> extends MemberOf {

        /** A member of its class object: a static member. */
        non-sealed interface ClassObject<C extends Cardinality> extends MemberOf {
        }

        /** A member of its instances: one it has of its own, not one of the classes that implement it. */
        non-sealed interface Instance<C extends Cardinality> extends MemberOf {
        }
    }

    /** A top-level member of a {@link ClassKind.Enum}. */
    non-sealed interface Enum<C extends Cardinality> extends MemberOf {

        /** A member of its class object: a static member. */
        non-sealed interface ClassObject<C extends Cardinality> extends MemberOf {
        }

        /** A member of its instances: its constants. */
        non-sealed interface Instance<C extends Cardinality> extends MemberOf {
        }
    }

    /** A top-level member of a {@link ClassKind.Record}. */
    non-sealed interface Record<C extends Cardinality> extends MemberOf {

        /** A member of its class object: a static member. */
        non-sealed interface ClassObject<C extends Cardinality> extends MemberOf {
        }

        /** A member of its instances. */
        non-sealed interface Instance<C extends Cardinality> extends MemberOf {
        }
    }

    /** A top-level member of a {@link ClassKind.Annotation}. */
    non-sealed interface Annotation<C extends Cardinality> extends MemberOf {

        /** A member of its class object: a static member. */
        non-sealed interface ClassObject<C extends Cardinality> extends MemberOf {
        }

        /** A member of its instances: the objects the runtime makes for the annotation where it is used. */
        non-sealed interface Instance<C extends Cardinality> extends MemberOf {
        }
    }
}
