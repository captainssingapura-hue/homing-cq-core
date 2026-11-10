package com.homing_labs.cq.scan;

import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A unit as a scan found it: the unit, the rules applied to it whole - each enforcer whose unit type it is of - and
 * the rules applied to each member class nested in it alone, as sub-rules are.
 */
public record ScannedUnit(JavaUnit unit, AppliedRules applied, MemberRules members) implements Scalar {

    /** How far the unit complies with the rules applied to it whole. */
    public Compliance compliance() {
        return new Compliance(applied.items());
    }

    /**
     * The rules applied to a class of the unit: to a member class alone, or else to the whole unit - the top-level
     * class's, and a local or anonymous class's, which is no member.
     */
    public AppliedRules appliedTo(final ClassName name) {
        return members.items().stream().filter(member -> member.name().equals(name)).map(ClassRules::applied)
                .findFirst().orElse(applied);
    }
}
