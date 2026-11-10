package com.homing_labs.cq.scan;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.extract.JavaCodeBase;
import com.homing_labs.cq.lang.java.extract.JavaCodeBases;
import com.homing_labs.cq.lang.java.extract.JavaUnitReader;
import com.homing_labs.cq.lang.java.extract.SourceDirectories;
import com.homing_labs.cq.lang.java.extract.SourceDirectory;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the units under a root package from the classpath: every top-level class in the package or in a package below
 * it, in each code base - a directory or a jar - the class loader finds the package in. A directory is read from the
 * package down; a jar is read whole, and its units outside the package dropped. Where two code bases have a class of
 * the same name, the first the class loader finds is kept. Several root packages may be read together, and main code
 * alone: not a test classes directory, nor a jar of tests. A unit's source is looked for in the source directories
 * given, then in those beside its code base, as Maven lays a module out.
 */
public record ClasspathUnits() implements StatelessFunctionalObject {

    public static final ClasspathUnits INSTANCE = new ClasspathUnits();

    /** The units under {@code rootPackage}, sorted by class name, each loaded by {@code loader}. */
    public List<JavaUnit> read(final String rootPackage, final ClassLoader loader) {
        return read(rootPackage, loader, false, new SourceDirectories());
    }

    /**
     * The units under any of {@code rootPackages}, each once - one root may be under another - sorted by class name.
     */
    public List<JavaUnit> read(final List<String> rootPackages, final ClassLoader loader) {
        return read(rootPackages, loader, false, new SourceDirectories());
    }

    /**
     * The units of main code under any of {@code rootPackages}, each once, sorted by class name: those of a test
     * classes directory, {@code target/test-classes}, or of a jar of tests, {@code *-tests.jar}, are left out. Each
     * unit's source is looked for in {@code sources} first, in order.
     */
    public List<JavaUnit> main(final List<String> rootPackages, final List<Path> sources, final ClassLoader loader) {
        return read(rootPackages, loader, true, new SourceDirectories(sources.stream()
                .map(directory -> new SourceDirectory(directory.toUri())).toList()));
    }

    private List<JavaUnit> read(final List<String> rootPackages, final ClassLoader loader, final boolean mainOnly,
                                final SourceDirectories sources) {
        final Map<String, JavaUnit> units = new LinkedHashMap<>();
        rootPackages.forEach(rootPackage -> read(rootPackage, loader, mainOnly, sources)
                .forEach(unit -> units.putIfAbsent(unit.type().getName(), unit)));
        return units.values().stream().sorted(Comparator.comparing(unit -> unit.type().getName())).toList();
    }

    private List<JavaUnit> read(final String rootPackage, final ClassLoader loader, final boolean mainOnly,
                                final SourceDirectories sources) {
        final String path = rootPackage.replace('.', '/');
        final Map<String, JavaUnit> units = new LinkedHashMap<>();
        for (final URL found : packages(path, loader)) {
            final Path classes = classes(found, path);
            if (mainOnly && tests(classes)) {
                continue;
            }
            JavaUnitReader.INSTANCE.read(codeBase(found, classes, sources), loader).stream()
                    .filter(unit -> isUnder(unit.type().getPackageName(), rootPackage))
                    .forEach(unit -> units.putIfAbsent(unit.type().getName(), unit));
        }
        return units.values().stream().sorted(Comparator.comparing(unit -> unit.type().getName())).toList();
    }

    private List<URL> packages(final String path, final ClassLoader loader) {
        try {
            return Collections.list(loader.getResources(path));
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot look for " + path + " on the classpath", e);
        }
    }

    /** Where the classes a package was found among are: the jar, or the directory the package's path is in. */
    private Path classes(final URL found, final String path) {
        try {
            if (found.getProtocol().equals("jar")) {
                return Path.of(((JarURLConnection) found.openConnection()).getJarFileURL().toURI());
            }
            Path classes = Path.of(found.toURI());
            for (int depth = path.split("/").length; depth > 0; depth--) {
                classes = classes.getParent();
            }
            return classes;
        } catch (final IOException | URISyntaxException e) {
            throw new IllegalStateException("Cannot read the code base of " + found, e);
        }
    }

    /**
     * The code base a package was found in: a jar whole, or the package's own directory - its sources in the
     * directories given, then in those beside its classes.
     */
    private JavaCodeBase codeBase(final URL found, final Path classes, final SourceDirectories given) {
        final SourceDirectories sources = given.then(JavaCodeBases.INSTANCE.at(classes).sources());
        if (found.getProtocol().equals("jar")) {
            return new JavaCodeBase(classes.toUri(), sources);
        }
        try {
            return new JavaCodeBase(Path.of(found.toURI()).toUri(), sources);
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Cannot read the code base of " + found, e);
        }
    }

    /** Whether the classes are tests: Maven's test classes directory, or a jar of tests. */
    private boolean tests(final Path classes) {
        final String name = classes.getFileName().toString();
        return name.equals("test-classes") || name.endsWith("-tests.jar");
    }

    private boolean isUnder(final String packageName, final String rootPackage) {
        return packageName.equals(rootPackage) || packageName.startsWith(rootPackage + ".");
    }
}
