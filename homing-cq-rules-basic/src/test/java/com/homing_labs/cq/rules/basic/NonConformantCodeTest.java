package com.homing_labs.cq.rules.basic;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.rules.basic.enforce.NoStaticMethodsEnforcer;
import com.homing_labs.cq.rules.basic.enforce.NoStaticMutableVariablesEnforcer;
import com.homing_labs.cq.rules.basic.enforce.StaticMethod;
import com.homing_labs.cq.rules.basic.enforce.StaticMutableVariable;
import com.homing_labs.cq.rules.basic.nonconformant.ClassicSingleton;
import com.homing_labs.cq.rules.basic.nonconformant.Order;
import com.homing_labs.cq.rules.basic.nonconformant.Palette;
import com.homing_labs.cq.rules.basic.nonconformant.Registry;
import com.homing_labs.cq.rules.basic.nonconformant.ReportBuilder;
import com.homing_labs.cq.rules.basic.nonconformant.Settings;
import com.homing_labs.cq.rules.basic.nonconformant.StringUtils;
import com.homing_labs.cq.enforcer.def.Finding;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaField;
import com.homing_labs.cq.lang.java.def.JavaMethod;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.extract.JavaUnitReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Code that breaks the basic rules, read from outside like any other code: each rule's findings, by the unit they are
 * on. The class that conforms has no result with findings.
 *
 * <p>The code is in this module's test classes, read through one of them; only its package is kept, since the same
 * classes folder holds the tests too.</p>
 */
class NonConformantCodeTest {

    private static final String PACKAGE = "com.homing_labs.cq.rules.basic.nonconformant";

    private final List<JavaUnit> units = JavaUnitReader.INSTANCE.read(Order.class).stream()
            .filter(unit -> unit.type().getPackageName().equals(PACKAGE))
            .toList();

    @Test
    void readsEveryClassOfThePackage() {
        assertEquals(List.of(
                        PACKAGE + ".ClassicSingleton",
                        PACKAGE + ".Order",
                        PACKAGE + ".Palette",
                        PACKAGE + ".Registry",
                        PACKAGE + ".ReportBuilder",
                        PACKAGE + ".Settings",
                        PACKAGE + ".StringUtils"),
                units.stream().map(unit -> unit.type().getName()).toList());
    }

    @Test
    void findsEveryStaticMethodOnItsUnit() {
        assertEquals(Map.of(
                        ClassicSingleton.class, found(
                                new StaticMethod(new JavaMethod(ClassicSingleton.class, "getInstance"))),
                        Palette.class, found(
                                new StaticMethod(new JavaMethod(Palette.class, "named", String.class))),
                        Registry.class, found(new StaticMethod(
                                new JavaMethod(Registry.class, "register", String.class, Object.class))),
                        StringUtils.class, found(
                                new StaticMethod(new JavaMethod(StringUtils.class, "isBlank", String.class)),
                                new StaticMethod(new JavaMethod(StringUtils.class, "capitalise", String.class)))),
                RuleFindings.INSTANCE.of(NoStaticMethodsEnforcer.INSTANCE, units));
    }

    @Test
    void findsEveryStaticMutableVariableOnItsUnit() throws ClassNotFoundException {
        // ReportBuilder.Counter is package-private, so it is named by its binary name from here.
        final Class<?> counter = Class.forName(PACKAGE + ".ReportBuilder$Counter");
        assertEquals(Map.of(
                        ClassicSingleton.class, found(new StaticMutableVariable.NotFinal(
                                new JavaField(ClassicSingleton.class, "instance", ClassicSingleton.class))),
                        Palette.class, found(new StaticMutableVariable.NotKnownImmutable(
                                new JavaField(Palette.class, "BY_NAME", Map.class))),
                        Registry.class, found(new StaticMutableVariable.NotKnownImmutable(
                                new JavaField(Registry.class, "ENTRIES", Map.class))),
                        ReportBuilder.class, found(new StaticMutableVariable.NotFinal(
                                new JavaField(counter, "built", int.class))),
                        Settings.class, found(
                                new StaticMutableVariable.NotFinal(
                                        new JavaField(Settings.class, "region", String.class)),
                                new StaticMutableVariable.NotFinal(
                                        new JavaField(Settings.class, "verbose", boolean.class)))),
                RuleFindings.INSTANCE.of(NoStaticMutableVariablesEnforcer.INSTANCE, units));
    }

    @SafeVarargs
    private <R extends Rule<?, JavaLang>> Result<JavaLang, R> found(final Finding<JavaLang, R>... findings) {
        return new Result<>(new Findings<>(Set.of(findings)));
    }
}
