package com.homing_labs.cq.scan;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Two declarations as one, as {@link Declaration#and} composes them: what the first declares, then what the second
 * declares that the first does not.
 */
record ComposedDeclaration(Declaration first, Declaration second) implements Declaration {

    @Override
    public List<String> packages() {
        return united(first.packages(), second.packages());
    }

    @Override
    public List<Ruleset> rulesets() {
        return united(first.rulesets(), second.rulesets());
    }

    @Override
    public List<Path> sources() {
        return united(first.sources(), second.sources());
    }

    @Override
    public List<Exemptions<?>> exemptions() {
        return united(first.exemptions(), second.exemptions());
    }

    private <T> List<T> united(final List<T> firsts, final List<T> seconds) {
        return Stream.concat(firsts.stream(), seconds.stream()).distinct().toList();
    }
}
