package com.homing_labs.cq.scan;

import com.homing_labs.cq.core.def.Excuse;
import com.homing_labs.cq.core.def.Rule;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A class let off a rule: the class that breaks it, the rule, and the excuse it pleads. All three are classes, and the
 * excuse is typed to the rule, so the compiler refuses one written for another rule. Nothing here says why in words:
 * the excuse does.
 *
 * <p>It covers that class for that rule and nothing wider. The class is a top-level one, the class the gate and the
 * report name: a rule checks a unit whole, so what it finds on a class nested in the unit is the unit's to answer for.
 * </p>
 *
 * <p>The rule still runs and what it finds is still shown, and the {@link Gate} alone lets it through: an exemption
 * changes no code, so it moves no gauge. One that lets nothing off is {@link StaleExemption stale}, and fails the gate.
 * </p>
 *
 * @param <C> the class let off
 * @param <R> the rule it breaks
 * @param <E> the excuse it pleads
 */
public record Exemption<C, R extends Rule<?, ?>, E extends Excuse<R>>(Class<C> offender, Class<R> rule,
                                                                      Class<E> excuse) implements Scalar {

    public Exemption {
        if (offender.getNestHost() != offender) {
            throw new IllegalArgumentException("Exempt the top-level class, " + offender.getNestHost().getName()
                    + ", not " + offender.getName() + " nested in it");
        }
    }
}
