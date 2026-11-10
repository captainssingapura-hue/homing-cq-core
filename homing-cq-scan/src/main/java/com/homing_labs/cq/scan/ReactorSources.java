package com.homing_labs.cq.scan;

import com.homing_labs.cq.lang.java.extract.JavaCodeBases;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The sources of the reactor a class - the anchor - was built in, declared alone, to compose with what else a check
 * declares: the {@code src/main/java} of every module of the reactor, so they are found even where the modules'
 * classes come from jars in a repository, as when a qc module is run alone. The anchor's classes must be where Maven
 * builds them, {@code <module>/target/classes} or a jar in {@code <module>/target}. The reactor is the highest
 * directory above the module that has a {@code pom.xml}, with one in every directory between; its modules are the
 * directories in it that have one, and the directories in those that have one, at any depth, so a module nested in
 * another is found wherever the anchor is. A module built alone has its own sources; classes anywhere else have none.
 */
public record ReactorSources(Class<?> anchor) implements Declaration {

    @Override
    public List<Path> sources() {
        final Path classes = Path.of(JavaCodeBases.INSTANCE.of(anchor).classes()).toAbsolutePath().normalize();
        final Optional<Path> module = Optional.ofNullable(classes.getParent())
                .filter(target -> target.getFileName() != null && target.getFileName().toString().equals("target"))
                .map(Path::getParent);
        if (module.isEmpty()) {
            return List.of();
        }
        final Optional<Path> reactor = Stream.iterate(module.get().getParent(),
                        folder -> folder != null && Files.isRegularFile(folder.resolve("pom.xml")), Path::getParent)
                .reduce((below, above) -> above);
        if (reactor.isEmpty()) {
            return Stream.of(module.get()).map(this::mainSources).filter(Files::isDirectory).toList();
        }
        return modulesIn(reactor.get()).map(this::mainSources).filter(Files::isDirectory).sorted().toList();
    }

    /** The directories in a folder that have a {@code pom.xml}, each followed by those in it, at any depth. */
    private Stream<Path> modulesIn(final Path folder) {
        final List<Path> modules;
        try (Stream<Path> children = Files.list(folder)) {
            modules = children.filter(child -> Files.isRegularFile(child.resolve("pom.xml"))).toList();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot list the modules in " + folder, e);
        }
        return modules.stream().flatMap(each -> Stream.concat(Stream.of(each), modulesIn(each)));
    }

    private Path mainSources(final Path module) {
        return module.resolve("src").resolve("main").resolve("java");
    }
}
