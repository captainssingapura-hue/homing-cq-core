package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A method: a member of a class object, as a static method, or of the instances, as an instance method. It is a
 * declaration, an implementation, a default implementation or a native implementation. Its constructors and its static
 * initialiser are kinds of their own, not methods.
 */
public sealed interface Method extends ObjectKind {

    /**
     * A method declared without a body: abstract, and so always an instance method. An abstract class declares them,
     * an interface does, and an enum does when each of its constants has a body that implements them. An annotation's
     * elements are declarations too, with a default value or without.
     */
    record Declaration() implements Method,
            MemberOf.AbstractClass.Instance<Cardinality.ZeroOrMore>,
            MemberOf.Interface.Instance<Cardinality.ZeroOrMore>,
            MemberOf.Enum.Instance<Cardinality.ZeroOrMore>,
            MemberOf.Annotation.Instance<Cardinality.ZeroOrMore> {

        public static final Declaration INSTANCE = new Declaration();
    }

    /**
     * A method with its body in the class file, static or instance, other than an interface's default method.
     *
     * <p>An enum's class object always has {@code values()} and {@code valueOf(String)}, and a record's instances
     * always have {@code equals}, {@code hashCode} and {@code toString}, beside an accessor for each component. An
     * interface's instance implementations are its private methods. An annotation declares none, but javac adds a
     * static one for each lambda in its fields' initialisers.</p>
     */
    record Implementation() implements Method,
            MemberOf.Class.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Class.Instance<Cardinality.ZeroOrMore>,
            MemberOf.AbstractClass.ClassObject<Cardinality.ZeroOrMore>,
            MemberOf.AbstractClass.Instance<Cardinality.ZeroOrMore>,
            MemberOf.Interface.ClassObject<Cardinality.ZeroOrMore>,
            MemberOf.Interface.Instance<Cardinality.ZeroOrMore>,
            MemberOf.Enum.ClassObject<Cardinality.OneOrMore>, MemberOf.Enum.Instance<Cardinality.ZeroOrMore>,
            MemberOf.Record.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Record.Instance<Cardinality.OneOrMore>,
            MemberOf.Annotation.ClassObject<Cardinality.ZeroOrMore> {

        public static final Implementation INSTANCE = new Implementation();
    }

    /**
     * An interface's default method: an instance method with a body, which a class implementing the interface takes
     * unless it overrides it. Only an interface has them; an annotation cannot.
     */
    record DefaultImplementation() implements Method, MemberOf.Interface.Instance<Cardinality.ZeroOrMore> {

        public static final DefaultImplementation INSTANCE = new DefaultImplementation();
    }

    /**
     * A native method: not abstract, and called like any other, but its body is not in the class file - the JVM finds
     * it in native code. A class, an abstract class or an enum can have them, static or instance; a record, an
     * interface or an annotation cannot.
     */
    record NativeImplementation() implements Method,
            MemberOf.Class.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Class.Instance<Cardinality.ZeroOrMore>,
            MemberOf.AbstractClass.ClassObject<Cardinality.ZeroOrMore>,
            MemberOf.AbstractClass.Instance<Cardinality.ZeroOrMore>,
            MemberOf.Enum.ClassObject<Cardinality.ZeroOrMore>, MemberOf.Enum.Instance<Cardinality.ZeroOrMore> {

        public static final NativeImplementation INSTANCE = new NativeImplementation();
    }
}
