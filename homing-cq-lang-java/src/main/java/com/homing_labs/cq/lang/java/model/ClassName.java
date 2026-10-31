package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A class by its binary name - {@code a.b.Outer$Inner} - which names it without loading it, so the same model can be
 * built from a class file or from source.
 */
public record ClassName(String binaryName) implements Scalar {

    /** The name without its package: {@code Outer$Inner}. */
    public String simpleName() {
        return binaryName.substring(binaryName.lastIndexOf('.') + 1);
    }

    /** The package, or the empty string for the unnamed package. */
    public String packageName() {
        final int dot = binaryName.lastIndexOf('.');
        return dot < 0 ? "" : binaryName.substring(0, dot);
    }
}
