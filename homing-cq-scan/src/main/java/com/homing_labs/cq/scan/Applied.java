package com.homing_labs.cq.scan;

import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.enforcer.def.Result;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A rule applied to a unit: what its enforcer is for, and its result - or why it gave none, when it failed - and the
 * {@link Exemption} that lets the unit off what it found, if one does. Only findings are let off: a rule that failed
 * stays failed, as nothing says what it would have found.
 */
public record Applied(EnforcerSignature signature, Optional<Result<?, ?>> result, Optional<String> failure,
                      Optional<Exemption<?, ?, ?>> exemption) implements Scalar {

    /** A rule applied, with nothing exempted. */
    public Applied(final EnforcerSignature signature, final Optional<Result<?, ?>> result,
                   final Optional<String> failure) {
        this(signature, result, failure, Optional.empty());
    }

    /** This rule with its findings let off by an exemption: as it was, when it has none. */
    public Applied exempt(final Exemption<?, ?, ?> lettingOff) {
        return findingCount() == 0 ? this : new Applied(signature, result, failure, Optional.of(lettingOff));
    }

    /** Whether an exemption lets its findings off. */
    public boolean exempted() {
        return exemption.isPresent();
    }

    /**
     * Whether the unit complies with the rule: it was checked, and nothing was found. A notice is no finding, and an
     * exemption changes no code, so a unit let off a rule still does not comply with it.
     */
    public boolean complies() {
        return result.map(given -> !given.hasFindings()).orElse(false);
    }

    /** How many findings the rule has on the unit, exempted or not: none, when it failed. */
    public int findingCount() {
        return result.map(given -> given.findings().items().size()).orElse(0);
    }

    /** How the rule came out: its findings counted, saying so if exempted; {@code none}; or {@code failed}. */
    public String outcome() {
        return result.map(given -> given.hasFindings()
                ? findingCount() + (exempted() ? " exempted" : "") : "none").orElse("failed");
    }

    /** Its findings, each shown without the packages of the classes it names: none, when it failed. */
    public List<String> findings() {
        return result.map(given -> given.findings().items().stream()
                .sorted(Comparator.comparing(String::valueOf)).<String>map(ResultText.INSTANCE::of).toList())
                .orElse(List.of());
    }

    /** Its {@link #findings}, each saying so, with the excuse pleaded, if it is exempted. */
    public List<String> markedFindings() {
        return findings().stream()
                .map(finding -> exemption.map(one -> "exempted (" + one.excuse().getSimpleName() + "): " + finding)
                        .orElse(finding))
                .toList();
    }

    /**
     * Its findings, each shown without the packages of the classes it names, and saying so, with the excuse pleaded, if
     * it is exempted; then its notices; or why it failed.
     */
    public List<String> lines() {
        return result.map(given -> Stream.concat(markedFindings().stream(),
                        given.notices().items().stream().sorted(Comparator.comparing(String::valueOf))
                                .map(notice -> "notice: " + ResultText.INSTANCE.of(notice)))
                        .toList())
                .orElseGet(() -> failure.stream().toList());
    }
}
