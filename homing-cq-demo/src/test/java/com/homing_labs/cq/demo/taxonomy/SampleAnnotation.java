package com.homing_labs.cq.demo.taxonomy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.util.function.Function;

/**
 * An annotation with every object of the taxonomy an annotation can have:
 * <ul>
 *     <li>superinterface: {@link java.lang.annotation.Annotation}, which its source does not name</li>
 *     <li>class object - fields: {@code DEFAULT_LEVEL}, {@code OK} and {@code NAMER}; implementation: the one javac
 *     generates for the lambda in {@code NAMER}; member class: {@code Kind}</li>
 *     <li>instances - declarations: the elements {@code value}, and {@code level}, with a default</li>
 *     <li>in its code - local class: {@code Trimmer}; anonymous class: a {@link Function}; both in the lambda that
 *     initialises {@code NAMER}</li>
 *     <li>dependencies - in signatures, an annotation ({@link Retention}), in code, in local variables (the lambda's
 *     parameter), in nesting and an inlined constant ({@link HttpURLConnection#HTTP_OK}); an annotation cannot be
 *     sealed</li>
 * </ul>
 * <p>Beside them, and no object of the taxonomy, how it is initialised: giving {@code NAMER} its value, which is
 * not a constant.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SampleAnnotation {

    int DEFAULT_LEVEL = 1;

    int OK = HttpURLConnection.HTTP_OK;

    Function<String, String> NAMER = name -> {
        class Trimmer {
            String trim(final String text) {
                return text.trim();
            }
        }
        final Function<String, String> trimmed = new Function<>() {
            @Override
            public String apply(final String text) {
                return new Trimmer().trim(text);
            }
        };
        return trimmed.apply(name);
    };

    String value();

    int level() default DEFAULT_LEVEL;

    enum Kind {
        PLAIN, STRICT
    }
}
