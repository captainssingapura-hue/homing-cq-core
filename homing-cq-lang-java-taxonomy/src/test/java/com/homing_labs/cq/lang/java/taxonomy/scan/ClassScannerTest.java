package com.homing_labs.cq.lang.java.taxonomy.scan;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.Dependency;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassScannerTest {

    private static final String FIXTURE = "com/homing_labs/cq/lang/java/taxonomy/scan/DependencyFixture";

    private static final String LEAF = FIXTURE + "$Leaf";

    @Test
    void objectIsAClassWithNoSuperclass() throws IOException {
        final List<Occurrence> found = ClassScanner.INSTANCE.scan(classFile(Object.class));
        assertEquals(new Occurrence.OfClass("java/lang/Object", ClassKind.Class.INSTANCE), found.getFirst());
        assertTrue(found.stream().noneMatch(occurrence -> occurrence.kind() instanceof Superclass));
    }

    @Test
    void eachKindOfDependencyIsFoundWhereItIsNamed() throws IOException {
        final Map<Class<?>, Set<String>> found = dependencies(DependencyFixture.class);
        assertEquals(Set.of("java/net/HttpURLConnection"), found.get(Dependency.InlinedConstant.class));
        assertEquals(Set.of("java/lang/Deprecated"), found.get(Dependency.Annotation.class));
        assertEquals(Set.of(LEAF), found.get(Dependency.PermittedSubclass.class));
        assertEquals(Set.of(LEAF), found.get(Dependency.Nesting.class));
        assertTrue(found.get(Dependency.Signature.class).contains("java/time/Duration"));
        assertTrue(found.get(Dependency.Code.class).contains("java/util/ArrayList"));
        assertTrue(found.get(Dependency.LocalVariable.class).contains("java/time/Instant"));
    }

    @Test
    void aLocalsTypeArgumentIsNamedOnlyInTheDebugTables() throws IOException {
        final Map<Class<?>, Set<String>> found = dependencies(DependencyFixture.class);
        assertEquals(Set.of(Dependency.LocalVariable.class), found.entrySet().stream()
                .filter(entry -> entry.getValue().contains("java/time/Instant"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()));
    }

    @Test
    void aClassIsNoDependencyOfItself() throws IOException {
        assertFalse(dependencies(DependencyFixture.class).values().stream().anyMatch(names -> names.contains(FIXTURE)));
    }

    @Test
    void occurrencesAndTheScannerHoldToTheOntology() {
        for (final Class<?> type : List.of(Occurrence.class, Occurrence.OfClass.class, Occurrence.OfMember.class,
                ClassScanner.class, DependencyFinder.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }

    /** The classes a class depends on, by the kind of place they are named in. */
    private Map<Class<?>, Set<String>> dependencies(final Class<?> type) throws IOException {
        return ClassScanner.INSTANCE.scan(classFile(type)).stream()
                .flatMap(occurrence -> occurrence instanceof Occurrence.OfMember member
                        && member.kind() instanceof Dependency ? Stream.of(member) : Stream.empty())
                .collect(Collectors.groupingBy(member -> member.kind().getClass(),
                        Collectors.mapping(Occurrence.OfMember::name, Collectors.toSet())));
    }

    private byte[] classFile(final Class<?> type) throws IOException {
        try (InputStream in = ClassLoader.getSystemResourceAsStream(type.getName().replace('.', '/') + ".class")) {
            return in.readAllBytes();
        }
    }
}
