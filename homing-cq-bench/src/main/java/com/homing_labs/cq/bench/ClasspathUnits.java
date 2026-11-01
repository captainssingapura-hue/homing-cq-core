package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.extract.JavaCodeBase;
import com.homing_labs.cq.lang.java.extract.JavaCodeBases;
import com.homing_labs.cq.lang.java.extract.JavaUnitReader;
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
 * the same name, the first the class loader finds is kept. Several root packages may be read together.
 */
public record ClasspathUnits() implements StatelessFunctionalObject {

    public static final ClasspathUnits INSTANCE = new ClasspathUnits();

    /** The units under {@code rootPackage}, sorted by class name, each loaded by {@code loader}. */
    public List<JavaUnit> read(final String rootPackage, final ClassLoader loader) {
        final String path = rootPackage.replace('.', '/');
        final Map<String, JavaUnit> units = new LinkedHashMap<>();
        for (final URL found : packages(path, loader)) {
            JavaUnitReader.INSTANCE.read(codeBase(found, path), loader).stream()
                    .filter(unit -> isUnder(unit.type().getPackageName(), rootPackage))
                    .forEach(unit -> units.putIfAbsent(unit.type().getName(), unit));
        }
        return units.values().stream().sorted(Comparator.comparing(unit -> unit.type().getName())).toList();
    }

    /**
     * The units under any of {@code rootPackages}, each once - one root may be under another - sorted by class name.
     */
    public List<JavaUnit> read(final List<String> rootPackages, final ClassLoader loader) {
        final Map<String, JavaUnit> units = new LinkedHashMap<>();
        rootPackages.forEach(rootPackage -> read(rootPackage, loader)
                .forEach(unit -> units.putIfAbsent(unit.type().getName(), unit)));
        return units.values().stream().sorted(Comparator.comparing(unit -> unit.type().getName())).toList();
    }

    private List<URL> packages(final String path, final ClassLoader loader) {
        try {
            return Collections.list(loader.getResources(path));
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot look for " + path + " on the classpath", e);
        }
    }

    /** The code base a package was found in: the package's own directory, with the sources of the whole directory. */
    private JavaCodeBase codeBase(final URL found, final String path) {
        try {
            if (found.getProtocol().equals("jar")) {
                final URL jar = ((JarURLConnection) found.openConnection()).getJarFileURL();
                return JavaCodeBases.INSTANCE.at(Path.of(jar.toURI()));
            }
            final Path packageDirectory = Path.of(found.toURI());
            Path classes = packageDirectory;
            for (int depth = path.split("/").length; depth > 0; depth--) {
                classes = classes.getParent();
            }
            return new JavaCodeBase(packageDirectory.toUri(), JavaCodeBases.INSTANCE.at(classes).sources());
        } catch (final IOException | URISyntaxException e) {
            throw new IllegalStateException("Cannot read the code base of " + found, e);
        }
    }

    private boolean isUnder(final String packageName, final String rootPackage) {
        return packageName.equals(rootPackage) || packageName.startsWith(rootPackage + ".");
    }
}
