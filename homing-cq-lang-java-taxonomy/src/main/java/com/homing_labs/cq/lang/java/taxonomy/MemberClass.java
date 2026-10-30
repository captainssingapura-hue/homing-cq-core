package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A member class: a class declared in the body of a class, and a class of its own, of any kind. A static nested class
 * is a member of the class object; an inner class is a member of the instances, each of its objects made with one.
 * Local and anonymous classes are not members: they belong to the code that declares them.
 *
 * <p>An interface, an enum, a record or an annotation declared in a class is always static, and so is every member
 * class of an interface or an annotation. A class, an abstract class, an enum or a record can have inner classes as
 * well.</p>
 */
public record MemberClass() implements ObjectKind,
        MemberOf.Class.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Class.Instance<Cardinality.ZeroOrMore>,
        MemberOf.AbstractClass.ClassObject<Cardinality.ZeroOrMore>,
        MemberOf.AbstractClass.Instance<Cardinality.ZeroOrMore>,
        MemberOf.Interface.ClassObject<Cardinality.ZeroOrMore>,
        MemberOf.Enum.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Enum.Instance<Cardinality.ZeroOrMore>,
        MemberOf.Record.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Record.Instance<Cardinality.ZeroOrMore>,
        MemberOf.Annotation.ClassObject<Cardinality.ZeroOrMore> {

    public static final MemberClass INSTANCE = new MemberClass();
}
