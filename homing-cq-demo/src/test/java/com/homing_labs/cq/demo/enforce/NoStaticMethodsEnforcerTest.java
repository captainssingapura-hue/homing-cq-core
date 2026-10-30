package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.NoStaticMethods;
import com.homing_labs.cq.demo.samples.Clean;
import com.homing_labs.cq.demo.samples.GlobalCounter;
import com.homing_labs.cq.demo.samples.Greeting;
import com.homing_labs.cq.demo.samples.StaticHelper;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaMethod;
import com.homing_labs.cq.lang.java.def.JavaUnits;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoStaticMethodsEnforcerTest {

    @Test
    void findsStaticMethodsInTheClassAndInItsNestedClasses() throws ClassNotFoundException {
        // StaticHelper.Inner is package-private, so it is named by its binary name from here.
        final Class<?> inner = Class.forName(StaticHelper.class.getName() + "$Inner");
        assertEquals(found(
                        new StaticMethod(new JavaMethod(StaticHelper.class, "twice", int.class)),
                        new StaticMethod(new JavaMethod(inner, "label", Object.class, int.class))),
                check(StaticHelper.class));
    }

    @Test
    void findsAnInterfacesStaticMethod() {
        assertEquals(found(new StaticMethod(new JavaMethod(Greeting.class, "english"))), check(Greeting.class));
    }

    @Test
    void doesNotCountWhatTheCompilerMakes() {
        assertEquals(found(), check(Clean.class));
    }

    @Test
    void staticFieldsAreNotItsConcern() {
        assertEquals(found(), check(GlobalCounter.class));
    }

    private Result<JavaLang, NoStaticMethods> check(final Class<?> type) {
        return NoStaticMethodsEnforcer.INSTANCE.check(JavaUnits.INSTANCE.of(type, Optional.empty()));
    }

    private Result<JavaLang, NoStaticMethods> found(final StaticMethod... methods) {
        return new Result<>(new Findings<>(Set.of(methods)));
    }
}
