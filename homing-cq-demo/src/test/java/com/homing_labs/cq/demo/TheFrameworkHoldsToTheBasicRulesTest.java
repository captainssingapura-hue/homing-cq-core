package com.homing_labs.cq.demo;

import com.homing_labs.cq.bench.Bench;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.core.principle.def.Principle;
import com.homing_labs.cq.demo.exempted.DemoExemptions;
import com.homing_labs.cq.demo.exempted.WireMessage;
import com.homing_labs.cq.rules.basic.BasicRules;
import com.homing_labs.cq.rules.basic.rule.NoStaticMethods;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.outline.JavaOutliner;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.scan.Compliance;
import com.homing_labs.cq.scan.Gate;
import com.homing_labs.cq.scan.ScanResult;
import com.homing_labs.cq.scan.Scanner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The framework's own modules, and this demo's, checked against the basic rules from outside - as a qc module checks
 * the code it depends on, which knows nothing of the check - by the declarations the bench takes too: the demo's code,
 * the basic rules and the demo's exemptions. The scan reads all the main code under the framework's top package that
 * this module has on its classpath; its tests are left out. The code passes the gate: every unit read with its source,
 * and none breaking a rule, but for the one class in the demo that does so on purpose and is let off.
 */
class TheFrameworkHoldsToTheBasicRulesTest {

    /**
     * One class of each module the scan must read: principle, core model, enforcer model, Java, Java taxonomy, Java
     * lexer, Java outline, scan, the basic rules, bench, and this demo.
     */
    private final List<Class<?>> modules = List.of(Principle.class, JavaLang.class, Enforcer.class, JavaUnit.class,
            ObjectKind.class, JavaLexer.class, JavaOutliner.class, Scanner.class, NoStaticMethods.class, Bench.class,
            DemoCode.class);

    private final ClassLoader loader = getClass().getClassLoader();

    private final ScanResult result = Scanner.INSTANCE.scan(
            DemoCode.INSTANCE.and(BasicRules.INSTANCE).and(DemoExemptions.INSTANCE), loader);

    @Test
    void everyModuleIsRead() {
        final List<String> read = result.units().items().stream().map(scanned -> scanned.unit().type().getName())
                .toList();
        assertEquals(List.of(), modules.stream().map(Class::getName).filter(name -> !read.contains(name)).toList(),
                "a module the scan did not read");
    }

    @Test
    void theFrameworkPassesTheGate() {
        assertEquals(List.of(), Gate.INSTANCE.failures(result));
    }

    @Test
    void itPassesByOneExemptionAndNoMore() {
        final List<String> withoutIt = Gate.INSTANCE.failures(DemoCode.INSTANCE.and(BasicRules.INSTANCE), loader);
        assertEquals(1, withoutIt.size(), withoutIt.toString());
        assertTrue(withoutIt.getFirst().startsWith(WireMessage.class.getName() + " NoPublicFields: "),
                withoutIt.toString());
        assertEquals(1, result.exemptionsUsed());
        assertEquals(List.of(), result.stale());
    }

    @Test
    void theClassLetOffStillCountsAsNotComplying() {
        assertEquals(new Compliance(result.units().items().size() - 1, result.units().items().size(), 1),
                result.compliance());
    }
}
