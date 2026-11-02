package com.homing_labs.cq.scan;

import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.enforcer.def.Enforcers;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Applies rules to a unit: each enforcer whose unit type the unit is of is run on it - a rule for every unit, or one
 * for its kind of class alone.
 */
public record RuleChecks() implements StatelessFunctionalObject {

    public static final RuleChecks INSTANCE = new RuleChecks();

    /** Each of the enforcers that checks units like this one, run on it, by the name of its rule. */
    public List<Applied> applied(final List<Enforcer<?, ?, ?>> enforcers, final QcUnit<?> unit) {
        return enforcers.stream()
                .filter(enforcer -> Enforcers.INSTANCE.signatureOf(enforcer).checks(unit))
                .map(enforcer -> run(enforcer, unit))
                .sorted(Comparator.comparing(applied -> applied.signature().rule().getSimpleName()))
                .toList();
    }

    /** How far a unit complies with the enforcers that check units like it. */
    public Compliance compliance(final List<Enforcer<?, ?, ?>> enforcers, final QcUnit<?> unit) {
        return new Compliance(applied(enforcers, unit));
    }

    private Applied run(final Enforcer<?, ?, ?> enforcer, final QcUnit<?> unit) {
        final EnforcerSignature signature = Enforcers.INSTANCE.signatureOf(enforcer);
        try {
            return new Applied(signature, Optional.of(Enforcers.INSTANCE.check(enforcer, unit)), Optional.empty());
        } catch (final RuntimeException e) {
            return new Applied(signature, Optional.empty(), Optional.of(String.valueOf(e)));
        }
    }
}
