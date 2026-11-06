package com.homing_labs.cq.bench;

import com.homing_labs.cq.scan.Declaration;
import com.homing_labs.cq.scan.NamedDeclarations;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

/**
 * The bench: shows the code a declaration covers, checked by its rulesets - the same declaration a build's
 * {@link com.homing_labs.cq.scan.Gate} takes. Code hands it a declaration; run as a program, its arguments name the
 * declarations to compose. It runs with the code it shows on its classpath, and starts JavaFX itself rather than
 * being an {@link javafx.application.Application}, so it starts with JavaFX on the classpath as well as on the module
 * path.
 */
public record Bench() implements StatelessFunctionalObject {

    public static final Bench INSTANCE = new Bench();

    /** Shows the bench over what {@code declaration} covers, in a window of its own. */
    public void start(final Declaration declaration) {
        Platform.startup(() -> show(declaration, new Stage()));
    }

    /**
     * The JVM's entry point: shows the bench over the declarations its arguments name, composed in order. It is an
     * instance method: the launcher makes the bench, then starts it.
     */
    public void main(final String[] args) {
        start(NamedDeclarations.INSTANCE.of(List.of(args), getClass().getClassLoader()));
    }

    private void show(final Declaration declaration, final Stage stage) {
        BenchIcon.INSTANCE.onEveryWindow();
        stage.setTitle("CQ Bench");
        stage.setScene(new Scene(BenchPane.INSTANCE.content(String.join(", ", declaration.packages()),
                declaration.rulesets(), declaration.sources(), getClass().getClassLoader(),
                List.of(new ClassObjectView(), new RulesView()), new SourceView()), 1600, 900));
        stage.show();
    }
}
