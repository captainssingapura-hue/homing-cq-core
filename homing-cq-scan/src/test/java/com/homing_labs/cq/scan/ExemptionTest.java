package com.homing_labs.cq.scan;

import com.homing_labs.cq.core.def.Excuse;
import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Findings;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.taxonomy.MemberOf;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.scan.ClassScanner;
import com.homing_labs.cq.lang.java.unit.JavaNestedUnit;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.scan.declarations.ClassScannerExemptions;
import com.homing_labs.cq.scan.declarations.SeenRules;
import com.homing_labs.cq.scan.declarations.TaxonomyCode;
import com.homing_labs.cq.scan.rules.AnInterfaceOnPurpose;
import com.homing_labs.cq.scan.rules.EveryUnitIsSeen;
import com.homing_labs.cq.scan.rules.InterfacesAreSeen;
import com.homing_labs.cq.scan.rules.Neatness;
import com.homing_labs.cq.scan.rules.Seen;
import com.homing_labs.cq.scan.rules.SeenByAccident;
import com.homing_labs.cq.scan.rules.SeenOnPurpose;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExemptionTest {

    private static final String SCANNER = ClassScanner.class.getName();

    private static final String OBJECT_KIND = ObjectKind.class.getName();

    private final ClassLoader loader = getClass().getClassLoader();

    private final Declaration checked = TaxonomyCode.INSTANCE.and(new SeenRules());

    private final Exemption<ClassScanner, ?, ?> scanner =
            ClassScannerExemptions.INSTANCE.exemptions().iterator().next();

    /** The class that is not under the packages scanned. */
    private final Exemptions<Scanner> notScanned = new Given<>(Set.of(
            new Exemption<>(Scanner.class, EveryUnitIsSeen.class, SeenOnPurpose.class)));

    private final Exemptions<ObjectKind> objectKind = new Given<>(Set.of(
            new Exemption<>(ObjectKind.class, EveryUnitIsSeen.class, SeenOnPurpose.class)));

    @Test
    void anExemptionTakesOneFailureOffTheGateAndLeavesTheRest() {
        final String failure = SCANNER + " EveryUnitIsSeen: Seen[unit=ClassScanner]";
        final List<String> before = Gate.INSTANCE.failures(checked, loader);
        final List<String> after = Gate.INSTANCE.failures(exempting(checked, ClassScannerExemptions.INSTANCE), loader);
        assertTrue(before.contains(failure), before.toString());
        assertEquals(before.stream().filter(line -> !line.equals(failure)).toList(), after);
    }

    @Test
    void anExemptionCoversThatClassForThatRuleAndNothingWider() {
        final List<String> failures = Gate.INSTANCE.failures(exempting(checked, objectKind), loader);
        assertFalse(failures.contains(OBJECT_KIND + " EveryUnitIsSeen: Seen[unit=ObjectKind]"), failures.toString());
        assertTrue(failures.contains(OBJECT_KIND + " InterfacesAreSeen: Seen[unit=ObjectKind]"), failures.toString());
        assertTrue(failures.contains(SCANNER + " EveryUnitIsSeen: Seen[unit=ClassScanner]"), failures.toString());
    }

    @Test
    void aClassMayBeLetOffSeveralRulesEachWithItsOwnExcuse() {
        final Declaration twoRules = exempting(checked, new Given<>(Set.of(
                new Exemption<>(ObjectKind.class, EveryUnitIsSeen.class, SeenOnPurpose.class),
                new Exemption<>(ObjectKind.class, InterfacesAreSeen.class, AnInterfaceOnPurpose.class))));
        assertEquals(List.of(), Gate.INSTANCE.failures(twoRules, loader).stream()
                .filter(line -> line.startsWith(OBJECT_KIND + " ")).toList());
    }

    @Test
    void whatAnExemptionLetsOffStillRunsAndIsStillShownWithItsExcuse() {
        final Applied applied = onlyApplied(Scanner.INSTANCE.scan(exempting(checked, ClassScannerExemptions.INSTANCE),
                loader), ClassScanner.class);
        assertTrue(applied.exempted());
        assertEquals(Optional.of(scanner), applied.exemption());
        assertEquals(1, applied.findingCount());
        assertEquals(List.of("Seen[unit=ClassScanner]"), applied.findings());
        assertEquals(List.of("exempted (SeenOnPurpose): Seen[unit=ClassScanner]"), applied.markedFindings());
        assertEquals(List.of("exempted (SeenOnPurpose): Seen[unit=ClassScanner]"), applied.lines());
        assertEquals("1 exempted", applied.outcome());
    }

    @Test
    void anExemptionMovesNoGaugeAsItChangesNoCode() {
        final ScanResult free = Scanner.INSTANCE.scan(exempting(checked, ClassScannerExemptions.INSTANCE), loader);
        assertFalse(onlyApplied(free, ClassScanner.class).complies());
        assertEquals(Scanner.INSTANCE.scan(checked, loader).compliance(), free.compliance());
    }

    @Test
    void theClassesNestedInAnExemptedUnitAreLetOffWithIt() {
        final Declaration onNested = declaring(TaxonomyCode.INSTANCE.packages(),
                List.of(new Listed(List.of(new SeenInNestedEnforcer(), new SeenWholeEnforcer()))),
                List.of(new Given<>(Set.of(new Exemption<>(MemberOf.class, SeenWhole.class, SeenAllOver.class)))));
        final ScannedUnit memberOf = Scanner.INSTANCE.scan(onNested, loader).units().items().stream()
                .filter(scanned -> scanned.unit().type() == MemberOf.class).findFirst().orElseThrow();
        assertFalse(memberOf.members().items().isEmpty());
        memberOf.members().items().forEach(member -> member.applied().items().forEach(applied ->
                assertTrue(applied.exempted(), member.name().binaryName())));
    }

    @Test
    void aNestedClassCannotBeExemptedAloneAsARuleChecksTheUnitWhole() {
        assertEquals("Exempt the top-level class, " + ExemptionTest.class.getName() + ", not "
                        + Listed.class.getName() + " nested in it",
                assertThrows(IllegalArgumentException.class,
                        () -> new Exemption<>(Listed.class, EveryUnitIsSeen.class, SeenOnPurpose.class)).getMessage());
    }

    @Test
    void aClassPleadsOneExcuseToARule() {
        final Declaration twoExcuses = exempting(checked, new Given<>(Set.of(
                new Exemption<>(ObjectKind.class, EveryUnitIsSeen.class, SeenOnPurpose.class),
                new Exemption<>(ObjectKind.class, EveryUnitIsSeen.class, SeenByAccident.class))));
        assertEquals(OBJECT_KIND + " pleads 2 excuses to " + EveryUnitIsSeen.class.getName(),
                assertThrows(IllegalArgumentException.class, () -> Scanner.INSTANCE.scan(twoExcuses, loader))
                        .getMessage());
    }

    @Test
    void anExemptionARuleFindsNothingUnderIsStaleAndFailsTheGateNamingItsExcuse() {
        final Exemption<ClassScanner, FindsNothing, NothingToFind> exemption =
                new Exemption<>(ClassScanner.class, FindsNothing.class, NothingToFind.class);
        final ScanResult result = Scanner.INSTANCE.scan(declaring(TaxonomyCode.INSTANCE.packages(),
                List.of(new Listed(List.of(new FindsNothingEnforcer()))),
                List.of(new Given<>(Set.of(exemption)))), loader);
        assertEquals(List.of(new StaleExemption.NothingFound(exemption)), result.stale());
        assertEquals(List.of(SCANNER + " FindsNothing: NothingToFind is stale: the rule found nothing on the class"),
                Gate.INSTANCE.failures(result));
        assertFalse(onlyApplied(result, ClassScanner.class).exempted());
    }

    @Test
    void anExemptionOfAClassNotScannedOrARuleNotAppliedIsStaleEachInItsOwnWay() {
        final Exemption<ClassScanner, InterfacesAreSeen, AnInterfaceOnPurpose> notApplied =
                new Exemption<>(ClassScanner.class, InterfacesAreSeen.class, AnInterfaceOnPurpose.class);
        assertEquals(List.of(new StaleExemption.NotApplied(notApplied),
                        new StaleExemption.NotScanned(notScanned.exemptions().iterator().next())),
                Scanner.INSTANCE.scan(exempting(checked, notScanned, new Given<>(Set.of(notApplied))), loader).stale());
    }

    @Test
    void aRuleThatThrowsStaysAFailureAndLeavesItsExemptionNotStale() {
        final ScanResult result = Scanner.INSTANCE.scan(declaring(TaxonomyCode.INSTANCE.packages(),
                List.of(new Listed(List.of(new ThrowsEnforcer()))),
                List.of(new Given<>(Set.of(new Exemption<>(ClassScanner.class, Throws.class, ItThrows.class))))),
                loader);
        assertEquals(List.of(), result.stale());
        assertFalse(onlyApplied(result, ClassScanner.class).exempted());
        assertTrue(Gate.INSTANCE.failures(result).contains(
                SCANNER + " Throws: java.lang.IllegalStateException: thrown"));
    }

    @Test
    void anExemptionDeclaredTwiceCountsOnce() {
        final ScanResult result = Scanner.INSTANCE.scan(exempting(exempting(checked, ClassScannerExemptions.INSTANCE),
                new Given<>(Set.of(scanner))), loader);
        assertEquals(List.of(scanner), result.exemptions().items());
        assertTrue(Gate.INSTANCE.closing(result).endsWith(", 1 exemption used"), Gate.INSTANCE.closing(result));
    }

    @Test
    void declarationsDeclareNoExemptionsUnlessTheySayAndComposeEachClasssOnce() {
        assertEquals(List.of(), checked.exemptions());
        assertEquals(List.of(ClassScannerExemptions.INSTANCE, objectKind),
                exempting(new SeenRules(), ClassScannerExemptions.INSTANCE)
                        .and(exempting(TaxonomyCode.INSTANCE, objectKind, ClassScannerExemptions.INSTANCE))
                        .exemptions());
    }

    @Test
    void theGateSaysHowManyExemptionsItTookAndNothingWhenItTookNone() {
        final ScanResult result = Scanner.INSTANCE.scan(exempting(checked, ClassScannerExemptions.INSTANCE, objectKind,
                notScanned), loader);
        assertEquals(2, result.exemptionsUsed());
        assertTrue(Gate.INSTANCE.closing(result).endsWith(" failures, 2 exemptions used"),
                Gate.INSTANCE.closing(result));
        assertFalse(Gate.INSTANCE.closing(Scanner.INSTANCE.scan(checked, loader)).contains("exemption"));
    }

    @Test
    void anExemptedFindingKeepsItsRowAndItsIdAndSaysItIsExemptedAndByWhatExcuse() {
        final ReportRow before = row(Scanner.INSTANCE.scan(checked, loader), "EveryUnitIsSeen:" + SCANNER + "#1");
        final ReportRow after = row(Scanner.INSTANCE.scan(exempting(checked, ClassScannerExemptions.INSTANCE), loader),
                before.id());
        assertEquals("finding", before.entry());
        assertEquals(Optional.empty(), before.excuse());
        assertEquals("exempted", after.entry());
        assertEquals(Optional.of("SeenOnPurpose"), after.excuse());
        assertEquals(before.details(), after.details());
    }

    @Test
    void aStaleExemptionIsARowOfItsOwnSayingHowAndNamingItsExcuse() {
        final ScanResult result = Scanner.INSTANCE.scan(exempting(checked, notScanned), loader);
        final String name = Scanner.class.getName();
        assertEquals(new ReportRow("EveryUnitIsSeen:" + name + "#stale", "stale", Scanner.class.getPackageName(), name,
                        Optional.of("homing-cq-scan"), Optional.empty(), "", "EveryUnitIsSeen",
                        Optional.of("NotScanned"), "the class was not scanned", Optional.of("SeenOnPurpose")),
                row(result, "EveryUnitIsSeen:" + name + "#stale"));
    }

    @Test
    void theSummarySaysWhatIsExemptedAndWhatIsStaleAndCountsExemptedFindingsAsFindings() {
        final Declaration declared = exempting(checked, ClassScannerExemptions.INSTANCE, notScanned);
        final ScanResult result = Scanner.INSTANCE.scan(declared, loader);
        final String summary = Report.INSTANCE.summary(result, declared, Instant.EPOCH);
        assertTrue(summary.contains("- Exemptions: 2\n"), summary);
        assertTrue(summary.contains("| Findings | " + result.compliance().findings()
                + " |\n| Findings exempted | 1 |\n| Stale exemptions | 1 |\n"), summary);
        assertTrue(summary.contains("## Exempted\n\n| Class | Rule | Excuse | Findings let off |\n|---|---|---|---|\n"
                + "| `" + SCANNER + "` | `EveryUnitIsSeen` | `" + SeenOnPurpose.class.getName() + "` | 1 |\n"),
                summary);
        assertTrue(summary.contains("## Stale exemptions\n\n| Class | Rule | Excuse | How |\n|---|---|---|---|\n| `"
                + Scanner.class.getName() + "` | `EveryUnitIsSeen` | `" + SeenOnPurpose.class.getName()
                + "` | the class was not scanned |\n"), summary);
    }

    @Test
    void theSummarySaysWhenNothingIsExempted() {
        final String summary = Report.INSTANCE.summary(Scanner.INSTANCE.scan(checked, loader), checked, Instant.EPOCH);
        assertTrue(summary.contains("## Exempted\n\nNone.\n"), summary);
        assertTrue(summary.contains("## Stale exemptions\n\nNone.\n"), summary);
    }

    private Applied onlyApplied(final ScanResult result, final Class<?> unit) {
        return result.units().items().stream().filter(scanned -> scanned.unit().type() == unit)
                .flatMap(scanned -> scanned.applied().items().stream()).findFirst().orElseThrow();
    }

    private ReportRow row(final ScanResult result, final String id) {
        return Report.INSTANCE.rows(result, Optional.empty()).items().stream().filter(row -> row.id().equals(id))
                .findFirst().orElseThrow();
    }

    private Declaration exempting(final Declaration declaration, final Exemptions<?>... exemptions) {
        return declaration.and(new Declaration() {
            @Override
            public List<Exemptions<?>> exemptions() {
                return List.of(exemptions);
            }
        });
    }

    private Declaration declaring(final List<String> packages, final List<Ruleset> rulesets,
                                  final List<Exemptions<?>> exemptions) {
        return new Declaration() {
            @Override
            public List<String> packages() {
                return packages;
            }

            @Override
            public List<Ruleset> rulesets() {
                return rulesets;
            }

            @Override
            public List<Exemptions<?>> exemptions() {
                return exemptions;
            }
        };
    }

    /** A class's exemptions as a test gives them: the real thing is stateless, as {@link ClassScannerExemptions} is. */
    private record Given<C>(Set<Exemption<C, ?, ?>> exemptions) implements Exemptions<C> {
    }

    /** A ruleset of the enforcers it lists. */
    private record Listed(List<Enforcer<?, ?, ?>> listed) implements Ruleset {

        @Override
        public String name() {
            return "listed";
        }

        @Override
        public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader classLoader) {
            return listed;
        }
    }

    record FindsNothing() implements Rule<Neatness, JavaLang> {
    }

    record NothingToFind() implements Excuse<FindsNothing> {
    }

    record FindsNothingEnforcer() implements Enforcer<JavaLang, FindsNothing, JavaUnit> {

        @Override
        public Result<JavaLang, FindsNothing> check(final JavaUnit unit) {
            return new Result<>(new Findings<>(Set.of()));
        }
    }

    record Throws() implements Rule<Neatness, JavaLang> {
    }

    record ItThrows() implements Excuse<Throws> {
    }

    record ThrowsEnforcer() implements Enforcer<JavaLang, Throws, JavaUnit> {

        @Override
        public Result<JavaLang, Throws> check(final JavaUnit unit) {
            throw new IllegalStateException("thrown");
        }
    }

    record SeenWhole() implements Rule<Neatness, JavaLang> {
    }

    record SeenAllOver() implements Excuse<SeenWhole> {
    }

    record SeenWholeEnforcer() implements Enforcer<JavaLang, SeenWhole, JavaUnit> {

        @Override
        public Result<JavaLang, SeenWhole> check(final JavaUnit unit) {
            return new Result<>(new Findings<>(Set.of(new Seen<>("whole"))));
        }
    }

    record SeenInNestedEnforcer() implements Enforcer<JavaLang, SeenWhole, JavaNestedUnit> {

        @Override
        public Result<JavaLang, SeenWhole> check(final JavaNestedUnit unit) {
            return new Result<>(new Findings<>(Set.of(new Seen<>("nested"))));
        }
    }
}
