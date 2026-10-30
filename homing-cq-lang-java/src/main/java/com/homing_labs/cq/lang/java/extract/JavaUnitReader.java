package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.def.JavaUnit;
import com.homing_labs.cq.lang.java.def.JavaUnits;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads the units of a module: every top-level class compiled into its code base, each loaded by its name without
 * being initialised, made a unit of its kind, and paired with the source file that declares it when the code base has
 * sources.
 *
 * <p>Build-time code: this is where classes are loaded by name. Each class file is first read with the Class-File API,
 * to tell a top-level class from a nested one and to learn which file declares it (its {@code SourceFile}), so only
 * top-level classes are loaded. {@code module-info} and {@code package-info} declare no class and are skipped, as is
 * everything under {@code META-INF}.</p>
 */
public record JavaUnitReader() implements StatelessFunctionalObject {

    public static final JavaUnitReader INSTANCE = new JavaUnitReader();

    /** The units of the module the anchor was compiled in, loaded by the anchor's class loader. */
    public List<JavaUnit> read(final Class<?> anchor) {
        return read(JavaCodeBases.INSTANCE.of(anchor), anchor.getClassLoader());
    }

    /** The units of a code base, sorted by class name, each class loaded by {@code loader}. */
    public List<JavaUnit> read(final JavaCodeBase base, final ClassLoader loader) {
        final Path classes = Path.of(base.classes());
        final Optional<Path> sources = base.sources().map(Path::of);
        if (Files.isDirectory(classes)) {
            return readUnder(classes, sources, loader);
        }
        try (FileSystem jar = FileSystems.newFileSystem(classes)) {
            return readUnder(jar.getPath("/"), sources, loader);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot open " + classes + " as a jar", e);
        }
    }

    private List<JavaUnit> readUnder(final Path root, final Optional<Path> sources, final ClassLoader loader) {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(file -> isClassFile(root, file))
                    .map(this::topLevel)
                    .flatMap(Optional::stream)
                    .map(found -> unit(found, sources, loader))
                    .sorted(Comparator.comparing(unit -> unit.type().getName()))
                    .toList();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot list the classes under " + root, e);
        }
    }

    private boolean isClassFile(final Path root, final Path file) {
        if (!Files.isRegularFile(file) || root.relativize(file).startsWith("META-INF")) {
            return false;
        }
        final String name = file.getFileName().toString();
        return name.endsWith(".class") && !name.equals("module-info.class") && !name.equals("package-info.class");
    }

    /** The class a class file declares, when it is a top-level class; nested classes belong to their top-level one. */
    private Optional<TopLevelClass> topLevel(final Path classFile) {
        final ClassModel model = ClassFile.of().parse(bytes(classFile));
        final String name = model.thisClass().asInternalName();
        final boolean nested = model.findAttribute(Attributes.innerClasses())
                .map(inner -> inner.classes().stream()
                        .anyMatch(entry -> entry.innerClass().asInternalName().equals(name)))
                .orElse(false);
        if (nested) {
            return Optional.empty();
        }
        final String sourceFile = model.findAttribute(Attributes.sourceFile())
                .map(attribute -> attribute.sourceFile().stringValue())
                .orElse(name.substring(name.lastIndexOf('/') + 1) + ".java");
        return Optional.of(new TopLevelClass(name, sourceFile));
    }

    private JavaUnit unit(final TopLevelClass found, final Optional<Path> sources, final ClassLoader loader) {
        final Class<?> type = load(found.binaryName(), loader);
        return JavaUnits.INSTANCE.of(type, sources
                .map(root -> root.resolve(found.packagePath()).resolve(found.sourceFile()))
                .filter(Files::isRegularFile)
                .map(file -> new JavaSource(file.toUri(), text(file))));
    }

    private Class<?> load(final String binaryName, final ClassLoader loader) {
        try {
            return Class.forName(binaryName, false, loader);
        } catch (final ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException(binaryName + " is compiled into the code base, but its class loader"
                    + " cannot load it", e);
        }
    }

    private byte[] bytes(final Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    private String text(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    /** A top-level class as its class file names it - {@code a/b/C} - and the name of the file that declares it. */
    private record TopLevelClass(String internalName, String sourceFile) implements Scalar {

        String binaryName() {
            return internalName.replace('/', '.');
        }

        String packagePath() {
            final int slash = internalName.lastIndexOf('/');
            return slash < 0 ? "" : internalName.substring(0, slash);
        }
    }
}
