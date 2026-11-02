package com.homing_labs.cq.demo;

import com.homing_labs.cq.bench.Bench;
import com.homing_labs.cq.scan.Ruleset;

import java.util.List;

/**
 * The bench over the framework and this demo - all the main code under {@code com.homing_labs.cq}, the bench's own
 * included - checked by the {@link DemoRules}. {@code mvn javafx:run} in this module starts it.
 */
public final class DemoBench extends Bench {

    @Override
    protected List<String> packages() {
        return List.of("com.homing_labs.cq");
    }

    @Override
    protected List<Ruleset> rulesets() {
        return List.of(DemoRules.INSTANCE);
    }
}
