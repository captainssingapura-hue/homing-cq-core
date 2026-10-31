package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A static field: one for the class, not for each of its objects. An enum's constants are not static fields here, but
 * {@link EnumConstant}s.
 *
 * <p>An interface's or an annotation's fields are all static: its constants. javac adds a {@code $VALUES} array to
 * every enum, beside its constants, so an enum has one static field at least.</p>
 */
public record StaticField() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.OneOrMore>,
        MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

    public static final StaticField INSTANCE = new StaticField();
}
