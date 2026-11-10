package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A static member class: a class declared in the body of a class, of any kind, that needs no object of it. An inner
 * class is a {@link MemberClass}. Local and anonymous classes are not members: they are {@link LocalClass}es and
 * {@link AnonymousClass}es of the class whose code declares them.
 *
 * <p>An interface, an enum, a record or an annotation declared in a class is always static, and so is every member
 * class of an interface or an annotation.</p>
 */
public record StaticMemberClass() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
        MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

    public static final StaticMemberClass INSTANCE = new StaticMemberClass();
}
