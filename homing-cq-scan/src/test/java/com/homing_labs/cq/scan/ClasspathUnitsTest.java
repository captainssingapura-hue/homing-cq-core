package com.homing_labs.cq.scan;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
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
        assertTrue(units.stream().allMatch(unit -> unit.source().isPresent()), units.toString());
    }

    @Test
    void severalRootPackagesAreReadTogetherEachUnitOnce() {
        assertEquals(units, ClasspathUnits.INSTANCE.read(List.of(TAXONOMY + ".scan", TAXONOMY, "no.such.root"),
                getClass().getClassLoader()), "the units under the first root are under the second too");
    }

    @Test
    void aPackageNoCodeBaseHasHasNoUnits() {
        assertEquals(List.of(), ClasspathUnits.INSTANCE.read("no.such.root", getClass().getClassLoader()));
    }

    @Test
    void mainCodeLeavesTheTestsOut() {
        final List<String> scan = List.of("com.homing_labs.cq.scan");
        final List<String> all = ClasspathUnits.INSTANCE.read(scan, getClass().getClassLoader()).stream()
                .map(unit -> unit.type().getName()).toList();
        final List<String> main = ClasspathUnits.INSTANCE.main(scan, getClass().getClassLoader()).stream()
                .map(unit -> unit.type().getName()).toList();
        assertTrue(all.containsAll(List.of(Scanner.class.getName(), getClass().getName())), all.toString());
        assertTrue(main.contains(Scanner.class.getName()), main.toString());
        assertTrue(main.stream().noneMatch(name -> name.endsWith("Test") || name.contains(".rules.")),
                main.toString());
    }
}
