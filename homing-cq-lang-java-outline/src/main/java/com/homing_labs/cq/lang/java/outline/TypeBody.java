package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Scalar;

/** A type's body as tokens, its braces included, and how it ends. */
record TypeBody(TokenRange range, BodyEnd end) implements Scalar {

    /** The tokens between the braces: up to the end, when no brace closes it. */
    TokenRange inside() {
        return new TokenRange(range.from() + 1, end instanceof BodyEnd.Closed ? range.to() - 1 : range.to());
    }
}
