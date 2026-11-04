package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** The report program's command line, read into what it was given. */
record ReportCommandLine() implements StatelessFunctionalObject {

    static final ReportCommandLine INSTANCE = new ReportCommandLine();

    private static final String USAGE = "Usage: Report [--out <folder>] [--root <folder>] <declaration class>...";

    /** What the report was asked for: where it goes, what source paths are relative to, and what to check. */
    record Arguments(Path folder, Optional<Path> root, List<String> declarations) {
    }

    /** What the arguments ask for; a bad argument is refused, saying how the program is used. */
    Arguments of(final List<String> args) {
        Path folder = Path.of("target", "cq-report");
        Optional<Path> root = Optional.empty();
        final List<String> declarations = new ArrayList<>();
        final Iterator<String> given = args.iterator();
        while (given.hasNext()) {
            final String argument = given.next();
            if (argument.equals("--out")) {
                folder = Path.of(value(argument, given));
            } else if (argument.equals("--root")) {
                root = Optional.of(Path.of(value(argument, given)));
            } else if (argument.startsWith("--")) {
                throw new IllegalArgumentException("No option " + argument + ". " + USAGE);
            } else {
                declarations.add(argument);
            }
        }
        if (declarations.isEmpty()) {
            throw new IllegalArgumentException("Name the class of at least one declaration. " + USAGE);
        }
        return new Arguments(folder, root, List.copyOf(declarations));
    }

    private String value(final String option, final Iterator<String> given) {
        if (!given.hasNext()) {
            throw new IllegalArgumentException(option + " needs a folder. " + USAGE);
        }
        return given.next();
    }
}
