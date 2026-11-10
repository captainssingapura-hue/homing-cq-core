package com.homing_labs.cq.demo.exempted;

import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.Exemptions;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * The demo's exemptions, declared apart from its code and from the rules they answer to, and composed with them as a
 * project's would be: the bench, the report and the self-check all take the three together. The class let off still
 * counts as not complying, so the bench's dial stops one step short of the green: an exemption changes no code.
 */
public record DemoExemptions() implements Declaration, StatelessFunctionalObject {

    public static final DemoExemptions INSTANCE = new DemoExemptions();

    @Override
    public List<Exemptions<?>> exemptions() {
        return List.of(WireMessageExemptions.INSTANCE);
    }
}
