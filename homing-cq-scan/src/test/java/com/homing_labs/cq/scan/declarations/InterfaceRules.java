package com.homing_labs.cq.scan.declarations;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Ruleset;
import com.homing_labs.cq.scan.rules.InterfacesAreSeenEnforcer;

import java.util.List;

/** A ruleset that sees interfaces alone, and declares itself, to compose with the code it checks. */
public record InterfaceRules() implements Ruleset, Declaration {

    public static final InterfaceRules INSTANCE = new InterfaceRules();

    @Override
    public String name() {
        return "interfaces";
    }

    @Override
    public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
        return List.of(new InterfacesAreSeenEnforcer());
    }

    @Override
    public List<Ruleset> rulesets() {
        return List.of(this);
    }
}
