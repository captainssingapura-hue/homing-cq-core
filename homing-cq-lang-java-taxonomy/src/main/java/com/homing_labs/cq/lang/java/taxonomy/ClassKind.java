package com.homing_labs.cq.lang.java.taxonomy;

/**
 * What a class is, at the top of everything it is made of: a class, an abstract class, an interface, an enum, a record
 * or an annotation. Each is told apart by its class file - its flags and its superclass - with no need of the source.
 */
public sealed interface ClassKind extends ObjectKind {

    /**
     * A concrete class: of none of the other kinds, and not abstract. The body of an enum constant is one: an anonymous
     * class extending its enum.
     */
    record Class() implements ClassKind {

        public static final Class INSTANCE = new Class();
    }

    /**
     * An abstract class: flagged abstract, and neither an interface nor an enum. It is never instantiated itself, and
     * it can declare methods without a body. An enum that declares such methods is flagged abstract too, but stays an
     * enum.
     */
    record AbstractClass() implements ClassKind {

        public static final AbstractClass INSTANCE = new AbstractClass();
    }

    /** An interface that is not an annotation: its class file is flagged an interface, and not an annotation. */
    record Interface() implements ClassKind {

        public static final Interface INSTANCE = new Interface();
    }

    /**
     * An enum: flagged an enum, with {@link java.lang.Enum} as its superclass. It is final, unless one of its constants
     * has a body; then it is sealed, and permits those bodies.
     */
    record Enum() implements ClassKind {

        public static final Enum INSTANCE = new Enum();
    }

    /**
     * A record: final, with {@link java.lang.Record} as its superclass, and its components listed in its class file.
     */
    record Record() implements ClassKind {

        public static final Record INSTANCE = new Record();
    }

    /**
     * An annotation interface: flagged both an interface and an annotation, and extending
     * {@link java.lang.annotation.Annotation}.
     */
    record Annotation() implements ClassKind {

        public static final Annotation INSTANCE = new Annotation();
    }
}
