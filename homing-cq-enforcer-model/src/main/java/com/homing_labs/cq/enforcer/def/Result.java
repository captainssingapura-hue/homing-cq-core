package com.homing_labs.cq.enforcer.def;

import com.homing_labs.cq.core.def.Language;
import com.homing_labs.cq.core.def.Rule;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.HashSet;
import java.util.Set;

/**
 * What checking one rule on one unit came to: the rule's findings, its notices, and what is worked out from them. One
 * type for every rule, so that what is worked out from findings is worked out once, here.
 */
public record Result<L extends Language, R extends Rule<?, L>>(Findings<L, R> findings, Notices notices)
        implements Scalar {

    /** A result with findings and nothing to notice. */
    public Result(final Findings<L, R> findings) {
        this(findings, new Notices(Set.of()));
    }

    /** Whether the rule found anything on the unit. */
    public boolean hasFindings() {
        return !findings.items().isEmpty();
    }

    /** This result with a notice added. */
    public Result<L, R> noting(final Notice notice) {
        final Set<Notice> all = new HashSet<>(notices.items());
        all.add(notice);
        return new Result<>(findings, new Notices(all));
    }

    /** This result and another of the same rule together: findings and notices joined, as a unit delegating to its
     * parts. */
    public Result<L, R> and(final Result<L, R> other) {
        final Set<Finding<L, R>> joined = new HashSet<>(findings.items());
        joined.addAll(other.findings().items());
        final Set<Notice> noted = new HashSet<>(notices.items());
        noted.addAll(other.notices().items());
        return new Result<>(new Findings<>(joined), new Notices(noted));
    }
}
