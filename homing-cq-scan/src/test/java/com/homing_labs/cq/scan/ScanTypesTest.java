package com.homing_labs.cq.scan;

import com.homing_labs.cq.scan.declarations.ClassScannerExemptions;
import com.homing_labs.cq.scan.rules.SeenOnPurpose;
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
                ScannedUnit.class, ComposedDeclaration.class, ReactorSources.class, Instances.class,
                NamedDeclarations.class, Gate.class, Report.class, ReportRow.class, ReportRows.class,
                ReportTable.class, ReportSummary.class, ReportCommandLine.class, ResultText.class, Exemption.class,
                DeclaredExemptions.class, StaleExemption.NothingFound.class, StaleExemption.NotApplied.class,
                StaleExemption.NotScanned.class, ClassScannerExemptions.class, SeenOnPurpose.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }
}
