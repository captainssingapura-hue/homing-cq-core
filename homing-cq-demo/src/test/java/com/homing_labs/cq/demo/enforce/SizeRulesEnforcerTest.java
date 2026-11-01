package com.homing_labs.cq.demo.enforce;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.demo.principle.DivideAndConquer;
import com.homing_labs.cq.demo.rule.MethodsWithin50Lines;
import com.homing_labs.cq.demo.rule.TopLevelClassesWithin250Lines;
import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.enforcer.def.Enforcers;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Notice;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.extract.JavaUnitReader;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.unit.JavaUnits;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The two size rules, on {@link Measured} given sources written to the lengths each test needs: a class of at most 250
 * effective lines, and methods and constructors of at most 50.
 */
class SizeRulesEnforcerTest {

    private static final ClassName MEASURED = new ClassName(Measured.class.getName());

    @Test
    void aTopLevelClassOfMoreThan250EffectiveLinesIsFoundAndOneOf250IsNot() {
        assertEquals(found(new LongClass(MEASURED, 251)), TopLevelClassesWithin250LinesEnforcer.INSTANCE
                .check(measured(declaring(249, ""))));
        assertEquals(new Result<JavaLang, TopLevelClassesWithin250Lines>(new Findings<>(Set.of())),
                TopLevelClassesWithin250LinesEnforcer.INSTANCE.check(measured(declaring(248, ""))),
                "the header and the closing brace make 250");
    }

    @Test
    void blankLinesAndCommentsAreNotCountedButEveryLineOfCodeIs() {
        final String quiet = "\n    // a comment\n    /** A documentation comment. */\n".repeat(40);
        assertEquals(new Result<JavaLang, TopLevelClassesWithin250Lines>(new Findings<>(Set.of())),
                TopLevelClassesWithin250LinesEnforcer.INSTANCE.check(measured(declaring(248, quiet))));
        final String wide = "    private int counted = " + "1 + ".repeat(30) + "1;\n";
        assertEquals(found(new LongClass(MEASURED, 251)), TopLevelClassesWithin250LinesEnforcer.INSTANCE
                .check(measured(declaring(247, wide))), "a line of 148 characters counts as two");
    }

    @Test
    void aMethodOrAConstructorOfMoreThan50EffectiveLinesIsFoundInTheClassAndInAMemberClass() {
        final String source = "package com.homing_labs.cq.demo.enforce;\n"
                + "class Measured {\n"
                + "    Measured(final int start) {\n" + "        counted++;\n".repeat(50) + "    }\n"
                + "    int count() {\n" + "        counted++;\n".repeat(48) + "    }\n"
                + "    static class Part {\n"
                + "        /** Grows. */\n"
                + "        void grow() {\n" + "            size++;\n".repeat(49) + "        }\n"
                + "    }\n"
                + "}\n";
        assertEquals(new Result<>(new Findings<>(Set.of(
                        new LongMethod(MEASURED, "Measured(final int start)", 3, 52),
                        new LongMethod(new ClassName(Measured.class.getName() + "$Part"), "grow()", 106, 51)))),
                MethodsWithin50LinesEnforcer.INSTANCE.check(measured(source)),
                "count() is written in 50, the most allowed");
    }

    @Test
    void aUnitWithoutItsSourceIsNotCheckedAndSaysSo() {
        final JavaUnit unit = JavaUnits.INSTANCE.of(Measured.class, Optional.empty());
        final Notice unavailable = new Notice.SourceUnavailable(Measured.class.getName());
        assertEquals(new Result<JavaLang, TopLevelClassesWithin250Lines>(new Findings<>(Set.of())).noting(unavailable),
                TopLevelClassesWithin250LinesEnforcer.INSTANCE.check(unit));
        assertEquals(new Result<JavaLang, MethodsWithin50Lines>(new Findings<>(Set.of())).noting(unavailable),
                MethodsWithin50LinesEnforcer.INSTANCE.check(unit));
    }

    @Test
    void theClassAsWrittenHoldsToBoth() {
        final JavaUnit unit = JavaUnitReader.INSTANCE.read(Measured.class).stream()
                .filter(read -> read.type() == Measured.class).findFirst().orElseThrow();
        assertFalse(unit.source().isEmpty());
        assertEquals(new Result<JavaLang, TopLevelClassesWithin250Lines>(new Findings<>(Set.of())),
                TopLevelClassesWithin250LinesEnforcer.INSTANCE.check(unit));
        assertEquals(new Result<JavaLang, MethodsWithin50Lines>(new Findings<>(Set.of())),
                MethodsWithin50LinesEnforcer.INSTANCE.check(unit));
    }

    @Test
    void eachEnforcerSaysWhatItIsFor() {
        assertEquals(new EnforcerSignature(JavaLang.class, DivideAndConquer.class,
                        TopLevelClassesWithin250Lines.class, JavaUnit.class),
                Enforcers.INSTANCE.signatureOf(TopLevelClassesWithin250LinesEnforcer.INSTANCE));
        assertEquals(new EnforcerSignature(JavaLang.class, DivideAndConquer.class, MethodsWithin50Lines.class,
                        JavaUnit.class),
                Enforcers.INSTANCE.signatureOf(MethodsWithin50LinesEnforcer.INSTANCE));
    }

    /** The source of {@link Measured} declared with a field on each of so many lines, and more text after them. */
    private String declaring(final int fields, final String more) {
        return "package com.homing_labs.cq.demo.enforce;\n\n/** Measured. */\nclass Measured {\n"
                + "    private int counted;\n".repeat(fields) + more + "}\n";
    }

    private JavaUnit measured(final String source) {
        return JavaUnits.INSTANCE.of(Measured.class,
                Optional.of(new JavaSource(URI.create("file:///Measured.java"), source)));
    }

    private Result<JavaLang, TopLevelClassesWithin250Lines> found(final LongClass finding) {
        return new Result<>(new Findings<>(Set.of(finding)));
    }
}
