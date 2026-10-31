package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Where a member came from: the source declared it, or the compiler generated it for its own ends - a lambda's body, a
 * bridge, a switch map. A member the language implies from a declaration, such as a default constructor, counts as
 * declared until the source can be compared with the class file.
 */
public sealed interface Origin extends Stateless {

    record Declared() implements Origin {
        public static final Declared INSTANCE = new Declared();
    }

    record Generated() implements Origin {
        public static final Generated INSTANCE = new Generated();
    }
}
