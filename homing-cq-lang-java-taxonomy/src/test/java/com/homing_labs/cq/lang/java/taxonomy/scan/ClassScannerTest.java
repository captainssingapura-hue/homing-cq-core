package com.homing_labs.cq.lang.java.taxonomy.scan;

import com.homing_labs.cq.lang.java.taxonomy.AnonymousClass;
import com.homing_labs.cq.lang.java.taxonomy.ClassAnnotation;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ClassProperty;
import com.homing_labs.cq.lang.java.taxonomy.Dependency;
import com.homing_labs.cq.lang.java.taxonomy.LocalClass;
import com.homing_labs.cq.lang.java.taxonomy.RecordComponent;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import com.homing_labs.cq.lang.java.taxonomy.TypeParameter;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.RecordComponentInfo;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
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
    void aClassHasTheVisibilityItWasDeclaredWithEvenWhenNested() throws IOException {
        assertEquals(List.of("public"), properties(Object.class, ClassProperty.Visibility.INSTANCE));
        assertEquals(List.of("package"), properties(DependencyFixture.class, ClassProperty.Visibility.INSTANCE));
        assertEquals(List.of("private"), properties(Hidden.class, ClassProperty.Visibility.INSTANCE),
                "a private class is package-private in its own flags");
    }

    @Test
    void aClassIsFinalSealedOrOpen() throws IOException {
        assertEquals(List.of("open"), properties(Object.class, ClassProperty.Extensibility.INSTANCE));
        assertEquals(List.of("sealed"), properties(DependencyFixture.class, ClassProperty.Extensibility.INSTANCE));
        assertEquals(List.of("final"), properties(Hidden.class, ClassProperty.Extensibility.INSTANCE));
    }

    /** A class whose class file cannot say it is private. */
    private static final class Hidden {
    }

    @Test
    void aClassHasTheAnnotationsItsClassFileKeeps() throws IOException {
        assertEquals(List.of("java/lang/Deprecated"),
                ClassScanner.INSTANCE.scan(classFile(DependencyFixture.class)).stream()
                        .flatMap(occurrence -> occurrence instanceof Occurrence.OfMember member
                                && member.kind() instanceof ClassAnnotation ? Stream.of(member.name()) : Stream.empty())
                        .toList());
    }

    @Test
    void aLocalOrAnonymousClassIsFoundInItsOwnFileAsTheEnclosingClasses() throws IOException {
        class Local {
        }
        final Class<?> anonymous = new Object() {
        }.getClass();
        final String enclosing = internalName(ClassScannerTest.class);
        assertTrue(ClassScanner.INSTANCE.scan(classFile(Local.class))
                .contains(new Occurrence.OfMember(enclosing, LocalClass.INSTANCE, internalName(Local.class))));
        assertTrue(ClassScanner.INSTANCE.scan(classFile(anonymous))
                .contains(new Occurrence.OfMember(enclosing, AnonymousClass.INSTANCE, internalName(anonymous))));
        assertTrue(ClassScanner.INSTANCE.scan(classFile(ClassScannerTest.class)).stream()
                        .map(Occurrence::kind)
                        .noneMatch(kind -> kind instanceof LocalClass || kind instanceof AnonymousClass),
                "the enclosing class's own file cannot tell its local classes from deeper ones");
    }

    @Test
    void aGenericClassHasItsTypeParametersInOrder() throws IOException {
        assertEquals(List.of("K", "V"), typeParameters(Generic.class));
        assertEquals(List.of(), typeParameters(Hidden.class));
    }

    /** A class with two type parameters, the second bounded by the first. */
    private static final class Generic<K, V extends K> {
    }

    /** The names of the type parameters the scanner finds a class declares. */
    private List<String> typeParameters(final Class<?> type) throws IOException {
        return ClassScanner.INSTANCE.scan(classFile(type)).stream()
                .flatMap(occurrence -> occurrence instanceof Occurrence.OfMember member
                        && member.kind() instanceof TypeParameter ? Stream.of(member.name()) : Stream.empty())
                .toList();
    }

    @Test
    void aRecordAttributeMakesARecordOnlyOfAFinalClassExtendingRecord() {
        final ClassDesc record = ClassDesc.of("java.lang.Record");
        assertEquals(List.of("Record [x]", "Class []", "AbstractClass []", "Class []"), Stream.of(
                        withRecordAttribute("Final", record, ClassFile.ACC_FINAL),
                        withRecordAttribute("Open", record, 0),
                        withRecordAttribute("Abstract", record, ClassFile.ACC_ABSTRACT),
                        withRecordAttribute("Pretender", ConstantDescs.CD_Object, ClassFile.ACC_FINAL))
                .map(this::kindAndComponents)
                .toList());
    }

    /**
     * A class file javac would never write: a class with a field {@code x} and a {@code Record} attribute naming it as
     * a component, whatever the class's superclass and flags.
     */
    private byte[] withRecordAttribute(final String simpleName, final ClassDesc superclass, final int flags) {
        return ClassFile.of().build(ClassDesc.of("handbuilt." + simpleName), builder -> builder
                .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER | flags)
                .withSuperclass(superclass)
                .withField("x", ConstantDescs.CD_int, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL)
                .with(RecordAttribute.of(RecordComponentInfo.of("x", ConstantDescs.CD_int, List.of()))));
    }

    /** The kind of the class a class file declares, and the names of the record components the scanner finds. */
    private String kindAndComponents(final byte[] classFile) {
        final List<Occurrence> found = ClassScanner.INSTANCE.scan(classFile);
        return found.getFirst().kind().getClass().getSimpleName() + " " + found.stream()
                .flatMap(occurrence -> occurrence instanceof Occurrence.OfMember member
                        && member.kind() instanceof RecordComponent ? Stream.of(member.name()) : Stream.empty())
                .toList();
    }

    private String internalName(final Class<?> type) {
        return type.getName().replace('.', '/');
    }

    /** What the scanner finds of a property of a class. */
    private List<String> properties(final Class<?> type, final ClassProperty property) throws IOException {
        return ClassScanner.INSTANCE.scan(classFile(type)).stream()
                .flatMap(occurrence -> occurrence instanceof Occurrence.OfMember member
                        && member.kind().equals(property) ? Stream.of(member.name()) : Stream.empty())
                .toList();
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
