package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A superinterface: an interface a class names as a direct supertype - one a class, an enum or a record implements, or
 * one an interface extends. With the superclass, it is the way up the inheritance tree to what a class inherits. Every
 * kind of class can have them; an annotation has exactly one, {@link java.lang.annotation.Annotation}, which its source
 * never names.
 */
public record Superinterface() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Interface<Cardinality.ZeroOrMore>,
        MemberOf.Enum<Cardinality.ZeroOrMore>, MemberOf.Record<Cardinality.ZeroOrMore>,
        MemberOf.Annotation<Cardinality.One> {

    public static final Superinterface INSTANCE = new Superinterface();
}
