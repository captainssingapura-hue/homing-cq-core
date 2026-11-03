package com.homing_labs.cq.bench;

import com.homing_labs.cq.scan.Ruleset;
import hue.captains.singapura.tao.ontology.Stateless;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

/**
 * The bench, for the code it checks to extend: a subclass names the packages to scan and the rulesets to check them
 * by - and, if it likes, the directories their sources are in - and runs as a program of its own, with that code on
 * its classpath. It starts JavaFX itself rather than being an {@link javafx.application.Application}, so it starts
 * with JavaFX on the classpath as well as on the module path.
 */
public abstract class Bench implements Stateless {

    /** The root packages to scan, each with the packages below it. */
    protected abstract List<String> packages();

    /** The rulesets to check the units by. */
    protected abstract List<Ruleset> rulesets();

    /**
     * The directories to look for the units' sources in first, in order - a build's source roots, say: none, by
     * default, and a unit's source is looked for beside its classes alone, as Maven lays a module out.
     */
    protected List<Path> sources() {
        return List.of();
    }

    /**
     * The JVM's entry point, which each subclass inherits: shows the bench in a window of its own. It is an instance
     * method: the launcher makes the subclass with its constructor, then starts it.
     */
    public void main(final String[] args) {
        Platform.startup(() -> show(new Stage()));
    }

    private void show(final Stage stage) {
        stage.setTitle("CQ Bench");
        stage.setScene(new Scene(BenchPane.INSTANCE.content(String.join(", ", packages()), rulesets(), sources(),
                getClass().getClassLoader(), List.of(new ClassObjectView(), new RulesView()), new SourceView()),
                1600, 900));
        stage.show();
    }
}
