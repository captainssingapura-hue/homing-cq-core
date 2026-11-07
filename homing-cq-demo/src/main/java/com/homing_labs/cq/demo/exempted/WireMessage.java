package com.homing_labs.cq.demo.exempted;

import hue.captains.singapura.tao.ontology.Mutable;

/**
 * A message as a serialiser fills it in, and the one class in this code that breaks a basic rule, on purpose: its field
 * is public, so {@code NoPublicFields} finds it, and {@link WireMessageExemptions} lets the class off. It is here to
 * show an exemption at work.
 */
public final class WireMessage implements Mutable {

    /** What the message says. */
    public String body;
}
