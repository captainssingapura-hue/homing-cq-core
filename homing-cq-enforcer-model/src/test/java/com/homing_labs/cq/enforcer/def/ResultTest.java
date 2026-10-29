package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.core.principle.def.Principle;
import com.homing_labs.cq.core.def.Rule;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    private final Mark first = new Mark("first");
    private final Mark second = new Mark("second");

    @Test
    void findingsAreEqualInAnyOrder() {
        final Set<Mark> firstThenSecond = new LinkedHashSet<>(List.of(first, second));
        final Set<Mark> secondThenFirst = new LinkedHashSet<>(List.of(second, first));
        assertEquals(new Findings<>(firstThenSecond), new Findings<>(secondThenFirst));
    }

    @Test
    void findingsAreCopiedWhenMade() {
        final Set<Mark> marks = new HashSet<>(Set.of(first));
        final Findings<JavaLang, Marking> findings = new Findings<>(marks);
        marks.add(second);
        assertEquals(new Findings<>(Set.of(first)), findings);
    }

    @Test
    void aResultHasFindingsWhenTheRuleFoundSomething() {
        assertTrue(new Result<>(new Findings<>(Set.of(first))).hasFindings());
        assertFalse(new Result<JavaLang, Marking>(new Findings<>(Set.of())).hasFindings());
    }

    @Test
    void findingsAndResultsHoldToTheOntology() {
        assertEquals(List.of(), new OntologyEnforcer().enforce(Findings.class));
        assertEquals(List.of(), new OntologyEnforcer().enforce(Result.class));
    }

    private record Tidiness() implements Principle {
    }

    private record Marking() implements Rule<Tidiness, JavaLang> {
    }

    private record Mark(String name) implements Finding<JavaLang, Marking> {
    }
}
