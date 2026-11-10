package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.extract.ClassFileAttributes;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ConstructorValue;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.Method;
import hue.captains.singapura.tao.ontology.Scalar;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** How the bench writes what a method or a constructor throws. */
class ValueTextTest {

    private static final String CONSTRUCTOR = " - package, fixed, declared";

    private static final String METHOD = " - package, overridable, unsynchronised, fixed, declared";

    private final Attributes throwing = ClassFileAttributes.INSTANCE.of(Throwing.class);

    @Test
    void aConstructorThrowsAsDeclaredWhenItsSignatureNamesWhatItThrowsAndAsItRunsWhenNot() {
        assertEquals(List.of(
                        "() throws IOException" + CONSTRUCTOR,
                        "<X extends Exception> (int) throws X, E" + CONSTRUCTOR),
                text(throwing.all(Constructor.INSTANCE, ConstructorValue.class)));
    }

    @Test
    void aMethodThrowsAsDeclaredWhenItsSignatureNamesWhatItThrowsAndAsItRunsWhenNot() {
        assertEquals(List.of(
                        "plain(): void throws IOException, InterruptedException" + METHOD,
                        "<X extends Throwable> either(): void throws X, E" + METHOD,
                        "<T> generic(T): void throws IOException" + METHOD,
                        "none(): void" + METHOD),
                text(throwing.all(Method.Implementation.INSTANCE, MethodValue.class)));
    }

    private List<String> text(final List<? extends WithSource<? extends Scalar>> values) {
        return values.stream().map(value -> ValueText.INSTANCE.of(value.object())).toList();
    }

    /**
     * A generic class whose constructors and methods throw: classes alone, which only the class file's
     * {@code Exceptions} names, or type variables too, which its signatures name with the rest. A generic method that
     * throws no type variable has a signature that names nothing thrown.
     */
    @SuppressWarnings("unused")
    private static class Throwing<E extends Exception> {

        Throwing() throws IOException {
        }

        <X extends Exception> Throwing(final int code) throws X, E {
        }

        void plain() throws IOException, InterruptedException {
        }

        <X extends Throwable> void either() throws X, E {
        }

        <T> void generic(final T value) throws IOException {
        }

        void none() {
        }
    }
}
