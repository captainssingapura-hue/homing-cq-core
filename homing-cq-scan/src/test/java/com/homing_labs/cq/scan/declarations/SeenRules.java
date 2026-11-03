package com.homing_labs.cq.scan.declarations;

import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.PackageRuleset;
import com.homing_labs.cq.scan.Ruleset;

import java.util.List;

/** The test rules, which see every unit, declared alone; it has no {@code INSTANCE}, so it is made. */
public record SeenRules() implements Declaration {

    @Override
    public List<Ruleset> rulesets() {
        return List.of(new PackageRuleset("com.homing_labs.cq.scan.rules"));
    }
}
