package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;

/**
 * An annotation applied to a parameter of a method or a constructor, by the parameter's position, counted from zero
 * among the parameters the class file annotates. javac leaves out a parameter it adds itself, such as an inner class's
 * outer instance, so a position can be one short of the parameter's place in the descriptor.
 */
public record ParameterAnnotation(int position, AppliedAnnotation annotation) implements Scalar {
}
