package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A record component: one of the values a record is made of, named in its header. It is no member: the class file
 * keeps the components in a {@code Record} attribute of their own, in the order they are declared, and javac derives
 * members from each - a private final {@link Field}, a public accessor {@link Method.Implementation}, and a parameter
 * of the canonical {@link Constructor}. Only a record has them, and a record can have none. A {@code Record} attribute
 * on a class that is no record names no components: the JDK ignores it.
 *
 * <p>An annotation on a component is copied to what javac derives from it, as far as the annotation's targets allow,
 * so it can be found on the component, its field, its accessor and its constructor parameter.</p>
 */
public record RecordComponent() implements ObjectKind, MemberOf.Record<Cardinality.ZeroOrMore> {

    public static final RecordComponent INSTANCE = new RecordComponent();
}
