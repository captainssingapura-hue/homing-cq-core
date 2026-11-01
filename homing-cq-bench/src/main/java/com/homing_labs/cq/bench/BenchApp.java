package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.Mutable;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

/**
 * The bench as a JavaFX application, in a window of its own. Its arguments are the root package to scan, or several
 * apart by commas - {@code com.homing_labs.cq} when there is none - then any class directories or jars to scan beside
 * the classpath it runs with. It checks the units by the rules under {@code com.homing_labs.cq}. {@link Bench} starts
 * it.
 */
public final class BenchApp extends Application implements Mutable {

    @Override
    public void start(final Stage stage) {
        final List<String> arguments = getParameters().getRaw();
        final String rootPackage = arguments.isEmpty() ? "com.homing_labs.cq" : arguments.getFirst();
        stage.setTitle("CQ Bench");
        final ClassLoader loader = loader(arguments.stream().skip(1).toList());
        stage.setScene(new Scene(BenchPane.INSTANCE.content(rootPackage, "com.homing_labs.cq", loader,
                List.of(new ClassObjectView(), new RulesView()), new SourceView()), 1600, 900));
        stage.show();
    }

    /** The bench's own class loader, or one that also loads from the class directories and jars given. */
    private ClassLoader loader(final List<String> classRoots) {
        if (classRoots.isEmpty()) {
            return BenchApp.class.getClassLoader();
        }
        final URL[] urls = classRoots.stream().map(root -> {
            try {
                return Path.of(root).toAbsolutePath().toUri().toURL();
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException("Not a class directory or jar: " + root, e);
            }
        }).toArray(URL[]::new);
        return new URLClassLoader("bench", urls, BenchApp.class.getClassLoader());
    }
}
