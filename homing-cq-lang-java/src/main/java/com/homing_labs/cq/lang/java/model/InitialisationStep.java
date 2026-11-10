package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.outline.CodeBlock;
import hue.captains.singapura.tao.ontology.Scalar;

/**
 * A step of how a class is initialised, as its source writes it: a piece javac puts into the class file's static
 * initialiser, which runs them in the order written. Each is written in a block of code.
 */
public sealed interface InitialisationStep extends Scalar {

    /** The code the step is written in. */
    CodeBlock block();

    /** A static field's initial value: the field, its declarator - the name and the value - and the value alone. */
    record InitialValue(String field, CodeBlock block, CodeBlock value) implements InitialisationStep {
    }

    /** An enum's constant, made with the arguments it is written with, and its own class body. */
    record EnumConstant(String name, CodeBlock block) implements InitialisationStep {
    }

    /** A static block. */
    record StaticBlock(CodeBlock block) implements InitialisationStep {
    }
}
