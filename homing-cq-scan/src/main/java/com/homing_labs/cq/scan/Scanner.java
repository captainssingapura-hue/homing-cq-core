package com.homing_labs.cq.scan;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.lang.java.unit.JavaNests;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.nio.file.Path;
import java.util.List;

/**
 * Scans the main code under some packages: reads every unit there the class loader finds - test code is left out -
 * and checks each by the rulesets, the unit whole and each member class nested in it alone. It needs no UI: the bench
 * shows what it finds, and a build's {@link Gate} passes or fails the code by it.
 */
public record Scanner() implements StatelessFunctionalObject {

    public static final Scanner INSTANCE = new Scanner();

    /** The units of main code {@code declaration} covers, as {@code loader} finds them, checked by its rulesets. */
    public ScanResult scan(final Declaration declaration, final ClassLoader loader) {
        return scan(declaration.packages(), declaration.rulesets(), declaration.sources(), loader);
    }

    /**
     * The units of main code under {@code packages}, as {@code loader} finds them, checked by {@code rulesets}: each
     * with its source from the first of {@code sources} that has it, or else from beside its classes.
     */
    public ScanResult scan(final List<String> packages, final List<Ruleset> rulesets, final List<Path> sources,
                           final ClassLoader loader) {
        final List<Enforcer<?, ?, ?>> enforcers = rulesets.stream()
                .flatMap(ruleset -> ruleset.enforcers(loader).stream())
                .distinct()
                .toList();
        return new ScanResult(new ScannedUnits(ClasspathUnits.INSTANCE.main(packages, sources, loader).stream()
                .map(unit -> scanned(enforcers, unit))
                .toList()), enforcers.size());
    }

    private ScannedUnit scanned(final List<Enforcer<?, ?, ?>> enforcers, final JavaUnit unit) {
        return new ScannedUnit(unit, new AppliedRules(RuleChecks.INSTANCE.applied(enforcers, unit)),
                new MemberRules(JavaNests.INSTANCE.of(unit).stream()
                        .map(nested -> new ClassRules(nested.nested().object().name(),
                                new AppliedRules(RuleChecks.INSTANCE.applied(enforcers, nested))))
                        .toList()));
    }
}
