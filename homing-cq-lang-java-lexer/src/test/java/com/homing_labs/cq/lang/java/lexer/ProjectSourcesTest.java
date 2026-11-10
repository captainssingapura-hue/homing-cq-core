package com.homing_labs.cq.lang.java.lexer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every source file of this project, read into tokens: all of it compiles, so none of it is wrong. */
class ProjectSourcesTest {

    @Test
    void everySourceFileOfTheProjectIsReadWithNothingWrongAndNothingLeftOut() throws IOException {
        final List<Path> files = sources();
        assertTrue(files.size() > 100, "found only " + files.size() + " source files");
        final List<String> problems = new ArrayList<>();
        for (final Path file : files) {
            final String text = Files.readString(file);
            final TokenStream stream = JavaLexer.INSTANCE.scan(new SourceText(file.getFileName().toString(), text));
            stream.diagnostics().items().forEach(diagnostic -> problems.add(file + ": " + diagnostic));
            int end = 0;
            for (final Token token : stream.tokens().items()) {
                if (!text.substring(end, token.start()).isBlank() || token.start() >= token.end()) {
                    problems.add(file + ": " + token + " after " + end);
                }
                end = token.end();
            }
            if (!text.substring(end).isBlank()) {
                problems.add(file + ": text left after " + end);
            }
        }
        assertEquals(List.of(), problems);
    }

    /** The Java source files of every module beside this one. */
    static List<Path> sources() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("").toAbsolutePath().getParent())) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("src"))
                    .filter(path -> !path.toString().contains("target"))
                    .sorted()
                    .toList();
        }
    }
}
