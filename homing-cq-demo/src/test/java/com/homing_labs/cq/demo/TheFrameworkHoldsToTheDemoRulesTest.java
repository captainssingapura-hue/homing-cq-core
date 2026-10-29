package com.homing_labs.cq.demo;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.core.principle.def.Principle;
import com.homing_labs.cq.demo.enforce.NoStaticMethodsEnforcer;
import com.homing_labs.cq.demo.enforce.NoStaticMutableVariablesEnforcer;
import com.homing_labs.cq.demo.rule.NoStaticMethods;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaUnit;
import com.homing_labs.cq.lang.java.extract.JavaUnitReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The framework's own modules, and this demo's, checked against the demo rules from outside - as a qc module checks
 * the code it depends on, which knows nothing of the check.
 */
class TheFrameworkHoldsToTheDemoRulesTest {

    /** One class of each module checked: principle, core model, enforcer model, Java, and this demo. */
    private final List<Class<?>> anchors = List.of(
            Principle.class, JavaLang.class, Enforcer.class, JavaUnit.class, NoStaticMethods.class);

    @Test
    void everyModuleConforms() {
        final List<Result<JavaLang, ?>> found = new ArrayList<>();
        for (final Class<?> anchor : anchors) {
            final List<JavaUnit> units = JavaUnitReader.INSTANCE.read(anchor);
            assertTrue(units.size() > 0, "no units read for the module of " + anchor.getName());
            found.addAll(DemoFindings.INSTANCE.of(NoStaticMethodsEnforcer.INSTANCE, units).values());
            found.addAll(DemoFindings.INSTANCE.of(NoStaticMutableVariablesEnforcer.INSTANCE, units).values());
        }
        assertEquals(List.of(), found);
    }
}
