package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.lexer.TokenStream;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every source file of this project, outlined, and read by javac's own parser: the two agree on the package, the
 * imports, each type's header, and each part of its body - down to where a field's initial value ends - and of each
 * member type's body, one level down; and the outline finds nothing wrong, as all of it compiles.
 */
class JavacAgreementTest {

    @Test
    void theOutlineReadsEverySourceFileOfTheProjectAsJavacDoes() throws IOException {
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

    /** The package, the imports, and each type's header and the parts of its body, as the outline reads them. */
    List<String> read(final Outline outline) {
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
            read.add(header(outline::text, type.header()));
            type.parts().items().forEach(part -> read.addAll(part(outline::text, part, "  ")));
        }
        return read;
    }

    private String header(final Function<TokenRange, String> written, final TypeHeader header) {
        return header.keyword().word() + " " + header.name().text() + modifiers(header.modifiers())
                + " <" + header.typeParameters().items().stream().map(parameter -> parameter.name().text())
                .toList() + ">"
                + " components " + header.components().map(components -> components.items().stream()
                .map(component -> component.name().text()).toList()).orElse(List.of())
                + " extends " + texts(written, header.keyword() instanceof TypeKeyword.Interface
                ? header.superinterfaces() : header.superclass().stream().toList())
                + " implements " + texts(written, header.keyword() instanceof TypeKeyword.Interface
                ? List.of() : header.superinterfaces())
                + " permits " + texts(written, header.permitted());
    }

    /**
     * A part of a type's body as the outline reads it, with the text its tokens are written with: a field as each of
     * its variables, nothing for a semicolon, and a member type as its header and, when the outline read its body, the
     * parts of that body, a level further in.
     */
    private List<String> part(final Function<TokenRange, String> written, final BodyPart part, final String indent) {
        return switch (part) {
            case BodyPart.Field field -> field.declarators().items().stream()
                    .map(declarator -> indent + "field " + declarator.name().text() + modifiers(field.modifiers())
                            + " " + (declarator.dimensions().isEmpty() ? text(written, field.type().range()) : "dims")
                            + " = " + declarator.initializer().map(range -> text(written, range)).orElse("-"))
                    .toList();
            case BodyPart.Method method -> List.of(indent + "method " + method.name().text()
                    + modifiers(method.modifiers()) + typeParameters(method.typeParameters()) + " "
                    + (method.dimensions().isEmpty() ? text(written, method.result().range()) : "dims")
                    + parameters(written, method.parameters()) + " throws " + texts(written, method.thrown().items())
                    + " default " + method.defaultValue().map(range -> text(written, range)).orElse("-")
                    + " body " + method.body().isPresent());
            case BodyPart.Constructor constructor -> List.of(indent + "constructor " + constructor.name().text()
                    + modifiers(constructor.modifiers()) + typeParameters(constructor.typeParameters())
                    + parameters(written, constructor.parameters())
                    + " throws " + texts(written, constructor.thrown().items())
                    + " body " + constructor.body().isPresent());
            case BodyPart.CompactConstructor constructor -> List.of(indent + "compact " + constructor.name().text()
                    + modifiers(constructor.modifiers()));
            case BodyPart.Initializer initializer -> List.of(indent + (initializer.isStatic() ? "static " : "")
                    + "block");
            case BodyPart.TypeDeclaration type -> {
                final List<String> lines = new ArrayList<>(List.of(indent + header(written, type.header())));
                type.nested().ifPresent(nested -> nested.parts().items()
                        .forEach(member -> lines.addAll(part(nested::text, member, indent + "  "))));
                yield lines;
            }
            case BodyPart.EnumConstant constant -> List.of(indent + "constant " + constant.name().text()
                    + " @" + constant.annotations().annotations().size()
                    + " arguments " + constant.arguments().map(range -> text(written, range)).orElse("")
                    + " body " + constant.body().isPresent());
            case BodyPart.EmptyDeclaration _ -> List.of();
            case BodyPart.Unrecognised _ -> List.of(indent + "unrecognised");
        };
    }

    private String modifiers(final Modifiers modifiers) {
        return " " + modifiers.words().stream().map(ModifierItem.Word::word).sorted().toList()
                + " @" + modifiers.annotations().size();
    }

    private String typeParameters(final TypeParameters parameters) {
        return " <" + parameters.items().stream().map(parameter -> parameter.name().text()).toList() + ">";
    }

    private String parameters(final Function<TokenRange, String> written, final Parameters parameters) {
        return " (" + parameters.items().stream().map(parameter -> switch (parameter) {
            case Parameter.Receiver receiver -> "receiver " + text(written, receiver.type().range());
            case Parameter.Formal formal -> (formal.dimensions().isEmpty()
                    ? text(written, formal.type().range()) + (formal.variable() ? "..." : "") : "dims")
                    + " " + formal.name().text() + modifiers(formal.modifiers());
        }).collect(Collectors.joining(", ")) + ")";
    }

    /** The package, the imports, and each type's header and members, as javac reads them. */
    List<String> read(final CompilationUnitTree unit, final SourcePositions positions, final String text) {
        final Javac javac = new Javac(unit, positions, text);
        final List<String> read = new ArrayList<>();
        if (unit.getPackageName() != null) {
            read.add("package " + unit.getPackageName());
        }
        for (final ImportTree tree : unit.getImports()) {
            read.add("import " + (tree.isStatic() ? "static " : "") + (tree.isModule() ? "module " : "")
                    + tree.getQualifiedIdentifier());
        }
        for (final Tree tree : unit.getTypeDecls()) {
            if (tree instanceof ClassTree type) {
                read.add(javac.header(type));
                type.getMembers().forEach(member -> read.addAll(javac.member(type, member, "  ", true)));
            }
        }
        return read;
    }

    private List<String> texts(final Function<TokenRange, String> written, final List<TypeText> types) {
        return types.stream().map(type -> text(written, type.range())).toList();
    }

    /** The text of the tokens, with no whitespace, so that how a type or a value is laid out does not count. */
    private String text(final Function<TokenRange, String> written, final TokenRange range) {
        return written.apply(range).replaceAll("\\s+", "");
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

    /** One file's trees as javac's parser makes them, written as the outline's parts are. */
    private record Javac(CompilationUnitTree unit, SourcePositions positions, String text) {

        String header(final ClassTree type) {
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
            return keyword + " " + type.getSimpleName() + modifiers(type.getModifiers())
                    + " <" + names(type.getTypeParameters()) + ">"
                    + " components " + recordComponents(type)
                    + " extends " + texts(extended)
                    + " implements " + texts(implemented)
                    + " permits " + texts(List.copyOf(type.getPermitsClause()));
        }

        /**
         * A member of the type: a variable a field declares, a method, a block or a member type - with its members a
         * level further in, when they are read. javac's parser makes a record's components its instance fields, and
         * its compact constructor's parameters; an enum constant is a field whose type is written nowhere.
         */
        List<String> member(final ClassTree type, final Tree member, final String indent, final boolean readsIn) {
            final String name = type.getSimpleName().toString();
            return switch (member) {
                case VariableTree field when type.getKind() == Tree.Kind.ENUM && unwritten(field.getType()) -> {
                    final NewClassTree made = (NewClassTree) field.getInitializer();
                    yield List.of(indent + "constant " + field.getName() + " @"
                            + field.getModifiers().getAnnotations().size()
                            + " arguments " + arguments(field, made) + " body " + (made.getClassBody() != null));
                }
                case VariableTree field when type.getKind() == Tree.Kind.RECORD
                        && !field.getModifiers().getFlags().contains(Modifier.STATIC) -> List.of();
                case VariableTree field -> List.of(indent + "field " + field.getName()
                        + modifiers(field.getModifiers()) + " " + (dimensioned(field) ? "dims" : text(field.getType()))
                        + " = " + (field.getInitializer() == null ? "-" : text(field.getInitializer())));
                case MethodTree method when method.getName().contentEquals("<init>") && compact(method) ->
                        List.of(indent + "compact " + name + modifiers(method.getModifiers()));
                case MethodTree method when method.getName().contentEquals("<init>") -> List.of(indent + "constructor "
                        + name + modifiers(method.getModifiers()) + " <" + names(method.getTypeParameters()) + ">"
                        + parameters(method) + " throws " + texts(method.getThrows())
                        + " body " + (method.getBody() != null));
                case MethodTree method -> List.of(indent + "method " + method.getName()
                        + modifiers(method.getModifiers()) + " <" + names(method.getTypeParameters()) + "> "
                        + (text(method.getReturnType()).contains("(") ? "dims" : text(method.getReturnType()))
                        + parameters(method) + " throws " + texts(method.getThrows())
                        + " default " + (method.getDefaultValue() == null ? "-" : text(method.getDefaultValue()))
                        + " body " + (method.getBody() != null));
                case BlockTree block -> List.of(indent + (block.isStatic() ? "static " : "") + "block");
                case ClassTree nested -> {
                    final List<String> lines = new ArrayList<>(List.of(indent + header(nested)));
                    if (readsIn) {
                        nested.getMembers().forEach(inner -> lines.addAll(member(nested, inner, indent + "  ", false)));
                    }
                    yield lines;
                }
                default -> List.of(indent + member.getKind());
            };
        }

        private String parameters(final MethodTree method) {
            final List<String> written = new ArrayList<>();
            if (method.getReceiverParameter() != null) {
                written.add("receiver " + text(method.getReceiverParameter().getType()));
            }
            method.getParameters().forEach(parameter -> written.add((dimensioned(parameter) ? "dims"
                    : text(parameter.getType())) + " " + parameter.getName() + modifiers(parameter.getModifiers())));
            return " (" + String.join(", ", written) + ")";
        }

        /**
         * The arguments an enum constant is made with, in their parentheses, if they are written. javac's trees for a
         * constant start where its name does, so the parentheses are found in the text after it.
         */
        private String arguments(final VariableTree constant, final NewClassTree made) {
            final int named = text.indexOf(constant.getName().toString(), constant.getModifiers().getAnnotations()
                    .stream().mapToInt(annotation -> (int) positions.getEndPosition(unit, annotation)).max()
                    .orElse(start(constant))) + constant.getName().length();
            int open = named;
            while (Character.isWhitespace(text.charAt(open))) {
                open++;
            }
            if (text.charAt(open) != '(') {
                return "";
            }
            final int close = text.indexOf(')', made.getArguments().isEmpty() ? open
                    : (int) positions.getEndPosition(unit, made.getArguments().getLast()));
            return text(open, close + 1);
        }

        /** Whether brackets are written after the variable's name: javac's type for it then spans the name. */
        private boolean dimensioned(final VariableTree variable) {
            return Pattern.compile("\\b" + Pattern.quote(variable.getName().toString())
                            + "\\s*(@[\\w.]+(\\([^)]*\\))?\\s*)*\\[")
                    .matcher(text.substring(start(variable.getType()),
                            (int) positions.getEndPosition(unit, variable.getType()))).find();
        }

        /** Whether a constructor is written with no parentheses: a record's compact one. */
        private boolean compact(final MethodTree method) {
            final String written = text.substring(start(method), (int) positions.getEndPosition(unit, method))
                    .replaceAll("@[\\w.]+(\\([^)]*\\))?", "");
            final int brace = written.indexOf('{');
            final int parenthesis = written.indexOf('(');
            return parenthesis < 0 || brace >= 0 && brace < parenthesis;
        }

        private boolean unwritten(final Tree tree) {
            return positions.getEndPosition(unit, tree) <= positions.getStartPosition(unit, tree);
        }

        /**
         * A record's components, which javac's parser makes the record's instance fields: any field written in a
         * record is static.
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

        private String modifiers(final ModifiersTree modifiers) {
            return " " + modifiers.getFlags().stream().map(Object::toString).sorted().toList()
                    + " @" + modifiers.getAnnotations().size();
        }

        private List<String> names(final List<? extends TypeParameterTree> parameters) {
            return parameters.stream().map(parameter -> parameter.getName().toString()).toList();
        }

        private List<String> texts(final List<? extends Tree> trees) {
            return trees.stream().map(this::text).toList();
        }

        private String text(final Tree tree) {
            return text(start(tree), (int) positions.getEndPosition(unit, tree));
        }

        private String text(final int from, final int to) {
            return text.substring(from, Math.max(from, to)).replaceAll("\\s+", "");
        }

        private int start(final Tree tree) {
            return (int) positions.getStartPosition(unit, tree);
        }
    }
}
