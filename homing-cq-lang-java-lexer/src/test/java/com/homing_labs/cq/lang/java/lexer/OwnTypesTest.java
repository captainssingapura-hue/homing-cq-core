package com.homing_labs.cq.lang.java.lexer;

import hue.captains.singapura.tao.ontology.FunctionalObject;
import hue.captains.singapura.tao.ontology.Immutable;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Stateless;
import hue.captains.singapura.tao.ontology.ValueObject;
import hue.captains.singapura.tao.ontology.enforcer.ContractViolation;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every type of this module says what it is, and is held to it. */
class OwnTypesTest {

    @Test
    void everyTypeSaysWhatItIsAndIsHeldToIt() throws IOException, ClassNotFoundException {
        final List<Class<?>> types = types(Path.of("target/classes"));
        assertTrue(types.size() > 10, "found only " + types);
        final List<String> violations = new ArrayList<>();
        for (final Class<?> type : types) {
            if (type.isInterface()) {
                if (Stream.of(Stateless.class, FunctionalObject.class, ValueObject.class, Immutable.class,
                        Mutable.class).noneMatch(marker -> marker.isAssignableFrom(type))) {
                    violations.add(type.getName() + " extends no jOntology marker");
                }
            } else {
                new OntologyEnforcer().enforce(type).stream().map(ContractViolation::message)
                        .forEach(violations::add);
            }
        }
        assertEquals(List.of(), violations);
    }

    /** The types compiled into the folder. */
    static List<Class<?>> types(final Path classes) throws IOException, ClassNotFoundException {
        final List<Class<?>> types = new ArrayList<>();
        try (Stream<Path> files = Files.walk(classes)) {
            for (final Path file : files.filter(path -> path.toString().endsWith(".class")).sorted().toList()) {
                final String name = classes.relativize(file).toString().replace(File.separatorChar, '.');
                types.add(Class.forName(name.substring(0, name.length() - ".class".length())));
            }
        }
        return types;
    }
}
