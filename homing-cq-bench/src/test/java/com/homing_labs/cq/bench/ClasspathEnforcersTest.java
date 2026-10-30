package com.homing_labs.cq.bench;

import com.homing_labs.cq.bench.rules.EveryUnitIsSeenEnforcer;
import com.homing_labs.cq.bench.rules.InterfacesAreSeenEnforcer;
import com.homing_labs.cq.enforcer.def.Enforcer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClasspathEnforcersTest {

    private final List<Enforcer<?, ?, ?>> enforcers =
            ClasspathEnforcers.INSTANCE.read("com.homing_labs.cq.bench.rules", getClass().getClassLoader());

    @Test
    void everyEnforcerUnderThePackageIsFound() {
        assertEquals(List.of(EveryUnitIsSeenEnforcer.class, InterfacesAreSeenEnforcer.class),
                enforcers.stream().map(Object::getClass).toList());
    }

    @Test
    void anEnforcerWithAnInstanceIsThatInstance() {
        assertSame(EveryUnitIsSeenEnforcer.INSTANCE, enforcers.getFirst());
    }
}
