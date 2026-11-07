package com.homing_labs.cq.scan;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The report's summary, in Markdown, for a person to read first: when it ran and over what, how it came out, the
 * totals, each rule, the packages and classes that need the most work, what is exempted, and what could not be checked.
 * Exempted findings count as findings here, as they do on the bench: an exemption changes no code.
 */
record ReportSummary() implements StatelessFunctionalObject {

    static final ReportSummary INSTANCE = new ReportSummary();

    private static final int MOST_FINDINGS = 20;

    /** The summary of a scan, made by a declaration at a time. */
    String text(final ScanResult result, final Declaration declaration, final Instant time) {
        return Stream.of(declared(declaration, time), outcome(result), totals(result), rules(result),
                        packages(result), classes(result), exempted(result), stale(result), unsourced(result),
                        threw(result))
                .flatMap(List::stream)
                .collect(Collectors.joining("\n", "", "\n"));
    }

    private List<String> declared(final Declaration declaration, final Instant time) {
        return List.of("# CQ report", "",
                "Ran at " + time + ".", "",
                "- Packages: " + quoted(declaration.packages()),
                "- Rulesets: " + quoted(declaration.rulesets().stream().map(set -> set.getClass().getName()).toList()),
                "- Source folders: " + declaration.sources().size(),
                "- Exemptions: " + declaration.exemptions().stream().mapToInt(forClass -> forClass.exemptions().size())
                        .sum());
    }

    private List<String> outcome(final ScanResult result) {
        final List<String> lines = new ArrayList<>(List.of("", Gate.INSTANCE.closing(result)));
        Gate.INSTANCE.nothingChecked(result).forEach(reason -> lines.addAll(List.of("", reason)));
        return lines;
    }

    private List<String> totals(final ScanResult result) {
        final List<Applied> applied = applied(result).toList();
        return List.of("", "## Totals", "", "| | |", "|---|---|",
                "| Units complying | " + complying(result.compliance()) + " |",
                "| Findings | " + result.compliance().findings() + " |",
                "| Findings exempted | " + applied.stream().filter(Applied::exempted)
                        .mapToInt(Applied::findingCount).sum() + " |",
                "| Stale exemptions | " + result.stale().size() + " |",
                "| Notices | " + applied.stream().mapToInt(this::notices).sum() + " |",
                "| Rules that threw | " + applied.stream().filter(this::threw).count() + " |");
    }

    private List<String> rules(final ScanResult result) {
        final List<String> rows = result.units().items().stream()
                .flatMap(scanned -> scanned.applied().items().stream()
                        .collect(Collectors.groupingBy(applied -> applied.signature().rule())).values().stream())
                .collect(Collectors.groupingBy(onOneUnit -> onOneUnit.getFirst().signature().rule()))
                .entrySet().stream()
                .sorted(Comparator.comparing(rule -> rule.getKey().getName()))
                .map(rule -> ruleRow(rule.getValue()))
                .toList();
        return section("## Rules", rows, List.of(
                "| Rule | Principle | Units checked | Units with findings | Findings | Units it threw on |",
                "|---|---|---|---|---|---|"));
    }

    /** A rule's row, from what it came to on each unit it checked. */
    private String ruleRow(final List<List<Applied>> onEachUnit) {
        final Applied any = onEachUnit.getFirst().getFirst();
        return "| " + String.join(" | ", "`" + any.signature().rule().getName() + "`",
                "`" + any.signature().principle().getName() + "`",
                String.valueOf(onEachUnit.size()),
                String.valueOf(onEachUnit.stream()
                        .filter(unit -> unit.stream().anyMatch(applied -> applied.findingCount() > 0)).count()),
                String.valueOf(onEachUnit.stream().flatMap(List::stream).mapToInt(Applied::findingCount).sum()),
                String.valueOf(onEachUnit.stream().filter(unit -> unit.stream().anyMatch(this::threw)).count()))
                + " |";
    }

    /** The packages below 100%, each counting its own units, the worst first, then by name. */
    private List<String> packages(final ScanResult result) {
        final List<String> rows = result.units().items().stream()
                .collect(Collectors.groupingBy(scanned -> scanned.unit().type().getPackageName(),
                        Collectors.reducing(Compliance.NONE, ScannedUnit::compliance, Compliance::plus)))
                .entrySet().stream()
                .filter(found -> !found.getValue().complete())
                .sorted(Comparator.<Map.Entry<String, Compliance>>comparingDouble(
                                found -> found.getValue().share())
                        .thenComparing(Map.Entry::getKey))
                .map(found -> "| `" + found.getKey() + "` | " + complying(found.getValue()) + " |")
                .toList();
        return section("## Packages below 100%", rows, List.of("| Package | Units complying |", "|---|---|"));
    }

    private List<String> classes(final ScanResult result) {
        final List<String> rows = result.units().items().stream()
                .filter(scanned -> scanned.compliance().findings() > 0)
                .sorted(Comparator.<ScannedUnit>comparingInt(scanned -> -scanned.compliance().findings())
                        .thenComparing(scanned -> scanned.unit().type().getName()))
                .limit(MOST_FINDINGS)
                .map(scanned -> "| `" + scanned.unit().type().getName() + "` | "
                        + scanned.compliance().findings() + " |")
                .toList();
        return section("## The " + MOST_FINDINGS + " classes with the most findings", rows,
                List.of("| Class | Findings |", "|---|---|"));
    }

    /** Each rule a class is let off, by class and then by rule, with the excuse it pleads and what it let off. */
    private List<String> exempted(final ScanResult result) {
        final List<String> rows = result.units().items().stream()
                .sorted(Comparator.comparing(scanned -> scanned.unit().type().getName()))
                .flatMap(scanned -> scanned.applied().items().stream().filter(Applied::exempted)
                        .map(applied -> "| `" + scanned.unit().type().getName() + "` | `"
                                + applied.signature().rule().getSimpleName() + "` | `"
                                + applied.exemption().orElseThrow().excuse().getName() + "` | "
                                + applied.findingCount() + " |"))
                .toList();
        return section("## Exempted", rows, List.of("| Class | Rule | Excuse | Findings let off |",
                "|---|---|---|---|"));
    }

    private List<String> stale(final ScanResult result) {
        final List<String> rows = result.stale().stream()
                .map(stale -> "| `" + stale.exemption().offender().getName() + "` | `"
                        + stale.exemption().rule().getSimpleName() + "` | `"
                        + stale.exemption().excuse().getName() + "` | " + stale.text() + " |")
                .toList();
        return section("## Stale exemptions", rows, List.of("| Class | Rule | Excuse | How |", "|---|---|---|---|"));
    }

    private List<String> unsourced(final ScanResult result) {
        return section("## Units read without their source", result.units().items().stream()
                .filter(scanned -> scanned.unit().source().isEmpty())
                .map(scanned -> "- `" + scanned.unit().type().getName() + "`")
                .toList(), List.of());
    }

    private List<String> threw(final ScanResult result) {
        return section("## Rules that threw", result.units().items().stream()
                .flatMap(scanned -> scanned.applied().items().stream().filter(this::threw)
                        .flatMap(applied -> Stream.of("`" + scanned.unit().type().getName() + "`, `"
                                        + applied.signature().rule().getSimpleName() + "`:", "", "```",
                                applied.failure().orElseThrow(), "```", "")))
                .toList(), List.of());
    }

    /** A section under its heading: its rows under their header, or that it has none. */
    private List<String> section(final String heading, final List<String> rows, final List<String> header) {
        final List<String> lines = new ArrayList<>(List.of("", heading, ""));
        if (rows.isEmpty()) {
            lines.add("None.");
            return lines;
        }
        lines.addAll(header);
        lines.addAll(rows);
        return lines;
    }

    private Stream<Applied> applied(final ScanResult result) {
        return result.units().items().stream().flatMap(scanned -> scanned.applied().items().stream());
    }

    private int notices(final Applied applied) {
        return applied.result().map(given -> given.notices().items().size()).orElse(0);
    }

    private boolean threw(final Applied applied) {
        return applied.failure().isPresent();
    }

    private String complying(final Compliance compliance) {
        return compliance.compliant() + " of " + compliance.total()
                + String.format(Locale.ROOT, " (%.1f%%)", 100 * compliance.share());
    }

    private String quoted(final List<String> names) {
        return names.isEmpty() ? "none" : names.stream().map(name -> "`" + name + "`")
                .collect(Collectors.joining(", "));
    }
}
