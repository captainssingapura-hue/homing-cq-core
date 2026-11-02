package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanTypesTest {

    @Test
    void everyTypeOfTheScanSaysWhatItIsAndIsHeldToIt() {
        for (final Class<?> type : List.of(ClasspathUnits.class, PackageRuleset.class, RuleChecks.class,
                Applied.class, AppliedRules.class, ClassRules.class, MemberRules.class, Compliance.class,
                UnitClasses.class, ClassAttributes.class, Scanner.class, ScanResult.class, ScannedUnits.class,
                ScannedUnit.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }
}
