package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * The word a type is declared with. It is a kind of class as the source writes it, with the modifiers left out, and
 * {@link TypeKeywords} tells which kinds of class each word declares.
 */
public sealed interface TypeKeyword extends Stateless, Scalar {

    /** The word as written: {@code class}, {@code interface}, {@code enum}, {@code record} or {@code @interface}. */
    default String word() {
        return switch (this) {
            case Class _ -> "class";
            case Interface _ -> "interface";
            case Enum _ -> "enum";
            case Record _ -> "record";
            case Annotation _ -> "@interface";
        };
    }

    record Class() implements TypeKeyword {
        public static final Class INSTANCE = new Class();
    }

    record Interface() implements TypeKeyword {
        public static final Interface INSTANCE = new Interface();
    }

    record Enum() implements TypeKeyword {
        public static final Enum INSTANCE = new Enum();
    }

    record Record() implements TypeKeyword {
        public static final Record INSTANCE = new Record();
    }

    record Annotation() implements TypeKeyword {
        public static final Annotation INSTANCE = new Annotation();
    }
}
