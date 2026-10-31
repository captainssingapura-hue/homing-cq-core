package com.homing_labs.cq.lang.java.taxonomy;

/**
 * An anonymous class: a class declared, without a name, by an expression in the class's code that makes its one
 * object. It is no member: it belongs to the class whose code declares it, which its class file names. An enum
 * constant with a body is one, made by the enum's static initialiser.
 *
 * <p>Every kind of class can have them: even an annotation, in the initialiser of one of its fields.</p>
 */
public record AnonymousClass() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
        MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

    public static final AnonymousClass INSTANCE = new AnonymousClass();
}
