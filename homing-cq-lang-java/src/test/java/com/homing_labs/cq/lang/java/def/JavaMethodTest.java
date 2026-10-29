package com.homing_labs.cq.lang.java.def;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JavaMethodTest {

    @Test
    void aReflectedMethodIsItsClassNameAndParameterTypes() throws NoSuchMethodException {
        assertEquals(new JavaMethod(String.class, "substring", int.class, int.class),
                new JavaMethod(String.class.getMethod("substring", int.class, int.class)));
    }

    @Test
    void overloadsAreToldApartByTheirParameterTypes() throws NoSuchMethodException {
        assertNotEquals(new JavaMethod(String.class.getMethod("valueOf", int.class)),
                new JavaMethod(String.class.getMethod("valueOf", char.class)));
    }

    @Test
    void parameterTypesCountInOrderAndEachTimeTheyCome() {
        assertNotEquals(new JavaMethod(Object.class, "m", int.class, String.class),
                new JavaMethod(Object.class, "m", String.class, int.class));
        assertNotEquals(new JavaMethod(Object.class, "m", int.class, int.class),
                new JavaMethod(Object.class, "m", int.class));
    }

    @Test
    void parameterTypesAreCopiedWhenMade() {
        final List<JavaType> types = new ArrayList<>(List.of(new JavaType(int.class)));
        final JavaTypes parameters = new JavaTypes(types);
        types.add(new JavaType(String.class));
        assertEquals(new JavaTypes(List.of(new JavaType(int.class))), parameters);
    }
}
