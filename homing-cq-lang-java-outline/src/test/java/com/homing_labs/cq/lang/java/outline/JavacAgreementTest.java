package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every source file of this project, read at its top level, and read by javac's own parser: the two agree on the
 * package, the imports, and each type's header, and the file level finds nothing wrong, as all of it compiles.
 */
class JavacAgreementTest {

    @Test
    void theFileLevelReadsEverySourceFileOfTheProjectAsJavacDoes() throws IOException {
        final List<Path> files = sources();
        assertTrue(files.size() > 100, "found only " + files.size() + " source files");
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> disagreements = new ArrayList<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null,
                StandardCharsets.UTF_8)) {
            final JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, List.of("-proc:none"), null,
                    fileManager.getJavaFileObjectsFromPaths(files));
            final SourcePositions positions = Trees.instance(task).getSourcePositions();
            for (final CompilationUnitTree unit : task.parse()) {
                final Path file = Path.of(unit.getSourceFile().toUri());
                final String text = Files.readString(file);
                final TokenStream tokens = JavaLexer.INSTANCE.scan(new SourceText(file.getFileName().toString(),
                        text));
                final Outline outline = JavaOutliner.INSTANCE.outline(tokens);
                final List<String> ours = new ArrayList<>();
                outline.diagnostics().items().forEach(diagnostic -> ours.add("diagnostic " + diagnostic));
                ours.addAll(read(outline));
                final List<String> javacs = read(unit, positions, text);
                if (!ours.equals(javacs)) {
                    disagreements.add(file + "\n  ours:  " + ours + "\n  javac: " + javacs);
                }
            }
        }
        assertEquals(List.of(), disagreements);
    }

    /** The package, the imports and the type headers, as the file level reads them. */
    private List<String> read(final Outline outline) {
        final List<String> read = new ArrayList<>();
        outline.packageDeclaration().ifPresent(declaration -> read.add("package " + declaration.name().text()));
        for (final FilePart.ImportDeclaration declaration : outline.imports()) {
            read.add("import " + switch (declaration.kind()) {
                case ImportKind.SingleType _ -> declaration.name().text();
                case ImportKind.OnDemand _ -> declaration.name().text() + ".*";
                case ImportKind.SingleStatic _ -> "static " + declaration.name().text();
                case ImportKind.StaticOnDemand _ -> "static " + declaration.name().text() + ".*";
                case ImportKind.Module _ -> "module " + declaration.name().text();
            });
        }
        for (final FilePart.TypeDeclaration type : outline.types()) {
            final TypeHeader header = type.header();
            read.add(header.keyword().word() + " " + header.name().text()
                    + " " + header.modifiers().words().stream().map(ModifierItem.Word::word).sorted().toList()
                    + " @" + header.modifiers().annotations().size()
                    + " <" + header.typeParameters().items().stream().map(parameter -> parameter.name().text())
                    .toList() + ">"
                    + " components " + header.components().map(components -> components.items().stream()
                    .map(component -> component.name().text()).toList()).orElse(List.of())
                    + " extends " + texts(outline, header.keyword() instanceof TypeKeyword.Interface
                    ? header.superinterfaces() : header.superclass().stream().toList())
                    + " implements " + texts(outline, header.keyword() instanceof TypeKeyword.Interface
                    ? List.of() : header.superinterfaces())
                    + " permits " + texts(outline, header.permitted()));
        }
        return read;
    }

    /** The package, the imports and the type headers, as javac reads them. */
    private List<String> read(final CompilationUnitTree unit, final SourcePositions positions, final String text) {
        final List<String> read = new ArrayList<>();
        if (unit.getPackageName() != null) {
            read.add("package " + unit.getPackageName());
        }
        for (final ImportTree tree : unit.getImports()) {
            read.add("import " + (tree.isStatic() ? "static " : "") + (tree.isModule() ? "module " : "")
                    + tree.getQualifiedIdentifier());
        }
        for (final Tree tree : unit.getTypeDecls()) {
            if (!(tree instanceof ClassTree type)) {
                continue;
            }
            final String keyword = switch (type.getKind()) {
                case INTERFACE -> "interface";
                case ENUM -> "enum";
                case RECORD -> "record";
                case ANNOTATION_TYPE -> "@interface";
                default -> "class";
            };
            final List<Tree> extended = type.getKind() == Tree.Kind.INTERFACE
                    ? List.copyOf(type.getImplementsClause())
                    : Stream.of(type.getExtendsClause()).filter(Objects::nonNull).map(Tree.class::cast).toList();
            final List<Tree> implemented = type.getKind() == Tree.Kind.INTERFACE ? List.of()
                    : List.copyOf(type.getImplementsClause());
            read.add(keyword + " " + type.getSimpleName()
                    + " " + type.getModifiers().getFlags().stream().map(Object::toString).sorted().toList()
                    + " @" + type.getModifiers().getAnnotations().size()
                    + " <" + type.getTypeParameters().stream().map(parameter -> parameter.getName().toString())
                    .toList() + ">"
                    + " components " + recordComponents(type)
                    + " extends " + texts(unit, positions, text, extended)
                    + " implements " + texts(unit, positions, text, implemented)
                    + " permits " + texts(unit, positions, text, List.copyOf(type.getPermitsClause())));
        }
        return read;
    }

    /**
     * A record's components, which javac's parser makes the record's instance fields: any field written in a record
     * is static.
     */
    private List<String> recordComponents(final ClassTree type) {
        if (type.getKind() != Tree.Kind.RECORD) {
            return List.of();
        }
        return type.getMembers().stream()
                .flatMap(member -> member instanceof VariableTree field
                        && !field.getModifiers().getFlags().contains(Modifier.STATIC)
                        ? Stream.of(field.getName().toString()) : Stream.empty())
                .toList();
    }

    private List<String> texts(final Outline outline, final List<TypeText> types) {
        return types.stream().map(type -> outline.text(type.range()).replaceAll("\\s+", " ")).toList();
    }

    private List<String> texts(final CompilationUnitTree unit, final SourcePositions positions, final String text,
                               final List<? extends Tree> trees) {
        return trees.stream()
                .map(tree -> text.substring((int) positions.getStartPosition(unit, tree),
                        (int) positions.getEndPosition(unit, tree)).replaceAll("\\s+", " "))
                .toList();
    }

    private List<Path> sources() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("").toAbsolutePath().getParent())) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("src"))
                    .filter(path -> !path.toString().contains("target"))
                    .sorted()
                    .toList();
        }
    }
}
