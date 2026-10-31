package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * A variable a field declaration declares: its name, brackets written after the name - an old form, which adds to the
 * declaration's type's dimensions (JLS 10.2) - and its initial value, as tokens, if it is given one.
 */
public record Declarator(Name name, TokenRange dimensions, Optional<TokenRange> initializer, TokenRange range)
        implements Scalar {
}
