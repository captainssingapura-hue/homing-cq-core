package com.homing_labs.cq.scan;

import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The report's table: its rows from a scan, and its text. The rows come from the rules applied to whole units, as the
 * gate and the rule book count them: what a sub-rule finds on a nested class is already in the result of the unit it
 * was delegated from.
 */
record ReportTable() implements StatelessFunctionalObject {

    static final ReportTable INSTANCE = new ReportTable();

    private static final String COLUMNS =
            "id\tentry\tpackage\tclass\tcodebase\tsource\tprinciple\trule\tfinding\tdetails\texcuse";

    /** The rows of a scan, sorted: source paths relative to {@code root} when it is given and holds them. */
    ReportRows rows(final ScanResult result, final Optional<Path> root) {
        return new ReportRows(Stream.concat(result.units().items().stream()
                        .sorted(Comparator.comparing(scanned -> scanned.unit().type().getName()))
                        .flatMap(scanned -> rows(scanned, root)),
                result.stale().stream().map(this::row)).toList());
    }

    /** A stale exemption's row: what it names may not have been scanned, so it says no more than the exemption does. */
    private ReportRow row(final StaleExemption stale) {
        final Class<?> offender = stale.exemption().offender();
        final String rule = stale.exemption().rule().getSimpleName();
        return new ReportRow(rule + ":" + offender.getName() + "#stale", "stale", offender.getPackageName(),
                offender.getName(), codebase(offender), Optional.empty(), "", rule,
                Optional.of(stale.getClass().getSimpleName()), stale.text(),
                Optional.of(stale.exemption().excuse().getSimpleName()));
    }

    /** The rows as a table: a header, then a line for each row, each ending in {@code \n} on every platform. */
    String text(final ReportRows rows) {
        return Stream.concat(Stream.of(COLUMNS), rows.items().stream().map(this::line))
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private String line(final ReportRow row) {
        return row.cells().stream().map(this::escaped).collect(Collectors.joining("\t"));
    }

    private String escaped(final String cell) {
        return cell.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private Stream<ReportRow> rows(final ScannedUnit scanned, final Optional<Path> root) {
        final Located located = located(scanned.unit(), root);
        return scanned.applied().items().stream()
                .collect(Collectors.groupingBy(applied -> applied.signature().rule().getSimpleName(), TreeMap::new,
                        Collectors.toList()))
                .entrySet().stream()
                .flatMap(rule -> rows(located, rule.getKey(), entries(rule.getValue())));
    }

    /** The rows of one rule on a unit, numbered from 1. */
    private Stream<ReportRow> rows(final Located unit, final String rule, final List<Entry> entries) {
        return IntStream.range(0, entries.size()).mapToObj(index -> {
            final Entry entry = entries.get(index);
            return new ReportRow(rule + ":" + unit.name() + "#" + (index + 1), entry.kind(), unit.packageName(),
                    unit.name(), unit.codebase(), unit.source(), entry.signature().principle().getSimpleName(), rule,
                    entry.finding(), entry.details(), entry.excuse());
        });
    }

    /** What a rule came to on a unit: its findings, then its notices, then what it threw, each by its text. */
    private List<Entry> entries(final List<Applied> applied) {
        return Stream.<Function<Applied, Stream<Entry>>>of(this::findings, this::notices, this::failures)
                .flatMap(kind -> applied.stream().flatMap(kind).sorted(Comparator.comparing(Entry::details)))
                .toList();
    }

    private Stream<Entry> findings(final Applied applied) {
        return applied.result().stream().<Scalar>flatMap(result -> result.findings().items().stream())
                .map(finding -> entry(applied, applied.exempted() ? "exempted" : "finding", finding,
                        applied.exemption().map(exemption -> exemption.excuse().getSimpleName())));
    }

    private Stream<Entry> notices(final Applied applied) {
        return applied.result().stream().<Scalar>flatMap(result -> result.notices().items().stream())
                .map(notice -> entry(applied, "notice", notice, Optional.empty()));
    }

    private Stream<Entry> failures(final Applied applied) {
        return applied.failure().stream()
                .map(failure -> new Entry(applied.signature(), "failure", Optional.empty(), failure, Optional.empty()));
    }

    private Entry entry(final Applied applied, final String kind, final Scalar found, final Optional<String> excuse) {
        return new Entry(applied.signature(), kind, Optional.of(found.getClass().getSimpleName()),
                ResultText.INSTANCE.of(found), excuse);
    }

    private Located located(final JavaUnit unit, final Optional<Path> root) {
        return new Located(unit.type().getName(), unit.type().getPackageName(), codebase(unit.type()),
                unit.source().map(JavaSource::file).map(file -> written(file, root)));
    }

    /** A source file's path, with forward slashes: relative to the root, if it holds the file, and else whole. */
    private String written(final URI file, final Optional<Path> root) {
        if (!"file".equals(file.getScheme())) {
            return file.toString();
        }
        final Path path = Path.of(file).toAbsolutePath().normalize();
        return root.map(folder -> folder.toAbsolutePath().normalize())
                .filter(path::startsWith)
                .map(folder -> folder.relativize(path))
                .orElse(path)
                .toString().replace('\\', '/');
    }

    /** Where a class was loaded from, when that is known; a class of the JDK comes from nowhere that says. */
    private Optional<String> codebase(final Class<?> type) {
        final CodeSource source = type.getProtectionDomain().getCodeSource();
        return source == null ? Optional.empty() : Optional.of(named(source.getLocation()));
    }

    private String named(final URL location) {
        try {
            final Path classes = Path.of(location.toURI());
            return Files.isRegularFile(classes) ? classes.getFileName().toString() : moduleOf(classes);
        } catch (final URISyntaxException e) {
            return location.toString();
        }
    }

    /** The name of the module folder above {@code target}, or of the classes folder, outside a module's target. */
    private String moduleOf(final Path classes) {
        return Stream.iterate(classes.toAbsolutePath().normalize(), Path::getParent)
                .takeWhile(folder -> folder != null && folder.getParent() != null)
                .filter(folder -> folder.getFileName().toString().equals("target"))
                .findFirst()
                .map(target -> target.getParent())
                .orElse(classes)
                .getFileName().toString();
    }

    /** What a row says of its unit. */
    private record Located(String name, String packageName, Optional<String> codebase, Optional<String> source) {
    }

    /** One thing a rule came to on a unit. */
    private record Entry(EnforcerSignature signature, String kind, Optional<String> finding, String details,
                         Optional<String> excuse) {
    }
}
