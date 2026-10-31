package com.homing_labs.cq.lang.java.taxonomy;

/**
 * An inner class: a member class that is not static, each of its objects made with an object of the class that
 * declares it. It is always a class or an abstract class. A static member class is a {@link StaticMemberClass}.
 *
 * <p>A class, an abstract class, an enum or a record can have inner classes; an interface or an annotation cannot.</p>
 */
public record MemberClass() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Enum<Cardinality.ZeroOrMore>, MemberOf.Record<Cardinality.ZeroOrMore> {

    public static final MemberClass INSTANCE = new MemberClass();
}
