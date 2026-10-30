package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A field: a member of a class object, as a static field, or of the instances, as an instance field.
 *
 * <p>An interface's or an annotation's fields are all static: its constants. An enum's constants are static fields,
 * and javac adds the {@code $VALUES} array beside them, so an enum's class object has one at least. A record's instance
 * fields are its components', one for each, and it can declare no other.</p>
 */
public record Field() implements ObjectKind,
        MemberOf.Class.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Class.Instance<Cardinality.ZeroOrMore>,
        MemberOf.AbstractClass.ClassObject<Cardinality.ZeroOrMore>,
        MemberOf.AbstractClass.Instance<Cardinality.ZeroOrMore>,
        MemberOf.Interface.ClassObject<Cardinality.ZeroOrMore>,
        MemberOf.Enum.ClassObject<Cardinality.OneOrMore>, MemberOf.Enum.Instance<Cardinality.ZeroOrMore>,
        MemberOf.Record.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Record.Instance<Cardinality.ZeroOrMore>,
        MemberOf.Annotation.ClassObject<Cardinality.ZeroOrMore> {

    public static final Field INSTANCE = new Field();
}
