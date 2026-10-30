package com.homing_labs.cq.demo.taxonomy;

import java.util.function.Supplier;

/**
 * An annotation with every object of the taxonomy an annotation can have:
 * <ul>
 *     <li>superinterface: {@link java.lang.annotation.Annotation}, which its source does not name</li>
 *     <li>static initialiser: for {@code NAMER}, whose initialiser is not a constant</li>
 *     <li>class object - fields: {@code DEFAULT_LEVEL} and {@code NAMER}; implementation: the one javac generates for
 *     the lambda in {@code NAMER}; member class: {@code Kind}</li>
 *     <li>instances - declarations: the elements {@code value}, and {@code level}, with a default</li>
 * </ul>
 */
public @interface SampleAnnotation {

    int DEFAULT_LEVEL = 1;

    Supplier<String> NAMER = () -> "sample";

    String value();

    int level() default DEFAULT_LEVEL;

    enum Kind {
        PLAIN, STRICT
    }
}
