package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.application.Application;

/**
 * Starts the bench: {@code Bench [root package [class directory or jar ...]]}. It is not an {@link Application}
 * itself, so it starts with JavaFX on the classpath as well as on the module path.
 */
public record Bench() implements StatelessFunctionalObject {

    /**
     * The JVM's entry point, which starts {@link BenchApp} at once. It is an instance method: the launcher makes the
     * bench with its constructor, then starts it.
     */
    public void main(final String[] args) {
        Application.launch(BenchApp.class, args);
    }
}
