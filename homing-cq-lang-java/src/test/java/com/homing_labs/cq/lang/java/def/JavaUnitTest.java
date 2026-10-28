package com.homing_labs.cq.lang.java.def;

import com.homing_labs.cq.lang.java.fixtures.Nesting;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaUnitTest {

    @Test
    void aClassOnlyUnitRefusesANestedClass() {
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> new JavaUnit.ClassOnly(Nesting.Member.class));
        assertEquals("A JavaUnit is a top-level class, but com.homing_labs.cq.lang.java.fixtures.Nesting$Member"
                + " is nested in com.homing_labs.cq.lang.java.fixtures.Nesting", refused.getMessage());
    }

    @Test
    void aUnitWithSourceRefusesANestedClass() {
        assertThrows(IllegalArgumentException.class, () -> new JavaUnit.WithSource(Nesting.StaticNested.class,
                new JavaSource(URI.create("file:///Nesting.java"), "")));
    }
}
