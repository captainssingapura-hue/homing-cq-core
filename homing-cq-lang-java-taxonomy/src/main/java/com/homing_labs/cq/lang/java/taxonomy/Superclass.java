package com.homing_labs.cq.lang.java.taxonomy;

/**
 * The superclass: the one class a class extends directly, and with its superinterfaces the way up the inheritance tree
 * to what it inherits. A class, an abstract class, an enum or a record has one, even when its source names none: a
 * class's is then {@link java.lang.Object}, an enum's is {@link java.lang.Enum} - or its enum, for the body of a
 * constant - and a record's is {@link java.lang.Record}. Only {@link java.lang.Object} itself, a class, has none. An
 * interface or an annotation has none either; its class file names {@link java.lang.Object} only because the JVM asks
 * for one. It is a type, with any type arguments the class gives it: {@code extends AbstractList<T>}.
 */
public record Superclass() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrOne>, MemberOf.AbstractClass<Cardinality.One>,
        MemberOf.Enum<Cardinality.One>, MemberOf.Record<Cardinality.One> {

    public static final Superclass INSTANCE = new Superclass();
}
