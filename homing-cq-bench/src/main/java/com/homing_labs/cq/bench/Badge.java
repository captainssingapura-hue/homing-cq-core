package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.Scalar;
import javafx.scene.paint.Color;

/**
 * A badge beside an entry of the bench's indexes: a short text, what it means in words, and its colour - filled for a
 * class's kind and for compliance, outlined for how a class exists.
 */
public record Badge(String text, String meaning, String colour, boolean filled) implements Scalar {

    /** The colour of its text when it is filled: dark on a light colour, such as yellow, and white on any other. */
    public String ink() {
        final Color fill = Color.web(colour);
        return 0.299 * fill.getRed() + 0.587 * fill.getGreen() + 0.114 * fill.getBlue() > 0.6 ? "#1f1f1f" : "white";
    }
}
