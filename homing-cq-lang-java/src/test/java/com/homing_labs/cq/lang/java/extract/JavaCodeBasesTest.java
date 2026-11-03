package com.homing_labs.cq.lang.java.extract;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaCodeBasesTest {

    @Test
    void findsSourcesBesideAClassesFolder(@TempDir final Path module) throws IOException {
        final Path classes = Files.createDirectories(module.resolve("target/classes"));
        final Path sources = Files.createDirectories(module.resolve("src/main/java"));

        assertEquals(new JavaCodeBase(classes.toUri(), new SourceDirectories(sources.toUri())),
                JavaCodeBases.INSTANCE.at(classes));
    }

    @Test
    void findsTestSourcesBesideATestClassesFolder(@TempDir final Path module) throws IOException {
        final Path classes = Files.createDirectories(module.resolve("target/test-classes"));
        Files.createDirectories(module.resolve("src/main/java"));
        final Path sources = Files.createDirectories(module.resolve("src/test/java"));

        assertEquals(new JavaCodeBase(classes.toUri(), new SourceDirectories(sources.toUri())),
                JavaCodeBases.INSTANCE.at(classes));
    }

    @Test
    void findsSourcesBesideAJarBuiltInTarget(@TempDir final Path module) throws IOException {
        Files.createDirectories(module.resolve("target"));
        final Path jar = Files.createFile(module.resolve("target/module-1.0.jar"));
        final Path sources = Files.createDirectories(module.resolve("src/main/java"));

        assertEquals(new JavaCodeBase(jar.toUri(), new SourceDirectories(sources.toUri())),
                JavaCodeBases.INSTANCE.at(jar));
    }

    @Test
    void aJarInARepositoryHasNoSources(@TempDir final Path repository) throws IOException {
        Files.createDirectories(repository.resolve("module/1.0"));
        Files.createDirectories(repository.resolve("module/src/main/java"));
        final Path jar = Files.createFile(repository.resolve("module/1.0/module-1.0.jar"));

        assertEquals(new JavaCodeBase(jar.toUri(), new SourceDirectories()), JavaCodeBases.INSTANCE.at(jar));
    }

    @Test
    void aClassesFolderWithNoSourcesFolderHasNoSources(@TempDir final Path module) throws IOException {
        final Path classes = Files.createDirectories(module.resolve("target/classes"));

        assertEquals(new JavaCodeBase(classes.toUri(), new SourceDirectories()), JavaCodeBases.INSTANCE.at(classes));
    }

    @Test
    void aJdkClassIsRefused() {
        final IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> JavaCodeBases.INSTANCE.of(String.class));
        assertEquals("java.lang.String has no code source: it comes with the JDK, not from a module that can be checked",
                refused.getMessage());
    }
}
