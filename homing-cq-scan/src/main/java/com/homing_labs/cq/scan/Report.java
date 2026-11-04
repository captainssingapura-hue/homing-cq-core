package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * What a scan found, written down as files, for what cannot open the bench: {@code cq-report.tsv}, a table an agent can
 * search and filter, and {@code cq-report.md}, a summary a person reads first. Like the bench it is a gauge: it never
 * fails because something was found. That is the {@link Gate}'s work.
 *
 * <p>The same scan of the same code gives the same table, byte for byte, so two runs can be compared. Nothing in it
 * depends on the time; a source's path depends on the machine, unless the paths are given a root.</p>
 *
 * <p>As a program it is a plain command line tool:</p>
 * <pre>{@code Report [--out <folder>] [--root <folder>] <declaration class>...}</pre>
 * <ul>
 *   <li>{@code --out}: the folder the two files go in, by default {@code target/cq-report} under the current folder;
 *   </li>
 *   <li>{@code --root}: the folder source paths are written relative to, by default none, so they are written whole;
 *   </li>
 *   <li>the declarations are named as the gate's are, and composed in order.</li>
 * </ul>
 *
 * <p>A build can run it with the Maven exec plugin, or a separate project can run it over the jars of the code it
 * checks. {@code exec:java} runs in the folder Maven was started in, not the module's, so a pom should pass
 * {@code --out ${project.build.directory}/cq-report}. Where a build also runs the gate, run the report first: the gate
 * stops the build when it fails, which is when the report is wanted most.</p>
 */
public record Report() implements StatelessFunctionalObject {

    public static final Report INSTANCE = new Report();

    /** The name of the table's file. */
    public static final String TABLE = "cq-report.tsv";

    /** The name of the summary's file. */
    public static final String SUMMARY = "cq-report.md";

    /**
     * The rows of a scan, sorted: source paths relative to {@code root} when it is given and holds them, and else
     * whole.
     */
    public ReportRows rows(final ScanResult result, final Optional<Path> root) {
        return ReportTable.INSTANCE.rows(result, root);
    }

    /** The table of some rows, as its file holds it. */
    public String table(final ReportRows rows) {
        return ReportTable.INSTANCE.text(rows);
    }

    /** The summary of a scan, made by a declaration at a time, as its file holds it. */
    public String summary(final ScanResult result, final Declaration declaration, final Instant time) {
        return ReportSummary.INSTANCE.text(result, declaration, time);
    }

    /** Writes the table and the summary of a scan into a folder, made if need be, in UTF-8 with no byte-order mark. */
    public void write(final ScanResult result, final Declaration declaration, final Instant time, final Path folder,
                      final Optional<Path> root) {
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(TABLE), table(rows(result, root)), StandardCharsets.UTF_8);
            Files.writeString(folder.resolve(SUMMARY), summary(result, declaration, time), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot write the report to " + folder, e);
        }
    }

    /**
     * The report as a program: scans by the declarations named, writes the two files, prints how the scan came out and
     * where they went, and ends normally, whatever was found. It throws only on a bad argument, or when it cannot
     * write. It is an instance method: the launcher makes the report, then runs it.
     */
    public void main(final String[] args) {
        final ReportCommandLine.Arguments given = ReportCommandLine.INSTANCE.of(List.of(args));
        final ClassLoader loader = getClass().getClassLoader();
        final Declaration declaration = NamedDeclarations.INSTANCE.of(given.declarations(), loader);
        final ScanResult result = Scanner.INSTANCE.scan(declaration, loader);
        write(result, declaration, Instant.now(), given.folder(), given.root());
        System.out.println(Gate.INSTANCE.closing(result));
        System.out.println("Report written to " + given.folder().toAbsolutePath().normalize());
    }
}
