package com.homing_labs.cq.lang.java.taxonomy;

/**
 * A dependency: another class a class names in its class file, by where it names it - a member of the class, beside
 * its superclass and superinterfaces. Those are kinds of their own, and dependencies only where they are named
 * again, in a signature or in code, say. A class's own name is not a dependency. One class can be a dependency several
 * times over, once for each kind of place it is named in.
 *
 * <p>Dependencies are read from the class file alone. Imports are not there: they are in the source, and only some of
 * them leave a dependency - an import used only in javadoc, for an annotation kept in the source only, or for a type
 * argument in code leaves none, while the compiler names classes the source never does.</p>
 */
public sealed interface Dependency extends ObjectKind {

    /**
     * Named in a declaration: the type of a field, of a record component, or of a method's parameters, result and
     * {@code throws}, and the classes in their generic signatures - with the class's own type parameters' bounds and
     * its supertypes' type arguments.
     */
    record Signature() implements Dependency,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
            MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

        public static final Dependency.Signature INSTANCE = new Dependency.Signature();
    }

    /**
     * Named in an annotation the class file keeps: the annotation's type, or a class or an enum among its values. An
     * annotation kept in the source only leaves none.
     */
    record Annotation() implements Dependency,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
            MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

        public static final Dependency.Annotation INSTANCE = new Dependency.Annotation();
    }

    /**
     * Named in code: a class made, cast to or tested for, a class whose members are called or read and the types in
     * their descriptors, a class constant, a caught exception's type, a type in a stack map frame, or a bootstrap
     * method's class and arguments. The compiler names many of these for itself - for string concatenation, lambdas,
     * records' methods and for-each loops, say.
     */
    record Code() implements Dependency,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
            MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

        public static final Dependency.Code INSTANCE = new Dependency.Code();
    }

    /**
     * Named in the debug tables of a method's local variables and parameters, which the class file keeps only when it
     * is compiled with {@code -g} - as Maven compiles by default. A local's generic type arguments are named nowhere
     * else.
     */
    record LocalVariable() implements Dependency,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
            MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

        public static final Dependency.LocalVariable INSTANCE = new Dependency.LocalVariable();
    }

    /**
     * Named for nesting: the host or the members of the class's nest, a nested class it names and that class's outer
     * class, or the class a local or anonymous class is declared in.
     */
    record Nesting() implements Dependency,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
            MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

        public static final Dependency.Nesting INSTANCE = new Dependency.Nesting();
    }

    /**
     * A permitted subclass of a sealed class, abstract class or interface, or of an enum whose constants have bodies. A
     * record or an annotation cannot be sealed.
     */
    record PermittedSubclass() implements Dependency,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore> {

        public static final Dependency.PermittedSubclass INSTANCE = new Dependency.PermittedSubclass();
    }

    /**
     * A class whose constant was copied into this one, and that nothing else in the class file names: javac names it
     * all the same, since JDK 9, so that tools can still see the dependency. A constant used only in an annotation's
     * value leaves none.
     */
    record InlinedConstant() implements Dependency,
            MemberOf.Class<Cardinality.ZeroOrMore>, MemberOf.AbstractClass<Cardinality.ZeroOrMore>,
            MemberOf.Interface<Cardinality.ZeroOrMore>, MemberOf.Enum<Cardinality.ZeroOrMore>,
            MemberOf.Record<Cardinality.ZeroOrMore>, MemberOf.Annotation<Cardinality.ZeroOrMore> {

        public static final Dependency.InlinedConstant INSTANCE = new Dependency.InlinedConstant();
    }
}
