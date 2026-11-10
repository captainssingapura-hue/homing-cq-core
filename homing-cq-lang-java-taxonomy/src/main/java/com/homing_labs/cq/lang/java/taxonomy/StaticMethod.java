package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A static method: called on the class, not on an object of it. It is an implementation or a native implementation; a
 * static method cannot be abstract, and only an instance method can be a default. An instance method is a
 * {@link Method}.
 */
public sealed interface StaticMethod extends ObjectKind {

    /**
     * A static method with its body in the class file.
     *
     * <p>An enum always has {@code values()} and {@code valueOf(String)}. An annotation declares none, but javac adds
     * one for each lambda in its fields' initialisers.</p>
     */
    record Implementation() implements StaticMethod,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.OneOrMore>,
            MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

        public static final Implementation INSTANCE = new Implementation();
    }

    /**
     * A static native method: its body is not in the class file - the JVM finds it in native code. A class, an abstract
     * class or an enum can have them; a record, an interface or an annotation cannot.
     */
    record NativeImplementation() implements StaticMethod,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Enum<Cardinality.ZeroOrMore> {

        public static final NativeImplementation INSTANCE = new NativeImplementation();
    }
}
