package com.homing_labs.cq.scan;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/** What a scan found: each unit under the packages scanned, and how many rules the rulesets had to check them by. */
public record ScanResult(ScannedUnits units, int rules) implements Scalar {

    /** How far the units comply, all together. */
    public Compliance compliance() {
        return units.items().stream().map(ScannedUnit::compliance).reduce(Compliance.NONE, Compliance::plus);
    }

    /** A unit as the scan found it, if the scan found it. */
    public Optional<ScannedUnit> of(final JavaUnit unit) {
        return units.items().stream()
                .filter(scanned -> scanned.unit().identity().name().equals(unit.identity().name()))
                .findFirst();
    }
}
