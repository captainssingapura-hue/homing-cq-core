package com.homing_labs.cq.lang.java.taxonomy;

/**
 * The two subordinates every class has, as top-level members beside its constructors and its static initialiser: its
 * class object, one for the class, and its instances, one for each object of it. A static member is a member of the
 * class object; every other member is a member of the instances. Either holds only the class's own members: an
 * inherited one is a member of the superclass's or interface's subordinate. A class has exactly one of each.
 */
public sealed interface Subordinate extends ObjectKind {

    /**
     * The class object: one for the class. It holds the class's static fields, its static methods are called on it,
     * its static nested classes belong to it, and its static initialiser sets it up, once.
     */
    record ClassObject() implements Subordinate,
            MemberOf.Class<Cardinality.One>, MemberOf.AbstractClass<Cardinality.One>,
            MemberOf.Interface<Cardinality.One>, MemberOf.Enum<Cardinality.One>, MemberOf.Record<Cardinality.One>,
            MemberOf.Annotation<Cardinality.One> {

        public static final Subordinate.ClassObject INSTANCE = new Subordinate.ClassObject();
    }

    /**
     * An instance: one for each object of the class. It holds the class's instance fields, its instance methods are
     * called on it, its inner classes are made with it, and one of its constructors sets it up. An enum's instances are
     * its constants. An object is an instance of each of its class's supertypes too, and holds their members as theirs.
     */
    record Instance() implements Subordinate,
            MemberOf.Class<Cardinality.One>, MemberOf.AbstractClass<Cardinality.One>,
            MemberOf.Interface<Cardinality.One>, MemberOf.Enum<Cardinality.One>, MemberOf.Record<Cardinality.One>,
            MemberOf.Annotation<Cardinality.One> {

        public static final Subordinate.Instance INSTANCE = new Subordinate.Instance();
    }
}
