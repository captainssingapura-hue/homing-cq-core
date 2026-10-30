package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathUnitsTest {

    private static final String TAXONOMY = "com.homing_labs.cq.lang.java.taxonomy";

    private final List<JavaUnit> units = ClasspathUnits.INSTANCE.read(TAXONOMY, getClass().getClassLoader());

    @Test
    void everyTopLevelClassUnderTheRootPackageIsAUnit() {
        final List<String> names = units.stream().map(unit -> unit.type().getName()).toList();
        assertTrue(names.containsAll(List.of(TAXONOMY + ".ClassKind", TAXONOMY + ".MemberOf",
                TAXONOMY + ".scan.ClassScanner")), names.toString());
        assertTrue(names.stream().allMatch(name -> name.startsWith(TAXONOMY + ".")), names.toString());
        assertTrue(names.stream().noneMatch(name -> name.contains("$")), names.toString());
        assertEquals(names.stream().sorted().toList(), names);
    }

    @Test
    void aModuleOnTheClasspathHasItsSourcesFound() {
        assertTrue(units.stream().allMatch(unit -> unit instanceof JavaUnit.WithSource), units.toString());
    }

    @Test
    void aPackageNoCodeBaseHasHasNoUnits() {
        assertEquals(List.of(), ClasspathUnits.INSTANCE.read("no.such.root", getClass().getClassLoader()));
    }
}
