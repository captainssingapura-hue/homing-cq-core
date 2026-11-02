package com.homing_labs.cq.scan;

import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.scan.rules.EveryUnitIsSeen;
import com.homing_labs.cq.scan.rules.InterfacesAreSeen;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScannerTest {

    private static final String TAXONOMY = "com.homing_labs.cq.lang.java.taxonomy";

    private static final Ruleset RULES = new PackageRuleset("com.homing_labs.cq.scan.rules");

    private final ScanResult result = Scanner.INSTANCE.scan(List.of(TAXONOMY), List.of(RULES),
            getClass().getClassLoader());

    @Test
    void theUnitsAreThoseOfMainCodeUnderThePackages() {
        assertEquals(ClasspathUnits.INSTANCE.main(List.of(TAXONOMY), getClass().getClassLoader()).stream()
                        .map(unit -> unit.type().getName()).toList(),
                result.units().items().stream().map(scanned -> scanned.unit().type().getName()).toList());
        assertEquals(2, result.rules());
    }

    @Test
    void eachUnitIsCheckedByTheRulesForItsKind() {
        final ScannedUnit occurrence = scanned("scan.Occurrence");
        assertEquals(List.of(EveryUnitIsSeen.class, InterfacesAreSeen.class), rules(occurrence.applied()));
        final ScannedUnit scanner = scanned("scan.ClassScanner");
        assertEquals(List.of(EveryUnitIsSeen.class), rules(scanner.applied()));
        assertEquals(new Compliance(0, 1, 1), scanner.compliance());
    }

    @Test
    void aMemberClassIsCheckedAloneAndAnyOtherClassAsItsUnit() {
        final ScannedUnit memberOf = scanned("MemberOf");
        final JavaUnit unit = memberOf.unit();
        assertFalse(memberOf.members().items().isEmpty(), "MemberOf declares member classes");
        memberOf.members().items().forEach(member -> assertEquals(member.applied(), memberOf.appliedTo(member.name())));
        assertEquals(memberOf.applied(), memberOf.appliedTo(unit.identity().name()));
        assertEquals(memberOf.applied(), memberOf.appliedTo(new ClassName(unit.type().getName() + "$1")));
    }

    @Test
    void theResultTalliesEveryUnitAndFindsEachAgain() {
        assertEquals(result.units().items().size(), result.compliance().total());
        result.units().items().forEach(scanned -> assertEquals(scanned, result.of(scanned.unit()).orElseThrow()));
    }

    private ScannedUnit scanned(final String name) {
        return result.units().items().stream()
                .filter(scanned -> scanned.unit().type().getName().equals(TAXONOMY + "." + name))
                .findFirst().orElseThrow(() -> new AssertionError(name + " not scanned"));
    }

    private List<Class<?>> rules(final AppliedRules applied) {
        return applied.items().stream().<Class<?>>map(each -> each.signature().rule()).toList();
    }
}
