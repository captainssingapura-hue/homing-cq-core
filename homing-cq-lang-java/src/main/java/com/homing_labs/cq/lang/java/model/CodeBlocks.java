package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.outline.CodeBlock;
import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** Blocks of code copied out of a source file, in the order they are written there. */
public record CodeBlocks(List<CodeBlock> items) implements Vector<CodeBlock> {

    public CodeBlocks {
        items = List.copyOf(items);
    }
}
