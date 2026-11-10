package com.homing_labs.cq.scan;

import com.homing_labs.cq.scan.declarations.Lone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclarationTest {

    private static final Ruleset FIRST = new PackageRuleset("first.rules");

    private static final Ruleset SECOND = new PackageRuleset("second.rules");

    @Test
    void aDeclarationDeclaresNothingItDoesNotSay() {
        final Declaration nothing = new Declaration() {
        };
        assertEquals(List.of(), nothing.packages());
        assertEquals(List.of(), nothing.rulesets());
        assertEquals(List.of(), nothing.sources());
        assertEquals(List.of(), nothing.exemptions());
    }

    @Test
    void composedDeclarationsDeclareWhatEitherDeclaresOnceFirstThingsFirst() {
        final Declaration composed = declared(List.of("a", "b"), List.of(FIRST), List.of(Path.of("x")))
                .and(declared(List.of("b", "c"), List.of(SECOND, FIRST), List.of(Path.of("y"), Path.of("x"))));
        assertEquals(List.of("a", "b", "c"), composed.packages());
        assertEquals(List.of(FIRST, SECOND), composed.rulesets());
        assertEquals(List.of(Path.of("x"), Path.of("y")), composed.sources());
    }

    @Test
    void aReactorsSourcesAreThoseOfEachOfItsModules() throws URISyntaxException {
        final List<Path> sources = new ReactorSources(Scanner.class).sources();
        final Path module = Path.of(Scanner.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toAbsolutePath().normalize().getParent().getParent();
        assertTrue(sources.contains(module.resolve("src/main/java")), "the anchor's own module");
        assertTrue(sources.contains(module.resolveSibling("homing-cq-lang-java").resolve("src/main/java")),
                "a module beside it");
        sources.forEach(source -> assertTrue(Files.isRegularFile(source.getParent().getParent().getParent()
                .resolve("pom.xml")), source + " is a module's"));
    }

    @Test
    void classesFromARepositoryHaveNoReactorSources() {
        assertEquals(List.of(), new ReactorSources(Test.class).sources());
    }

    @Test
    void aModuleBuiltAloneHasItsOwnSourcesAndOneOfAReactorThoseOfEveryModule(@TempDir final Path root)
            throws Exception {
        final Path module = root.resolve("module");
        final Path classes = module.resolve("target/classes");
        final Path own = Files.createDirectories(module.resolve("src/main/java"));
        copyLone(classes);
        assertEquals(List.of(own), sourcesAnchoredIn(classes));

        Files.writeString(module.resolve("pom.xml"), "<project/>");
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        final Path other = Files.createDirectories(root.resolve("other/src/main/java"));
        Files.writeString(root.resolve("other/pom.xml"), "<project/>");
        Files.createDirectories(root.resolve("no-module/src/main/java"));
        assertEquals(List.of(own, other), sourcesAnchoredIn(classes));
    }

    @Test
    void aNestedModulesReactorIsTheHighestWithAPomAndHasItsModulesAtAnyDepth(@TempDir final Path root)
            throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        module(root.resolve("beside"));
        final Path reactor = Files.createDirectories(root.resolve("plain/reactor"));
        Files.writeString(reactor.resolve("pom.xml"), "<project/>");
        final Path top = module(reactor.resolve("top"));
        Files.createDirectories(reactor.resolve("nest"));
        Files.writeString(reactor.resolve("nest/pom.xml"), "<project/>");
        final Path nested = module(reactor.resolve("nest/module"));
        final Path sibling = module(reactor.resolve("nest/sibling"));
        Files.createDirectories(reactor.resolve("loose/src/main/java"));
        module(reactor.resolve("loose/inner"));
        final Path classes = reactor.resolve("nest/module/target/classes");
        copyLone(classes);
        assertEquals(List.of(nested, sibling, top), sourcesAnchoredIn(classes));
    }

    /** A module with a {@code pom.xml} and main sources at {@code folder}: its sources. */
    private Path module(final Path folder) throws IOException {
        final Path sources = Files.createDirectories(folder.resolve("src/main/java"));
        Files.writeString(folder.resolve("pom.xml"), "<project/>");
        return sources;
    }

    private void copyLone(final Path classes) throws IOException, URISyntaxException {
        final String file = Lone.class.getName().replace('.', '/') + ".class";
        final Path built = Path.of(Lone.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve(file);
        Files.createDirectories(classes.resolve(file).getParent());
        Files.copy(built, classes.resolve(file));
    }

    private List<Path> sourcesAnchoredIn(final Path classes) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            return new ReactorSources(Class.forName(Lone.class.getName(), false, loader)).sources();
        }
    }

    private Declaration declared(final List<String> packages, final List<Ruleset> rulesets, final List<Path> sources) {
        return new Declaration() {
            @Override
            public List<String> packages() {
                return packages;
            }

            @Override
            public List<Ruleset> rulesets() {
                return rulesets;
            }

            @Override
            public List<Path> sources() {
                return sources;
            }
        };
    }
}
