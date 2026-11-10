package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.List;

/**
 * How far some units comply with the rules applied to them: how many comply, out of how many, and how many findings
 * the rest have between them. A unit complies when every rule applied to it ran and found nothing: one that failed
 * cannot vouch for it, and a unit no rule applies to complies with them all.
 */
public record Compliance(int compliant, int total, int findings) implements Scalar {

    /** No units at all. */
    public static final Compliance NONE = new Compliance(0, 0, 0);

    /** One unit's, from the rules applied to it. */
    public Compliance(final List<Applied> applied) {
        this(applied.stream().allMatch(Applied::complies) ? 1 : 0, 1,
                applied.stream().mapToInt(Applied::findingCount).sum());
    }

    /** These units and others together. */
    public Compliance plus(final Compliance other) {
        return new Compliance(compliant + other.compliant, total + other.total, findings + other.findings);
    }

    /** Whether every unit complies - as no units at all do. */
    public boolean complete() {
        return compliant == total;
    }

    /** The share of the units that comply, from 0 to 1: 1 of no units at all. */
    public double share() {
        return total == 0 ? 1 : (double) compliant / total;
    }
}
