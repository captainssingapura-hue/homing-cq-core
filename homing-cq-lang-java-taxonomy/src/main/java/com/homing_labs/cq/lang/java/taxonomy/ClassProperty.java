package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A property of the class itself: not a member of it, but something every class has exactly one of. What a class's
 * modifiers say is split by what each governs - who can reach the class, and who can extend it. The rest they can say
 * is said elsewhere: abstract, interface, enum, record and annotation by its {@link ClassKind}, and static, for a
 * member class, by its kind of member.
 *
 * <p>A member class's own class file does not keep its declared modifiers - a private one is package-private there,
 * and a protected one public - so they are read from the entry its {@code InnerClasses} attribute has for the class
 * itself.</p>
 */
public sealed interface ClassProperty extends ObjectKind {

    /**
     * Who can reach the class: anyone, its package and subclasses, its package, or its top-level class alone. A
     * top-level class is public or package-private.
     */
    record Visibility() implements ClassProperty,
            MemberOf.Class<Cardinality.One>, MemberOf.AbstractClass<Cardinality.One>,
            MemberOf.Interface<Cardinality.One>, MemberOf.Enum<Cardinality.One>, MemberOf.Record<Cardinality.One>,
            MemberOf.Annotation<Cardinality.One> {

        public static final Visibility INSTANCE = new Visibility();
    }

    /**
     * Who can extend the class, or implement it: no one, as a final class; only the classes it permits, as a sealed
     * one; or anyone. A record is always final, and an annotation always open; an abstract class cannot be final, nor
     * an interface. An enum is final, or sealed when a constant has a body.
     */
    record Extensibility() implements ClassProperty,
            MemberOf.Class<Cardinality.One>, MemberOf.AbstractClass<Cardinality.One>,
            MemberOf.Interface<Cardinality.One>, MemberOf.Enum<Cardinality.One>, MemberOf.Record<Cardinality.One>,
            MemberOf.Annotation<Cardinality.One> {

        public static final Extensibility INSTANCE = new Extensibility();
    }
}
