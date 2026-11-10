package com.homing_labs.cq.scan;

import com.homing_labs.cq.enforcer.def.Enforcer;
import hue.captains.singapura.tao.ontology.FunctionalObject;

import java.util.List;

/**
 * Rules to check units by, under a name: the enforcer of each, as the class loader of the code scanned finds them. A
 * ruleset may list its enforcers itself, or find them, as a {@link PackageRuleset} does under a package.
 */
public interface Ruleset extends FunctionalObject {

    /** What the ruleset is called, where the bench shows it. */
    String name();

    /** The enforcers of the rules, each once, as {@code loader} finds them. */
    List<Enforcer<?, ?, ?>> enforcers(ClassLoader loader);
}
