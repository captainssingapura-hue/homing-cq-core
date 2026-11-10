package com.homing_labs.cq.lang.java.model;

import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Stateless;

/**
 * Where in its class's code a local or anonymous class is declared, as its class file says: in a method, in a
 * constructor, or in neither - a field's initialiser or an initialiser block, static or instance, which the class file
 * does not tell apart. A class declared in a lambda is where the lambda is, not in the method javac makes of its body.
 */
public sealed interface Enclosure extends Scalar {

    /** In a method, by its name and parameter types. */
    record InMethod(String name, Parameters parameters) implements Enclosure {
    }

    /** In a constructor, by its parameter types. */
    record InConstructor(Parameters parameters) implements Enclosure {
    }

    /** In a field's initialiser or an initialiser block. */
    record InInitialiser() implements Enclosure, Stateless {

        public static final InInitialiser INSTANCE = new InInitialiser();
    }
}
