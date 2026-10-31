package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * What an import brings in (JLS 7.5). Whether a name in it is a package's or a type's is not known from the source
 * alone - {@code a.b.C.*} brings in the types of a package, or the member types of a class - and is left to the
 * resolver.
 */
public sealed interface ImportKind extends Stateless, Scalar {

    /** {@code import a.b.C;} - one type. */
    record SingleType() implements ImportKind {
        public static final SingleType INSTANCE = new SingleType();
    }

    /** {@code import a.b.*;} - the types of a package, or the member types of a type. */
    record OnDemand() implements ImportKind {
        public static final OnDemand INSTANCE = new OnDemand();
    }

    /** {@code import static a.b.C.m;} - the static members of a type that have one name. */
    record SingleStatic() implements ImportKind {
        public static final SingleStatic INSTANCE = new SingleStatic();
    }

    /** {@code import static a.b.C.*;} - every static member of a type. */
    record StaticOnDemand() implements ImportKind {
        public static final StaticOnDemand INSTANCE = new StaticOnDemand();
    }

    /** {@code import module a.b;} - the public types of every package a module exports. */
    record Module() implements ImportKind {
        public static final Module INSTANCE = new Module();
    }
}
