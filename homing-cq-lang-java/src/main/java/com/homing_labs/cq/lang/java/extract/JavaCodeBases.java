package com.homing_labs.cq.lang.java.extract;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Optional;

/**
 * Finds a module's code base from one of its classes - the anchor - by the anchor's code source, and the module's
 * sources by Maven's layout: {@code <module>/target/classes}, or a jar in {@code <module>/target}, has its sources in
 * {@code <module>/src/main/java}, and {@code <module>/target/test-classes} has them in {@code <module>/src/test/java}.
 * Classes anywhere else, such as a jar in a repository, have no sources.
 *
 * <p>The anchor is named by the code that checks the module; the module itself knows nothing of this.</p>
 */
public record JavaCodeBases() implements StatelessFunctionalObject {

    public static final JavaCodeBases INSTANCE = new JavaCodeBases();

    /** The code base of the module the anchor was compiled in. */
    public JavaCodeBase of(final Class<?> anchor) {
        final CodeSource codeSource = anchor.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalArgumentException(anchor.getName()
                    + " has no code source: it comes with the JDK, not from a module that can be checked");
        }
        try {
            return at(Path.of(codeSource.getLocation().toURI()));
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(anchor.getName() + " has a code source that is not a file path: "
                    + codeSource.getLocation(), e);
        }
    }

    /** The code base whose compiled classes are at {@code classes}, a directory or a jar. */
    public JavaCodeBase at(final Path classes) {
        return new JavaCodeBase(classes.toUri(), sourcesBeside(classes).map(Path::toUri));
    }

    private Optional<Path> sourcesBeside(final Path classes) {
        final Path absolute = classes.toAbsolutePath().normalize();
        final Path target = absolute.getParent();
        if (target == null || target.getParent() == null || !target.getFileName().toString().equals("target")) {
            return Optional.empty();
        }
        final String name = absolute.getFileName().toString();
        final boolean classesFolder = Files.isDirectory(absolute) && name.equals("classes");
        final boolean testClassesFolder = Files.isDirectory(absolute) && name.equals("test-classes");
        final boolean builtJar = Files.isRegularFile(absolute) && name.endsWith(".jar");
        if (!classesFolder && !testClassesFolder && !builtJar) {
            return Optional.empty();
        }
        final Path sources = target.getParent().resolve("src").resolve(testClassesFolder ? "test" : "main")
                .resolve("java");
        return Files.isDirectory(sources) ? Optional.of(sources) : Optional.empty();
    }
}
