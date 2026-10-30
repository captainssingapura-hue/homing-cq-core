package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.core.principle.def.Principle;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnforcersTest {

    private final Page page = new Page("one");

    private final Chapter chapter = new Chapter("first");

    @Test
    void anEnforcerIsForTheLanguagePrincipleRuleAndUnitItsClassNames() {
        assertEquals(new EnforcerSignature(JavaLang.class, Tidiness.class, Tidy.class, Page.class),
                Enforcers.INSTANCE.signatureOf(new PagesAreTidy()));
    }

    @Test
    void anEnforcerChecksTheUnitsOfItsTypeAndOfTypesBelowIt() {
        assertTrue(Enforcers.INSTANCE.signatureOf(new PagesAreTidy()).checks(page));
        assertFalse(Enforcers.INSTANCE.signatureOf(new PagesAreTidy()).checks(chapter));
        assertTrue(Enforcers.INSTANCE.signatureOf(new DocsAreTidy()).checks(page));
        assertTrue(Enforcers.INSTANCE.signatureOf(new DocsAreTidy()).checks(chapter));
    }

    @Test
    void anEnforcerRunsOnAUnitItChecks() {
        assertEquals(new Result<>(new Findings<>(Set.of(new Untidy("one")))),
                Enforcers.INSTANCE.check(new PagesAreTidy(), page));
    }

    @Test
    void anEnforcerIsNotRunOnAUnitItDoesNotCheck() {
        assertThrows(IllegalArgumentException.class, () -> Enforcers.INSTANCE.check(new PagesAreTidy(), chapter));
    }

    @Test
    void anEnforcerWhoseClassLeavesItsUnitOpenCannotBeRead() {
        assertThrows(IllegalArgumentException.class, () -> Enforcers.INSTANCE.signatureOf(new AnyDoc<Page>()));
    }

    @Test
    void signaturesAndTheReaderHoldToTheOntology() {
        assertEquals(List.of(), new OntologyEnforcer().enforce(EnforcerSignature.class));
        assertEquals(List.of(), new OntologyEnforcer().enforce(Enforcers.class));
    }

    private record Tidiness() implements Principle {
    }

    private record Tidy() implements Rule<Tidiness, JavaLang> {
    }

    private sealed interface Doc extends QcUnit<JavaLang> permits Page, Chapter {
    }

    private record Page(String name) implements Doc {
    }

    private record Chapter(String name) implements Doc {
    }

    private record Untidy(String name) implements Finding<JavaLang, Tidy> {
    }

    private record PagesAreTidy() implements Enforcer<JavaLang, Tidy, Page> {

        @Override
        public Result<JavaLang, Tidy> check(final Page unit) {
            return new Result<>(new Findings<>(Set.of(new Untidy(unit.name()))));
        }
    }

    private record DocsAreTidy() implements Enforcer<JavaLang, Tidy, Doc> {

        @Override
        public Result<JavaLang, Tidy> check(final Doc unit) {
            return new Result<>(new Findings<>(Set.of()));
        }
    }

    private record AnyDoc<D extends Doc>() implements Enforcer<JavaLang, Tidy, D> {

        @Override
        public Result<JavaLang, Tidy> check(final D unit) {
            return new Result<>(new Findings<>(Set.of()));
        }
    }
}
