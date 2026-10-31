package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A badge beside an entry of the bench's indexes: a short text, what it means in words, and its colour - filled for a
 * class's kind, outlined for how it exists.
 */
public record Badge(String text, String meaning, String colour, boolean filled) implements Scalar {
}
