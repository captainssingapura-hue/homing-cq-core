package com.homing_labs.cq.demo;

import com.homing_labs.cq.bench.Bench;
import com.homing_labs.cq.lang.java.extract.JavaCodeBases;
import com.homing_labs.cq.scan.Ruleset;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The bench over the framework and this demo - all the main code under {@code com.homing_labs.cq}, the bench's own
 * included - checked by the {@link DemoRules}. {@code mvn javafx:run} in this module starts it.
 */
public final class DemoBench extends Bench {

    @Override
    protected List<String> packages() {
        return List.of("com.homing_labs.cq");
    }

    @Override
    protected List<Ruleset> rulesets() {
        return List.of(DemoRules.INSTANCE);
    }

    /**
     * The sources of every module of the reactor this demo is built in, so that they are found even when the
     * framework's classes come from jars in a repository: the {@code src/main/java} of each directory beside this
     * module's own. Run from anywhere else, there are none.
     */
    @Override
    protected List<Path> sources() {
        final Path classes = Path.of(JavaCodeBases.INSTANCE.of(DemoBench.class).classes()).toAbsolutePath();
        final Optional<Path> reactor = Optional.ofNullable(classes.getParent()).map(Path::getParent)
                .map(Path::getParent);
        if (reactor.isEmpty() || !Files.isDirectory(reactor.get())) {
            return List.of();
        }
        try (Stream<Path> modules = Files.list(reactor.get())) {
            return modules.map(module -> module.resolve("src").resolve("main").resolve("java"))
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot list the modules in " + reactor.get(), e);
        }
    }
}
