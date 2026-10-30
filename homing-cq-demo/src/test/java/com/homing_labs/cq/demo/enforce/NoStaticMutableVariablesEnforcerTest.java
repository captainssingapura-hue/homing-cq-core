package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.rule.NoStaticMethods;
import com.homing_labs.cq.demo.rule.NoStaticMutableVariables;
import com.homing_labs.cq.demo.samples.Clean;
import com.homing_labs.cq.demo.samples.GlobalCounter;
import com.homing_labs.cq.demo.samples.StaticHelper;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaField;
import com.homing_labs.cq.lang.java.def.JavaUnits;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoStaticMutableVariablesEnforcerTest {

    @Test
    void findsAFieldThatIsNotFinalAndFinalFieldsWhoseContentsCanChange() {
        assertEquals(found(
                        new StaticMutableVariable.NotFinal(new JavaField(GlobalCounter.class, "count", int.class)),
                        new StaticMutableVariable.NotKnownImmutable(
                                new JavaField(GlobalCounter.class, "SEEN", List.class)),
                        new StaticMutableVariable.NotKnownImmutable(
                                new JavaField(GlobalCounter.class, "TABLE", int[].class))),
                check(GlobalCounter.class));
    }

    @Test
    void constantsOfImmutableTypesConformAndCompilerMadeFieldsAreNotCounted() {
        assertEquals(found(), check(Clean.class));
    }

    @Test
    void aStatelessSingletonsInstanceConforms() {
        assertEquals(found(), check(NoStaticMethods.class));
    }

    @Test
    void staticMethodsAreNotItsConcern() {
        assertEquals(found(), check(StaticHelper.class));
    }

    private Result<JavaLang, NoStaticMutableVariables> check(final Class<?> type) {
        return NoStaticMutableVariablesEnforcer.INSTANCE.check(JavaUnits.INSTANCE.of(type, Optional.empty()));
    }

    private Result<JavaLang, NoStaticMutableVariables> found(final StaticMutableVariable... variables) {
        return new Result<>(new Findings<>(Set.of(variables)));
    }
}
