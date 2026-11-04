package com.homing_labs.cq.scan;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Notice;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.InterfaceUnit;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.scan.declarations.SeenRules;
import com.homing_labs.cq.scan.declarations.TaxonomyCode;
import com.homing_labs.cq.scan.rules.Neatness;
import com.homing_labs.cq.scan.rules.Seen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTest {

    private static final String DEFINITIONS = "com.homing_labs.cq.lang.java.def";

    private static final String JAVA_FIELD = DEFINITIONS + ".JavaField";

    private static final String ONTOLOGY = "hue.captains.singapura.tao.ontology";

    private static final Instant TIME = Instant.parse("2026-11-05T09:30:00Z");

    private final ClassLoader loader = getClass().getClassLoader();

    @Test
    void eachFindingIsARowWithItsRuleAndItsClass() {
        final ReportRow row = rowsOn(JAVA_FIELD, rows(DEFINITIONS, new FoundTwiceEnforcer())).getFirst();
        assertEquals("FoundTwice:" + JAVA_FIELD + "#1", row.id());
        assertEquals("finding", row.entry());
        assertEquals(DEFINITIONS, row.packageName());
        assertEquals(JAVA_FIELD, row.unit());
        assertEquals("Neatness", row.principle());
        assertEquals("FoundTwice", row.rule());
        assertEquals(Optional.of("Seen"), row.finding());
        assertEquals("Seen[unit=first]", row.details());
    }

    @Test
    void findingsOfARuleOnAClassAreNumberedByTheirTextAndEachClassAndRuleCountsFromOne() {
        final List<ReportRow> rows = rows(DEFINITIONS, new FoundTwiceEnforcer(), new AlsoFoundEnforcer());
        assertEquals(List.of("AlsoFound:" + JAVA_FIELD + "#1", "FoundTwice:" + JAVA_FIELD + "#1",
                        "FoundTwice:" + JAVA_FIELD + "#2"),
                rowsOn(JAVA_FIELD, rows).stream().map(ReportRow::id).toList());
        assertEquals(List.of("Seen[unit=also]", "Seen[unit=first]", "Seen[unit=second]"),
                rowsOn(JAVA_FIELD, rows).stream().map(ReportRow::details).toList());
        assertEquals(rows.size(), rows.stream().map(ReportRow::id).distinct().count());
    }

    @Test
    void rowsAreSortedByClass() {
        final List<String> units = rows(DEFINITIONS, new FoundTwiceEnforcer()).stream().map(ReportRow::unit).toList();
        assertEquals(units.stream().sorted().toList(), units);
    }

    @Test
    void aNoticeIsARowOfItsOwnAfterTheFindingsSoItNeverMovesTheirNumbers() {
        final List<ReportRow> rows = rowsOn(JAVA_FIELD, rows(DEFINITIONS, new FoundAndNotedEnforcer()));
        assertEquals(List.of("finding", "notice"), rows.stream().map(ReportRow::entry).toList());
        assertEquals("FoundAndNoted:" + JAVA_FIELD + "#2", rows.get(1).id());
        assertEquals(Optional.of("SourceUnavailable"), rows.get(1).finding());
        assertEquals("SourceUnavailable[of=Thing]", rows.get(1).details(), "the entry says it is a notice");
    }

    @Test
    void aRuleThatThrewIsAFailureRowWithNoFindingAndWhatItThrew() {
        final ReportRow row = rowsOn(JAVA_FIELD, rows(DEFINITIONS, new ThrowsEnforcer())).getFirst();
        assertEquals("failure", row.entry());
        assertEquals(Optional.empty(), row.finding());
        assertEquals("java.lang.IllegalStateException: one\ttwo\nthree\\four\rfive", row.details());
    }

    @Test
    void aCellHoldsTabsNewlinesCarriageReturnsAndBackslashesEscapedSoARowIsOneLine() {
        final List<ReportRow> rows = rows(DEFINITIONS, new ThrowsEnforcer());
        final List<String> lines = Report.INSTANCE.table(new ReportRows(rows)).lines().toList();
        assertEquals(rows.size() + 1, lines.size());
        final String[] cells = lines.get(1).split("\t");
        assertEquals(10, cells.length);
        assertEquals("java.lang.IllegalStateException: one\\ttwo\\nthree\\\\four\\rfive", cells[9]);
    }

    @Test
    void aSourceIsWrittenWholeOrRelativeToTheRootThatHoldsIt(@TempDir final Path elsewhere) {
        final String file = "homing-cq-lang-java/src/main/java/com/homing_labs/cq/lang/java/def/JavaField.java";
        final Declaration checked = checked(DEFINITIONS, new FoundTwiceEnforcer());
        final ScanResult result = Scanner.INSTANCE.scan(checked, loader);
        assertEquals(Optional.of(file), sourceOfJavaField(result, Optional.of(Path.of(".."))));
        final String whole = sourceOfJavaField(result, Optional.empty()).orElseThrow();
        assertTrue(whole.endsWith("/" + file) && Path.of(whole).isAbsolute() && !whole.contains("\\"), whole);
        assertEquals(Optional.of(whole), sourceOfJavaField(result, Optional.of(elsewhere)), "outside the root");
    }

    @Test
    void aUnitFromAJarWithNoSourceSaysTheJarAndHasNoSource() {
        final ReportRow row = rows(ONTOLOGY, new FoundTwiceEnforcer()).getFirst();
        assertEquals(Optional.empty(), row.source());
        assertTrue(row.codebase().orElseThrow().matches("core-.*\\.jar"), row.toString());
    }

    @Test
    void aUnitFromAClassesFolderSaysItsModule() {
        assertEquals(Optional.of("homing-cq-scan"),
                rows("com.homing_labs.cq.scan", new FoundTwiceEnforcer()).getFirst().codebase());
    }

    @Test
    void theTableStartsWithItsColumnsAndEndsEachLineWithANewlineAlone() {
        final String table = Report.INSTANCE.table(new ReportRows(rows(DEFINITIONS, new FoundTwiceEnforcer())));
        assertTrue(table.startsWith(
                "id\tentry\tpackage\tclass\tcodebase\tsource\tprinciple\trule\tfinding\tdetails\n"));
        assertTrue(table.endsWith("\n"));
        assertFalse(table.contains("\r"));
    }

    @Test
    void writingAScanTwiceGivesTheSameTableToTheByteAndNoByteOrderMark(@TempDir final Path folder) throws IOException {
        final Declaration checked = checked(DEFINITIONS, new FoundTwiceEnforcer());
        final ScanResult result = Scanner.INSTANCE.scan(checked, loader);
        Report.INSTANCE.write(result, checked, TIME, folder.resolve("one"), Optional.empty());
        Report.INSTANCE.write(result, checked, TIME.plusSeconds(60), folder.resolve("two"), Optional.empty());
        final byte[] table = Files.readAllBytes(folder.resolve("one").resolve(Report.TABLE));
        assertArrayEquals(table, Files.readAllBytes(folder.resolve("two").resolve(Report.TABLE)));
        assertEquals('i', table[0]);
    }

    @Test
    void theSummarySaysWhenItRanWhatItCoveredAndHowItCameOut() {
        final Declaration checked = checked(DEFINITIONS, new FoundTwiceEnforcer());
        final ScanResult result = Scanner.INSTANCE.scan(checked, loader);
        final int units = result.units().items().size();
        final String summary = Report.INSTANCE.summary(result, checked, TIME);
        assertTrue(summary.startsWith("# CQ report\n\nRan at 2026-11-05T09:30:00Z.\n\n"
                + "- Packages: `" + DEFINITIONS + "`\n"
                + "- Rulesets: `" + Listed.class.getName() + "`\n- Source folders: 0\n\n"
                + Gate.INSTANCE.closing(result) + "\n"), summary);
        assertTrue(summary.contains("| Units complying | 0 of " + units + " (0.0%) |\n"), summary);
        assertTrue(summary.contains("| Findings | " + 2 * units + " |\n| Notices | 0 |\n| Rules that threw | 0 |\n"),
                summary);
        assertTrue(summary.contains("| `" + FoundTwice.class.getName() + "` | `" + Neatness.class.getName() + "` | "
                + units + " | " + units + " | " + 2 * units + " | 0 |\n"), summary);
        assertTrue(summary.contains("| `" + DEFINITIONS + "` | 0 of " + units + " (0.0%) |\n"), summary);
        assertTrue(summary.contains("| `" + JAVA_FIELD + "` | 2 |\n"), summary);
    }

    @Test
    void theSummaryPutsTheWorstPackageFirstCountingEachPackagesOwnUnits() {
        final Declaration checked = checked(TaxonomyCode.INSTANCE.packages().getFirst(),
                new ScanPackageIsWorstEnforcer());
        final String summary = Report.INSTANCE.summary(Scanner.INSTANCE.scan(checked, loader), checked, TIME);
        final int worst = summary.indexOf("| `com.homing_labs.cq.lang.java.taxonomy.scan` | 0 of ");
        final int better = summary.indexOf("| `com.homing_labs.cq.lang.java.taxonomy` | ");
        assertTrue(worst > 0 && worst < better, summary);
    }

    @Test
    void theSummaryListsTwentyClassesAtMostByHowManyFindings() {
        final Declaration checked = checked(TaxonomyCode.INSTANCE.packages().getFirst(), new FoundTwiceEnforcer());
        final ScanResult result = Scanner.INSTANCE.scan(checked, loader);
        assertTrue(result.units().items().size() > 20);
        final String summary = Report.INSTANCE.summary(result, checked, TIME);
        final String classes = summary.substring(summary.indexOf("## The 20 classes"),
                summary.indexOf("## Units read without"));
        assertEquals(20, classes.lines().filter(line -> line.startsWith("| `")).count());
    }

    @Test
    void theSummaryListsEachUnitWithoutItsSourceAndEachRuleThatThrewInFull() {
        final Declaration checked = checked(ONTOLOGY, new ThrowsEnforcer());
        final ScanResult result = Scanner.INSTANCE.scan(checked, loader);
        final String summary = Report.INSTANCE.summary(result, checked, TIME);
        assertTrue(summary.contains("- `" + ONTOLOGY + ".Scalar`\n"), summary);
        assertTrue(summary.contains("`" + ONTOLOGY + ".Scalar`, `Throws`:\n\n```\n"
                + "java.lang.IllegalStateException: one\ttwo\nthree\\four\rfive\n```\n"), summary);
    }

    @Test
    void theSummarySaysWhenAScanCheckedNothingAndWhatItHasNoneOf() {
        final Declaration checked = checked("no.such.code", new FoundTwiceEnforcer());
        final String summary = Report.INSTANCE.summary(Scanner.INSTANCE.scan(checked, loader), checked, TIME);
        assertTrue(summary.contains("0 units by 1 rules: 1 failures\n\nno unit read: nothing was checked\n"), summary);
        assertTrue(summary.contains("## Rules that threw\n\nNone.\n"), summary);
    }

    @Test
    void theProgramWritesBothFilesAndEndsNormallyOnAScanWithFindings(@TempDir final Path folder) throws IOException {
        final Path out = folder.resolve("deep").resolve("report");
        Report.INSTANCE.main(new String[]{"--out", out.toString(), "--root", "..", TaxonomyCode.class.getName(),
                SeenRules.class.getName()});
        final String table = Files.readString(out.resolve("cq-report.tsv"));
        assertTrue(table.contains("EveryUnitIsSeen:com.homing_labs.cq.lang.java.taxonomy.ClassKind#1\tfinding\t"),
                table);
        assertTrue(Files.readString(out.resolve("cq-report.md")).startsWith("# CQ report\n"));
    }

    @Test
    void aBadArgumentIsRefusedSayingHowTheProgramIsUsed() {
        for (final String[] args : List.of(new String[]{}, new String[]{"--out", "folder"}, new String[]{"--out"},
                new String[]{"--nonsense", TaxonomyCode.class.getName()},
                new String[]{"no.such.Declaration"})) {
            assertThrows(IllegalArgumentException.class, () -> Report.INSTANCE.main(args), String.join(" ", args));
        }
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Report.INSTANCE.main(new String[]{"--out"}))
                .getMessage().contains("Usage: Report [--out <folder>] [--root <folder>] <declaration class>..."));
    }

    @Test
    void theProgramThrowsWhenItCannotWrite(@TempDir final Path folder) throws IOException {
        final Path taken = Files.writeString(folder.resolve("a-file"), "");
        assertThrows(UncheckedIOException.class, () -> Report.INSTANCE.main(new String[]{"--out", taken.toString(),
                TaxonomyCode.class.getName(), SeenRules.class.getName()}));
    }

    private Optional<String> sourceOfJavaField(final ScanResult result, final Optional<Path> root) {
        return rowsOn(JAVA_FIELD, Report.INSTANCE.rows(result, root).items()).getFirst().source();
    }

    private List<ReportRow> rows(final String rootPackage, final Enforcer<?, ?, ?>... enforcers) {
        return Report.INSTANCE.rows(Scanner.INSTANCE.scan(checked(rootPackage, enforcers), loader), Optional.empty())
                .items();
    }

    private List<ReportRow> rowsOn(final String unit, final List<ReportRow> rows) {
        return rows.stream().filter(row -> row.unit().equals(unit)).toList();
    }

    private Declaration checked(final String rootPackage, final Enforcer<?, ?, ?>... enforcers) {
        return new Declaration() {
            @Override
            public List<String> packages() {
                return List.of(rootPackage);
            }

            @Override
            public List<Ruleset> rulesets() {
                return List.of(new Listed(List.of(enforcers)));
            }
        };
    }

    /** A ruleset of the enforcers it lists. */
    private record Listed(List<Enforcer<?, ?, ?>> listed) implements Ruleset {

        @Override
        public String name() {
            return "listed";
        }

        @Override
        public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
            return listed;
        }
    }

    record FoundTwice() implements Rule<Neatness, JavaLang> {
    }

    record FoundTwiceEnforcer() implements Enforcer<JavaLang, FoundTwice, JavaUnit> {

        @Override
        public Result<JavaLang, FoundTwice> check(final JavaUnit unit) {
            return new Result<>(new Findings<>(Set.of(new Seen<>("second"), new Seen<>("first"))));
        }
    }

    record AlsoFound() implements Rule<Neatness, JavaLang> {
    }

    record AlsoFoundEnforcer() implements Enforcer<JavaLang, AlsoFound, JavaUnit> {

        @Override
        public Result<JavaLang, AlsoFound> check(final JavaUnit unit) {
            return new Result<>(new Findings<>(Set.of(new Seen<>("also"))));
        }
    }

    record FoundAndNoted() implements Rule<Neatness, JavaLang> {
    }

    record FoundAndNotedEnforcer() implements Enforcer<JavaLang, FoundAndNoted, JavaUnit> {

        @Override
        public Result<JavaLang, FoundAndNoted> check(final JavaUnit unit) {
            return new Result<JavaLang, FoundAndNoted>(new Findings<>(Set.of(new Seen<>("found"))))
                    .noting(new Notice.SourceUnavailable("com.example.Thing"));
        }
    }

    record Throws() implements Rule<Neatness, JavaLang> {
    }

    record ThrowsEnforcer() implements Enforcer<JavaLang, Throws, JavaUnit> {

        @Override
        public Result<JavaLang, Throws> check(final JavaUnit unit) {
            throw new IllegalStateException("one\ttwo\nthree\\four\rfive");
        }
    }

    record ScanPackageIsWorst() implements Rule<Neatness, JavaLang> {
    }

    /** Finds on every unit of the taxonomy's scan package, and on the interfaces of the rest. */
    record ScanPackageIsWorstEnforcer() implements Enforcer<JavaLang, ScanPackageIsWorst, JavaUnit> {

        @Override
        public Result<JavaLang, ScanPackageIsWorst> check(final JavaUnit unit) {
            return unit instanceof InterfaceUnit || unit.type().getPackageName().endsWith(".scan")
                    ? new Result<>(new Findings<>(Set.of(new Seen<>("found"))))
                    : new Result<>(new Findings<>(Set.of()));
        }
    }
}
