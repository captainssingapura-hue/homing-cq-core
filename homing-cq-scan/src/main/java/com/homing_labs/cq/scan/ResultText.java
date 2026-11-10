package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * The one text of a finding or a notice, wherever it is shown: what it prints, without the packages of the classes it
 * names. The gate, the rule book and the report all show this, so the three always agree.
 */
public record ResultText() implements StatelessFunctionalObject {

    public static final ResultText INSTANCE = new ResultText();

    /** The text of a finding or a notice. */
    public String of(final Scalar findingOrNotice) {
        return String.valueOf(findingOrNotice).replaceAll("(?:class )?(?:[a-z_][a-z0-9_]*\\.)+", "");
    }
}
