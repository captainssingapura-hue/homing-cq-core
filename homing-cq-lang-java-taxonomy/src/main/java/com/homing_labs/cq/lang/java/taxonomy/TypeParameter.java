package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A type parameter the class declares: the {@code T} of {@code class Box<T>}, with its bounds. The class file keeps
 * them, in the order they are declared, in the class's {@code Signature} attribute, which javac writes only for a class
 * that is generic or has a generic supertype. A class, an abstract class, an interface and a record can have them; an
 * enum and an annotation interface cannot. A generic method or constructor's own type parameters are part of it.
 */
public record TypeParameter() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Record<Cardinality.ZeroOrMore> {

    public static final TypeParameter INSTANCE = new TypeParameter();
}
