package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.outline.CodeBlock;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A parameter of a method or a constructor as the source writes it: its name, which the class file keeps only when it
 * is compiled to, and its code - its modifiers, its type and its name.
 */
public record WrittenParameter(String name, CodeBlock block) implements Scalar {
}
