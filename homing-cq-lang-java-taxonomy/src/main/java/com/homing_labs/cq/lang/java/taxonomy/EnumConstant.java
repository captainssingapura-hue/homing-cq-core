package com.homing_labs.cq.lang.java.taxonomy;

/**
 * An enum constant: one of the enum's own objects, named in its declaration. Its class file keeps it as a public static
 * final field of the enum's type, flagged as an enum constant - which no other field is - so it is a kind of its own,
 * not a {@link StaticField}. Only an enum has them, and an enum can have none.
 *
 * <p>The constants are in the order they are declared, which is their ordinal order. A constant with a body is made as
 * an instance of an {@link AnonymousClass}; which one only the static initialiser's code says.</p>
 */
public record EnumConstant() implements ObjectKind, MemberOf.Enum<Cardinality.ZeroOrMore> {

    public static final EnumConstant INSTANCE = new EnumConstant();
}
