package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A local class: a class declared, by name, in a block of the class's code - in a method, a constructor, an initialiser
 * or a lambda's body. It is no member: it belongs to the class whose code declares it, which its class file names. It
 * can be a class, an abstract class, a record, an enum or an interface; the last three are implicitly static.
 *
 * <p>Every kind of class can have them: even an annotation, in a lambda that initialises one of its fields.</p>
 */
public record LocalClass() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
        MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

    public static final LocalClass INSTANCE = new LocalClass();
}
