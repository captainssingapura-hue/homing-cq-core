package com.homing_labs.cq.scan;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.List;
import java.util.Optional;

/**
 * What a scan found: each unit under the packages scanned, how many rules the rulesets had to check them by, and the
 * exemptions it was declared with.
 */
public record ScanResult(ScannedUnits units, int rules, DeclaredExemptions exemptions) implements Scalar {

    /** What a scan with nothing exempted found. */
    public ScanResult(final ScannedUnits units, final int rules) {
        this(units, rules, new DeclaredExemptions(List.of()));
    }

    /** How far the units comply, all together. An exemption changes no code, so it counts for nothing here. */
    public Compliance compliance() {
        return units.items().stream().map(ScannedUnit::compliance).reduce(Compliance.NONE, Compliance::plus);
    }

    /** The exemptions that let nothing off. */
    public List<StaleExemption> stale() {
        return exemptions.stale(units);
    }

    /** How many exemptions are in use: all of them but the stale. */
    public int exemptionsUsed() {
        return exemptions.items().size() - stale().size();
    }

    /** A unit as the scan found it, if the scan found it. */
    public Optional<ScannedUnit> of(final JavaUnit unit) {
        return units.items().stream()
                .filter(scanned -> scanned.unit().identity().name().equals(unit.identity().name()))
                .findFirst();
    }
}
