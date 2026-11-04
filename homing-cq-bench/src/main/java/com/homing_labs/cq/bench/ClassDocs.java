package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.extract.JavaCodeBases;
import com.homing_labs.cq.lang.java.lexer.JavaLexer;
import com.homing_labs.cq.lang.java.lexer.SourceText;
import com.homing_labs.cq.lang.java.outline.BodyPart;
import com.homing_labs.cq.lang.java.outline.FilePart;
import com.homing_labs.cq.lang.java.outline.JavaOutliner;
import com.homing_labs.cq.lang.java.outline.Outline;
import com.homing_labs.cq.lang.java.outline.TokenRange;
import com.homing_labs.cq.lang.java.outline.TypeHeader;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The documentation comment of a class, as plain text, from the source that declares it: a top-level class, or a
 * member class declared in one. The source is the file named after the top-level class, looked for in the directories
 * given, then in those beside the class's code base, as Maven lays a module out, and last on the class path under
 * {@code META-INF/homing-cq/sources}, where a jar of rules can pack its sources so a project that takes it from a
 * repository still has them explained. A class whose source is not at hand, declared deeper, or written without a
 * documentation comment, has none.
 */
public record ClassDocs() implements StatelessFunctionalObject {

    public static final ClassDocs INSTANCE = new ClassDocs();

    /** Where a jar packs its sources, by package. */
    private static final String PACKED_SOURCES = "/META-INF/homing-cq/sources/";

    /** The class's documentation comment, as {@link DocText} has it, its source looked for in {@code sources} first. */
    public Optional<String> of(final Class<?> type, final List<Path> sources) {
        final Class<?> top = top(type);
        return source(top, sources)
                .map(text -> JavaOutliner.INSTANCE.outline(JavaLexer.INSTANCE.scan(
                        new SourceText(top.getSimpleName() + ".java", text))))
                .flatMap(outline -> comment(outline, top, type))
                .map(DocText.INSTANCE::of);
    }

    /** The top-level class a class is declared in: the class itself, when it is one. */
    private Class<?> top(final Class<?> type) {
        Class<?> top = type;
        while (top.getDeclaringClass() != null) {
            top = top.getDeclaringClass();
        }
        return top;
    }

    /** The text of the file that declares the top-level class: the first directory's that has it, or else packed. */
    private Optional<String> source(final Class<?> top, final List<Path> sources) {
        final Path file = Path.of(top.getPackageName().replace('.', '/'), top.getSimpleName() + ".java");
        return Stream.concat(sources.stream(), beside(top).stream())
                .map(directory -> directory.resolve(file))
                .filter(Files::isRegularFile)
                .findFirst()
                .map(this::text)
                .or(() -> packed(top));
    }

    /** The text of the file packed on the class path, if it is. */
    private Optional<String> packed(final Class<?> top) {
        final String file = PACKED_SOURCES + top.getName().replace('.', '/') + ".java";
        try (InputStream in = top.getResourceAsStream(file)) {
            return in == null ? Optional.empty() : Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    /** The source directories beside the class's code base: none for a class of the JDK, which has none. */
    private List<Path> beside(final Class<?> top) {
        try {
            return JavaCodeBases.INSTANCE.of(top).sources().items().stream()
                    .map(directory -> Path.of(directory.location()))
                    .toList();
        } catch (final IllegalArgumentException noCodeBase) {
            return List.of();
        }
    }

    /** The comment of the class's header, the top-level class's or a member's of it, as written. */
    private Optional<String> comment(final Outline outline, final Class<?> top, final Class<?> type) {
        final Optional<FilePart.TypeDeclaration> declared = outline.types().stream()
                .filter(declaration -> declaration.header().name().text().equals(top.getSimpleName()))
                .findFirst();
        final Optional<TypeHeader> header = type == top ? declared.map(FilePart.TypeDeclaration::header)
                : type.getDeclaringClass() == top ? declared.flatMap(declaration -> member(declaration, type))
                : Optional.empty();
        return header.flatMap(TypeHeader::doc)
                .map(doc -> outline.text(new TokenRange(doc.token(), doc.token() + 1)));
    }

    private Optional<TypeHeader> member(final FilePart.TypeDeclaration declaration, final Class<?> type) {
        return declaration.parts().of(BodyPart.TypeDeclaration.class).stream()
                .map(BodyPart.TypeDeclaration::header)
                .filter(header -> header.name().text().equals(type.getSimpleName()))
                .findFirst();
    }

    private String text(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }
}
