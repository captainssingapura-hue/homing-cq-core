package com.homing_labs.cq.lang.java.taxonomy;

/**
 * An instance method: called on an object of the class. It is a declaration, an implementation, a default
 * implementation or a native implementation. A static method is a {@link StaticMethod}; constructors and the static
 * initialiser are kinds of their own, not methods.
 */
public sealed interface Method extends ObjectKind {

    /**
     * A method declared without a body: abstract. An abstract class declares them, an interface does, and an enum does
     * when each of its constants has a body that implements them. An annotation's elements are declarations too, with a
     * default value or without.
     */
    record Declaration() implements Method,
            MemberOf.AbstractClass<Cardinality.ZeroOrMore>, MemberOf.Interface<Cardinality.ZeroOrMore>,
            MemberOf.Enum<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

        public static final Declaration INSTANCE = new Declaration();
    }

    /**
     * An instance method with its body in the class file, other than an interface's default method.
     *
     * <p>A record always has {@code equals}, {@code hashCode} and {@code toString}, beside an accessor for each
     * component. An interface's instance implementations are its private methods.</p>
     */
    record Implementation() implements Method,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
            MemberOf.Record<Cardinality.OneOrMore> {

        public static final Implementation INSTANCE = new Implementation();
    }

    /**
     * An interface's default method: an instance method with a body, which a class implementing the interface takes
     * unless it overrides it. Only an interface has them; an annotation cannot.
     */
    record DefaultImplementation() implements Method, MemberOf.Interface<Cardinality.ZeroOrMore> {

        public static final DefaultImplementation INSTANCE = new DefaultImplementation();
    }

    /**
     * An instance native method: not abstract, and called like any other, but its body is not in the class file - the
     * JVM finds it in native code. A class, an abstract class or an enum can have them; a record, an interface or an
     * annotation cannot.
     */
    record NativeImplementation() implements Method,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Enum<Cardinality.ZeroOrMore> {

        public static final NativeImplementation INSTANCE = new NativeImplementation();
    }
}
