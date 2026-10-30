package com.homing_labs.cq.lang.java.taxonomy;

/**
 * The static initialiser: a top-level member of a class, beside its class object and its instances. It sets up the
 * class object, once: the static blocks and the static fields' initialisers in order, and for an enum the making of its
 * constants. A class has one at most, and an enum always has one. An interface or an annotation has one only for its
 * fields' initialisers, since it cannot declare a static block.
 */
public record StaticInitialiser() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrOne>, MemberOf.AbstractClass<Cardinality.ZeroOrOne>,
        MemberOf.Interface<Cardinality.ZeroOrOne>, MemberOf.Enum<Cardinality.One>,
        MemberOf.Record<Cardinality.ZeroOrOne>, MemberOf.Annotation<Cardinality.ZeroOrOne> {

    public static final StaticInitialiser INSTANCE = new StaticInitialiser();
}
