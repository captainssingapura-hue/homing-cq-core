package com.homing_labs.cq.demo.taxonomy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.util.function.Function;

/**
 * An annotation with every object of the taxonomy an annotation can have:
 * <ul>
 *     <li>superinterface: {@link java.lang.annotation.Annotation}, which its source does not name</li>
 *     <li>static initialiser: for {@code NAMER}, whose initialiser is not a constant</li>
 *     <li>class object - fields: {@code DEFAULT_LEVEL}, {@code OK} and {@code NAMER}; implementation: the one javac
 *     generates for the lambda in {@code NAMER}; member class: {@code Kind}</li>
 *     <li>instances - declarations: the elements {@code value}, and {@code level}, with a default</li>
 *     <li>dependencies - in signatures, an annotation ({@link Retention}), in code, in local variables (the lambda's
 *     parameter), in nesting and an inlined constant ({@link HttpURLConnection#HTTP_OK}); an annotation cannot be
 *     sealed</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SampleAnnotation {

    int DEFAULT_LEVEL = 1;

    int OK = HttpURLConnection.HTTP_OK;

    Function<String, String> NAMER = name -> name.trim();

    String value();

    int level() default DEFAULT_LEVEL;

    enum Kind {
        PLAIN, STRICT
    }
}
