package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An exemption that let nothing off, and how: each way is a type of its own. It fails the {@link Gate}, so that
 * exemptions can only shrink: when the code is fixed, the exemption goes too.
 */
public sealed interface StaleExemption extends Scalar {

    /** The exemption. */
    Exemption<?, ?, ?> exemption();

    /** How it let nothing off, in the words of the gate and the report. */
    String text();

    /** One line, in the gate's words: the class, the rule, the excuse, and how it let nothing off. */
    default String line() {
        return exemption().offender().getName() + " " + exemption().rule().getSimpleName() + ": "
                + exemption().excuse().getSimpleName() + " is stale: " + text();
    }

    /** The rule ran on the class and found nothing, so there is nothing to excuse. */
    record NothingFound(Exemption<?, ?, ?> exemption) implements StaleExemption {

        @Override
        public String text() {
            return "the rule found nothing on the class";
        }
    }

    /** The class was scanned, and no enforcer of the rule checked it. */
    record NotApplied(Exemption<?, ?, ?> exemption) implements StaleExemption {

        @Override
        public String text() {
            return "the rule was not applied to the class";
        }
    }

    /** The class is not among those scanned. */
    record NotScanned(Exemption<?, ?, ?> exemption) implements StaleExemption {

        @Override
        public String text() {
            return "the class was not scanned";
        }
    }
}
