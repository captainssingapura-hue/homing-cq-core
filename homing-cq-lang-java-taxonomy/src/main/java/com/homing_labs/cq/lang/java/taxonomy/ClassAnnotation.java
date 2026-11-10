package com.homing_labs.cq.lang.java.taxonomy;

/**
 * An annotation on the class itself, as its class file keeps it: one whose retention is the class file, or the runtime
 * too. An annotation kept only in the source is not in the class file at all. Annotations on a class's members are
 * part of each member; annotations on types, as {@code List<@NonNull String>} has, are not modelled yet.
 *
 * <p>Every kind of class can have them; an annotation's own, such as its {@code @Retention}, among them.</p>
 */
public record ClassAnnotation() implements ObjectKind,
        MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
        MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
        MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

    public static final ClassAnnotation INSTANCE = new ClassAnnotation();
}
