package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.FunctionalObject;

import java.nio.file.Path;
import java.util.List;

/**
 * What a check covers, as the code that checks declares it: the root packages to scan, the rulesets to check them by,
 * and the directories to look for their sources in first. A declaration may declare any of the three alone - a pack
 * of rules its rulesets, a module its packages, {@link ReactorSources} its sources - and declarations compose,
 * {@link #and}, into the one a check is given. The bench and a build's {@link Gate} take the same declaration.
 */
public interface Declaration extends FunctionalObject {

    /** The root packages to scan, each with the packages below it: none, unless declared. */
    default List<String> packages() {
        return List.of();
    }

    /** The rulesets to check the units by: none, unless declared. */
    default List<Ruleset> rulesets() {
        return List.of();
    }

    /**
     * The directories to look for the units' sources in first, in order: none, unless declared, and a unit's source
     * is looked for beside its classes alone, as Maven lays a module out.
     */
    default List<Path> sources() {
        return List.of();
    }

    /**
     * This declaration and {@code other} as one: the packages and the rulesets of both, each once, this one's first;
     * and the source directories of both, in order, so this one's are looked in first.
     */
    default Declaration and(final Declaration other) {
        return new ComposedDeclaration(this, other);
    }
}
