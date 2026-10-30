package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A constructor: a top-level member of a class, beside its class object and its instances. It sets up a new instance.
 * Only a class, an abstract class, an enum or a record has constructors, and it has at least one: when its source
 * writes none, one is implied. An abstract class's are called by its subclasses' only, an enum's are private, and a
 * record always has its canonical one.
 */
public record Constructor() implements ObjectKind,
        MemberOf.Class<Cardinality.OneOrMore>, MemberOf.AbstractClass<Cardinality.OneOrMore>,
        MemberOf.Enum<Cardinality.OneOrMore>, MemberOf.Record<Cardinality.OneOrMore> {

    public static final Constructor INSTANCE = new Constructor();
}
