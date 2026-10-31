package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;

/** Tokens, in the order they lie in the source text. */
public record Tokens(List<Token> items) implements Vector<Token> {

    public Tokens {
        items = List.copyOf(items);
    }
}
