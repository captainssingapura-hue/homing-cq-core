package com.homing_labs.cq.lang.java.taxonomy;

/**
 * An instance field: one in each object of the class. A static field is a {@link StaticField}.
 *
 * <p>An interface or an annotation has none. A record's instance fields are its components', one for each, and it can
 * declare no other.</p>
 */
public record Field() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Enum<Cardinality.ZeroOrMore>, MemberOf.Record<Cardinality.ZeroOrMore> {

    public static final Field INSTANCE = new Field();
}
