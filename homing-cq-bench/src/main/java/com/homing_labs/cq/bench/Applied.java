package com.homing_labs.cq.bench;

import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.enforcer.def.Result;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A rule applied to a unit: what its enforcer is for, and its result - or why it gave none, when it failed.
 */
public record Applied(EnforcerSignature signature, Optional<Result<?, ?>> result, Optional<String> failure)
        implements Scalar {

    /** Whether the unit complies with the rule: it was checked, and nothing was found. A notice is no finding. */
    public boolean complies() {
        return result.map(given -> !given.hasFindings()).orElse(false);
    }

    /** How many findings the rule has on the unit: none, when it failed. */
    public int findingCount() {
        return result.map(given -> given.findings().items().size()).orElse(0);
    }

    /** How the rule came out: its findings counted, {@code none}, or {@code failed}. */
    public String outcome() {
        return result.map(given -> given.hasFindings() ? String.valueOf(findingCount()) : "none").orElse("failed");
    }

    /**
     * Its findings, each shown without the packages of the classes it names, then its notices; or why it failed.
     */
    public List<String> lines() {
        return result.map(given -> Stream.concat(
                        given.findings().items().stream().map(Object::toString).sorted(),
                        given.notices().items().stream().map(notice -> "notice: " + notice).sorted())
                        .map(line -> line.replaceAll("(?:class )?(?:[a-z_][a-z0-9_]*\\.)+", ""))
                        .toList())
                .orElseGet(() -> failure.stream().toList());
    }
}
