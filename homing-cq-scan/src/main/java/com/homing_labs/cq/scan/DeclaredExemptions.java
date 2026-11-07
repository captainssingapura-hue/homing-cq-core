package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The exemptions a scan was declared with, each once, by class and then by rule. A class pleads one excuse to a rule:
 * two are refused, as nothing could say which one let it off.
 */
public record DeclaredExemptions(List<Exemption<?, ?, ?>> items) implements Vector<Exemption<?, ?, ?>> {

    public DeclaredExemptions {
        items = items.stream().distinct()
                .sorted(Comparator.comparing((Exemption<?, ?, ?> item) -> item.offender().getName())
                        .thenComparing(item -> item.rule().getName())
                        .thenComparing(item -> item.excuse().getName()))
                .toList();
        items.stream().collect(Collectors.groupingBy(item -> List.of(item.offender(), item.rule())))
                .values().stream().filter(pleaded -> pleaded.size() > 1).findFirst().ifPresent(pleaded -> {
                    throw new IllegalArgumentException(pleaded.getFirst().offender().getName() + " pleads "
                            + pleaded.size() + " excuses to " + pleaded.getFirst().rule().getName());
                });
    }

    /** The rules applied to a class of {@code unit}, each let off where an exemption covers the unit for it. */
    public AppliedRules on(final Class<?> unit, final AppliedRules applied) {
        return new AppliedRules(applied.items().stream()
                .map(one -> covering(one.signature().rule(), unit).map(one::exempt).orElse(one))
                .toList());
    }

    /** Those that let nothing off on these units. */
    public List<StaleExemption> stale(final ScannedUnits units) {
        return items.stream().flatMap(item -> stale(item, units).stream()).toList();
    }

    private Optional<Exemption<?, ?, ?>> covering(final Class<?> rule, final Class<?> unit) {
        return items.stream().filter(item -> item.rule() == rule && item.offender() == unit).findFirst();
    }

    /** How an exemption is stale: not at all, if it lets something off, or if a rule that threw leaves it unknown. */
    private Optional<StaleExemption> stale(final Exemption<?, ?, ?> exemption, final ScannedUnits units) {
        final Optional<ScannedUnit> offender = units.items().stream()
                .filter(scanned -> scanned.unit().type() == exemption.offender()).findFirst();
        if (offender.isEmpty()) {
            return Optional.of(new StaleExemption.NotScanned(exemption));
        }
        final List<Applied> applied = offender.get().applied().items().stream()
                .filter(one -> one.signature().rule() == exemption.rule()).toList();
        if (applied.isEmpty()) {
            return Optional.of(new StaleExemption.NotApplied(exemption));
        }
        return applied.stream().anyMatch(one -> one.exempted() || one.failure().isPresent()) ? Optional.empty()
                : Optional.of(new StaleExemption.NothingFound(exemption));
    }
}
