package com.homing_labs.cq.lang.java.def;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaFieldTest {

    @Test
    void aReflectedFieldIsItsClassNameAndType() throws NoSuchFieldException {
        assertEquals(new JavaField(Integer.class, "MAX_VALUE", int.class),
                new JavaField(Integer.class.getField("MAX_VALUE")));
    }
}
