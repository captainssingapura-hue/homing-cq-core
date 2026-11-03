package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.unit.AnnotationUnit;
import com.homing_labs.cq.lang.java.unit.ClassUnit;
import com.homing_labs.cq.lang.java.unit.EnumUnit;
import com.homing_labs.cq.lang.java.unit.InterfaceUnit;
import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.unit.RecordUnit;
import com.homing_labs.cq.lang.java.fixtures.Plain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the fixtures module - code that knows nothing of CQ - as a qc module would read the code it checks.
 */
class JavaUnitReaderTest {

    private static final String FIXTURES = "com.homing_labs.cq.lang.java.fixtures.";

    private final List<JavaUnit> units = JavaUnitReader.INSTANCE.read(Plain.class);

    @Test
    void readsEveryTopLevelClassAndNothingElse() {
        assertEquals(List.of(
                        FIXTURES + "ARecord",
                        FIXTURES + "AnAnnotation",
                        FIXTURES + "AnEnum",
                        FIXTURES + "AnInterface",
                        FIXTURES + "FailsOnInit",
                        FIXTURES + "Nesting",
                        FIXTURES + "Plain",
                        FIXTURES + "SecondInFile",
                        FIXTURES + "Switching",
                        FIXTURES + "TwoInOneFile",
                        FIXTURES + "deeper.Deep"),
                names(units));
    }

    @Test
    void pairsEachClassWithTheFileThatDeclaresIt() {
        for (final JavaUnit unit : units) {
            assertTrue(unit.source().isPresent(), unit.type().getName() + " was read without its source");
            final JavaSource source = unit.source().orElseThrow();
            final String simpleName = unit.type().getSimpleName();
            final String fileName = simpleName.equals("SecondInFile") ? "TwoInOneFile.java" : simpleName + ".java";
            final String path = unit.type().getPackageName().replace('.', '/') + "/" + fileName;
            assertTrue(source.file().getPath().endsWith(path),
                    unit.type().getName() + " was paired with " + source.file());
            assertTrue(Pattern.compile("\\b(class|interface|enum|record)\\s+" + simpleName + "\\b")
                            .matcher(source.text()).find(),
                    unit.type().getName() + "'s source does not declare it");
        }
    }

    @Test
    void eachUnitIsOfItsClassKind() {
        assertEquals(List.of(ClassUnit.class, InterfaceUnit.class, EnumUnit.class, RecordUnit.class,
                        AnnotationUnit.class),
                Stream.of("Plain", "AnInterface", "AnEnum", "ARecord", "AnAnnotation")
                        .map(name -> unit(name).getClass())
                        .toList());
    }

    @Test
    void aSecondTopLevelClassInAFileIsItsOwnUnitSharingThatFile() {
        final JavaUnit first = unit("TwoInOneFile");
        final JavaUnit second = unit("SecondInFile");
        assertTrue(first.source().isPresent());
        assertEquals(first.source(), second.source());
    }

    @Test
    void nestedClassesArePartOfTheirTopLevelUnit() {
        assertEquals(List.of(
                        FIXTURES + "Nesting",
                        FIXTURES + "Nesting$1",
                        FIXTURES + "Nesting$1Local",
                        FIXTURES + "Nesting$Member",
                        FIXTURES + "Nesting$StaticNested"),
                Arrays.stream(unit("Nesting").type().getNestMembers()).map(Class::getName).sorted().toList());
        assertEquals(List.of(FIXTURES + "AnEnum", FIXTURES + "AnEnum$1"),
                Arrays.stream(unit("AnEnum").type().getNestMembers()).map(Class::getName).sorted().toList());
    }

    @Test
    void aClassTheCompilerGeneratedIsNoUnit(@TempDir final Path classes) throws Exception {
        final Path plain = classes.resolve(Plain.class.getName().replace('.', '/') + ".class");
        Files.createDirectories(plain.getParent());
        try (InputStream in = Plain.class.getResourceAsStream(Plain.class.getSimpleName() + ".class")) {
            Files.copy(in, plain);
        }
        Files.write(plain.resolveSibling("Generated.class"), ClassFile.of().build(
                ClassDesc.of(FIXTURES + "Generated"), builder -> builder
                        .withFlags(ClassFile.ACC_SUPER | ClassFile.ACC_SYNTHETIC)
                        .withSuperclass(ConstantDescs.CD_Object)));
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            assertEquals(List.of(Plain.class.getName()), names(JavaUnitReader.INSTANCE.read(
                    new JavaCodeBase(classes.toUri(), new SourceDirectories()), loader)));
        }
    }

    @Test
    void loadsEachClassWithoutInitialisingIt() {
        final Class<?> failsOnInit = unit("FailsOnInit").type();
        assertThrows(ExceptionInInitializerError.class,
                () -> Class.forName(failsOnInit.getName(), true, failsOnInit.getClassLoader()));
    }

    @Test
    void skipsPackageInfo() {
        assertNotNull(Plain.class.getClassLoader().getResource(FIXTURES.replace('.', '/') + "package-info.class"),
                "the fixtures are compiled with -Xpkginfo:always, so there is a package-info.class to skip");
        assertTrue(names(units).stream().noneMatch(name -> name.endsWith("package-info")));
    }

    @Test
    void theSameCodeBaseGivesTheSameUnits() {
        assertEquals(units, JavaUnitReader.INSTANCE.read(Plain.class));
    }

    @Test
    void aJarWithNoSourcesBesideItGivesUnitsWithoutSources(@TempDir final Path temp) throws Exception {
        final Path jar = copyOfFixturesAsJar(temp.resolve("fixtures.jar"));
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            final Class<?> anchor = loader.loadClass(Plain.class.getName());
            final List<JavaUnit> fromJar = JavaUnitReader.INSTANCE.read(anchor);

            assertEquals(names(units), names(fromJar));
            for (final JavaUnit unit : fromJar) {
                assertTrue(unit.source().isEmpty(), unit.type().getName() + " found a source");
                assertEquals(loader, unit.type().getClassLoader(),
                        unit.type().getName() + " was not loaded from the jar");
            }
        }
    }

    @Test
    void aSourceIsFoundInTheFirstOfTheDirectoriesGivenThatHasIt(@TempDir final Path temp) throws Exception {
        final Path jar = copyOfFixturesAsJar(temp.resolve("fixtures.jar"));
        final Path empty = Files.createDirectories(temp.resolve("no-sources"));
        final SourceDirectories written = JavaCodeBases.INSTANCE.of(Plain.class).sources();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            final List<JavaUnit> fromJar = JavaUnitReader.INSTANCE.read(new JavaCodeBase(jar.toUri(),
                    new SourceDirectories(empty.toUri()).then(written)), loader);

            assertEquals(names(units), names(fromJar));
            assertTrue(fromJar.stream().allMatch(unit -> unit.source().isPresent()), "a unit without its source");
            assertEquals(units.stream().map(JavaUnit::source).toList(),
                    fromJar.stream().map(JavaUnit::source).toList());
        }
    }

    private JavaUnit unit(final String simpleName) {
        return units.stream()
                .filter(unit -> unit.type().getName().equals(FIXTURES + simpleName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no unit for " + simpleName));
    }

    private List<String> names(final List<JavaUnit> units) {
        return units.stream().map(unit -> unit.type().getName()).toList();
    }

    /**
     * Copies the fixtures' compiled classes, wherever the build put them, into a jar with nothing beside it - and one
     * of them again under {@code META-INF}, where a class is not the module's own and must not be read.
     */
    private Path copyOfFixturesAsJar(final Path jar) throws IOException {
        final Path classes = Path.of(JavaCodeBases.INSTANCE.of(Plain.class).classes());
        try (FileSystem out = FileSystems.newFileSystem(jar, Map.of("create", "true"))) {
            if (Files.isDirectory(classes)) {
                copyInto(classes, out);
            } else {
                try (FileSystem in = FileSystems.newFileSystem(classes)) {
                    copyInto(in.getPath("/"), out);
                }
            }
            final Path plain = out.getPath("/" + Plain.class.getName().replace('.', '/') + ".class");
            final Path underMetaInf = out.getPath("/META-INF/versions/9" + plain);
            Files.createDirectories(underMetaInf.getParent());
            Files.copy(plain, underMetaInf);
        }
        return jar;
    }

    private void copyInto(final Path root, final FileSystem out) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (final Path file : files.filter(Files::isRegularFile).toList()) {
                final String entry = StreamSupport.stream(root.relativize(file).spliterator(), false)
                        .map(Path::toString)
                        .collect(Collectors.joining("/"));
                final Path target = out.getPath("/" + entry);
                Files.createDirectories(target.getParent());
                Files.copy(file, target);
            }
        }
    }
}
