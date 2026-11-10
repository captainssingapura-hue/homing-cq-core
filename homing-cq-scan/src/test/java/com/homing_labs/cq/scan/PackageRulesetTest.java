package com.homing_labs.cq.scan;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.scan.rules.EveryUnitIsSeenEnforcer;
import com.homing_labs.cq.scan.rules.InterfacesAreSeenEnforcer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PackageRulesetTest {

    private static final String RULES = "com.homing_labs.cq.scan.rules";

    private final List<Enforcer<?, ?, ?>> enforcers = new PackageRuleset(RULES).enforcers(getClass().getClassLoader());

    @Test
    void theRulesetIsNamedByItsPackage() {
        assertEquals(RULES, new PackageRuleset(RULES).name());
    }

    @Test
    void everyEnforcerUnderThePackageIsFound() {
        assertEquals(List.of(EveryUnitIsSeenEnforcer.class, InterfacesAreSeenEnforcer.class),
                enforcers.stream().map(Object::getClass).toList());
    }

    @Test
    void anEnforcerWithAnInstanceIsThatInstance() {
        assertSame(EveryUnitIsSeenEnforcer.INSTANCE, enforcers.getFirst());
    }

    @Test
    void aPackageWithNoEnforcersIsAnEmptyRuleset() {
        assertEquals(List.of(), new PackageRuleset("no.rules.here").enforcers(getClass().getClassLoader()));
    }
}
