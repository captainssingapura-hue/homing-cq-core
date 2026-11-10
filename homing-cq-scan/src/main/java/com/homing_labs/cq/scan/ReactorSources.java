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
 * builds them, {@code <module>/target/classes} or a jar in {@code <module>/target}; the reactor is the directory
 * above the module, when it has a {@code pom.xml}, and its modules are the directories in it that have one. A module
 * built alone has its own sources; classes anywhere else have none.
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
        final Optional<Path> reactor = Optional.ofNullable(module.get().getParent())
                .filter(parent -> Files.isRegularFile(parent.resolve("pom.xml")));
        if (reactor.isEmpty()) {
            return Stream.of(module.get()).map(this::mainSources).filter(Files::isDirectory).toList();
        }
        try (Stream<Path> modules = Files.list(reactor.get())) {
            return modules.filter(each -> Files.isRegularFile(each.resolve("pom.xml")))
                    .map(this::mainSources)
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot list the modules in " + reactor.get(), e);
        }
    }

    private Path mainSources(final Path module) {
        return module.resolve("src").resolve("main").resolve("java");
    }
}
