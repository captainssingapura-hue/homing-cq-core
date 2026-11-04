package com.homing_labs.cq.bench;

import com.homing_labs.cq.core.def.Rule;
import com.homing_labs.cq.core.def.lang.JavaLang;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.unit.InterfaceUnit;
import com.homing_labs.cq.lang.java.unit.RecordUnit;
import com.homing_labs.cq.scan.Compliance;
import com.homing_labs.cq.scan.PackageRuleset;
import com.homing_labs.cq.scan.Ruleset;
import com.homing_labs.cq.scan.ScanResult;
import com.homing_labs.cq.scan.ScannedUnit;
import com.homing_labs.cq.scan.Scanner;
import com.homing_labs.cq.scan.rules.EveryUnitIsSeen;
import com.homing_labs.cq.scan.rules.InterfacesAreSeen;
import com.homing_labs.cq.scan.rules.Neatness;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rule book over the taxonomy module, checked by the scan's test rules, their sources given as a declaration's
 * would be.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RuleBookViewTest {

    private static final String TAXONOMY = "com.homing_labs.cq.lang.java.taxonomy";

    private static final String RULES = "com.homing_labs.cq.scan.rules";

    /** Where the scan's test rules are written, from this module's directory. */
    private static final Path RULE_SOURCES = Path.of("..", "homing-cq-scan", "src", "test", "java");

    private static final String NEATNESS = "A principle for the scan's tests, and the bench's: every unit is looked at.";

    private final ClassLoader loader = RuleBookViewTest.class.getClassLoader();

    private final List<Ruleset> rules = List.of(new PackageRuleset(RULES));

    private final ScanResult scanned = Scanner.INSTANCE.scan(List.of(TAXONOMY), rules, List.of(), loader);

    @BeforeAll
    void startJavaFx() {
        FxThread.INSTANCE.start();
    }

    @Test
    void eachRuleIsListedOnceWithItsPrincipleAndHowFarTheUnitsItChecksComplyWithIt() {
        final int units = scanned.units().items().size();
        final int interfaces = units(InterfaceUnit.class).size();
        assertTrue(interfaces > 0 && interfaces < units);
        assertEquals(List.of(new RuleBookView.Entry(Neatness.class, EveryUnitIsSeen.class,
                                new Compliance(0, units, units), 0),
                        new RuleBookView.Entry(Neatness.class, InterfacesAreSeen.class,
                                new Compliance(0, interfaces, interfaces), 0)),
                FxThread.INSTANCE.call(() -> book(rules).entries()));
    }

    @Test
    void selectingARuleExplainsItsPrincipleTheRuleAndEachEnforcerInTheWordsOfTheirDocumentation() {
        final RuleBookView book = FxThread.INSTANCE.call(() -> book(rules));
        assertEquals(List.of("Principle: Neatness", NEATNESS,
                        "Rule: InterfacesAreSeen", "Neatness for Java interfaces alone: each is seen.",
                        "Checked by", "InterfacesAreSeenEnforcer, on InterfaceUnit",
                        "Checks InterfacesAreSeen on interfaces alone; it has no INSTANCE, so it is found by its"
                                + " constructor."),
                FxThread.INSTANCE.call(() -> {
                    book.select(InterfacesAreSeen.class);
                    return book.explanation();
                }));
    }

    @Test
    void selectingARuleListsEveryFindingOfItOnTheUnitsScanned() {
        final RuleBookView book = FxThread.INSTANCE.call(() -> book(rules));
        final List<RuleBookView.Violation> violations = FxThread.INSTANCE.call(() -> {
            book.select(InterfacesAreSeen.class);
            return book.violations();
        });
        assertEquals(units(InterfaceUnit.class).stream()
                        .map(unit -> new RuleBookView.Violation(unit, "Seen[unit=" + unit.getSimpleName() + "]"))
                        .toList(),
                violations);
        assertTrue(violations.contains(new RuleBookView.Violation(ClassKind.class, "Seen[unit=ClassKind]")));
        final int interfaces = violations.size();
        assertEquals("0 of the " + interfaces + " units it checks comply; " + interfaces + " findings on the rest.",
                FxThread.INSTANCE.call(book::found));
    }

    @Test
    void aRuleThatFailsOnAUnitSaysWhyAndTheUnitCannotBeSaidToComply() {
        final List<Ruleset> unchecked = List.of(new Listed("unchecked", List.of(new UncheckedEnforcer())));
        final RuleBookView book = FxThread.INSTANCE.call(() -> book(unchecked));
        final List<Class<?>> records = units(RecordUnit.class);
        assertTrue(!records.isEmpty());
        assertEquals(List.of(new RuleBookView.Entry(Neatness.class, Unchecked.class,
                        new Compliance(0, records.size(), 0), records.size())),
                FxThread.INSTANCE.call(book::entries));
        assertEquals(records.stream().map(unit -> new RuleBookView.Violation(unit,
                        "failed: java.lang.IllegalStateException: it cannot be checked")).toList(),
                FxThread.INSTANCE.call(() -> {
                    book.select(Unchecked.class);
                    return book.violations();
                }));
        assertEquals("0 of the " + records.size() + " units it checks comply; it failed on " + records.size()
                + " units.", FxThread.INSTANCE.call(book::found));
    }

    @Test
    void aMemberClassIsExplainedFromTheSourceBesideItsCodeBase() {
        final RuleBookView book = FxThread.INSTANCE.call(() -> book(List.of(new Listed("unchecked",
                List.of(new UncheckedEnforcer())))));
        assertEquals(List.of("Principle: Neatness", NEATNESS,
                        "Rule: Unchecked", "A rule for the rule book's tests: none of it can be checked.",
                        "Checked by", "UncheckedEnforcer, on RecordUnit", "Checks Unchecked on records, and cannot."),
                FxThread.INSTANCE.call(() -> {
                    book.select(Unchecked.class);
                    return book.explanation();
                }));
    }

    @Test
    void aClassWithNoDocumentationCommentOrNoSourceAtHandHasNone() {
        assertEquals(Optional.empty(), ClassDocs.INSTANCE.of(Undocumented.class, List.of()));
        assertEquals(Optional.empty(), ClassDocs.INSTANCE.of(String.class, List.of()), "the JDK's has no source");
        assertEquals(Optional.of(NEATNESS), ClassDocs.INSTANCE.of(Neatness.class, List.of(RULE_SOURCES)));
    }

    @Test
    void aClassFromAJarInARepositoryIsExplainedFromTheSourcesPackedOnTheClassPath() {
        assertEquals(Optional.of("What a jar of rules packs: this stands for the source of a class from a jar in a "
                        + "repository, which has no source directory, for the rule book's tests."),
                ClassDocs.INSTANCE.of(org.junit.jupiter.api.Test.class, List.of()));
    }

    @Test
    void theRuleSelectedStaysSelectedWhenTheBenchScansAgain() {
        final RuleBookView book = FxThread.INSTANCE.call(() -> book(rules));
        final List<String> before = FxThread.INSTANCE.call(() -> {
            book.select(InterfacesAreSeen.class);
            return book.explanation();
        });
        assertEquals(before, FxThread.INSTANCE.call(() -> {
            book.scanned(Scanner.INSTANCE.scan(List.of(TAXONOMY), rules, List.of(), loader));
            return book.explanation();
        }));
        assertEquals(units(InterfaceUnit.class).size(), FxThread.INSTANCE.call(book::violations).size());
    }

    @Test
    void theToolBarOpensOneRuleBookOverTheBenchCountedOnItsScan() {
        final List<String> opened = FxThread.INSTANCE.call(() -> {
            final Stage owner = new Stage();
            owner.setScene(new Scene(BenchPane.INSTANCE.content(TAXONOMY, rules, List.of(), loader, List.of(),
                    new SourceView()), 1280, 800));
            owner.show();
            final Button button = (Button) owner.getScene().getRoot().lookup("#rule-book-window");
            button.fire();
            button.fire();
            final List<Stage> windows = Window.getWindows().stream()
                    .flatMap(window -> window instanceof Stage stage && stage.getOwner() == owner
                            ? Stream.of(stage) : Stream.empty())
                    .toList();
            final List<String> seen = windows.stream().map(stage -> stage.getTitle() + ": " + checked(stage))
                    .toList();
            windows.forEach(Stage::close);
            owner.close();
            return seen;
        });
        final int units = scanned.units().items().size();
        assertEquals(List.of("Rule book: EveryUnitIsSeen checks " + units + ", InterfacesAreSeen checks "
                + units(InterfaceUnit.class).size()), opened, "opening it again brings the one window up");
    }

    /** The rule book of the rulesets, given what the bench found scanning the taxonomy module by them. */
    private RuleBookView book(final List<Ruleset> rulesets) {
        final RuleBookView book = new RuleBookView(rulesets, List.of(RULE_SOURCES), loader);
        book.scanned(Scanner.INSTANCE.scan(List.of(TAXONOMY), rulesets, List.of(), loader));
        return book;
    }

    /** The units scanned of a kind, by class, in the order scanned. */
    private List<Class<?>> units(final Class<?> kind) {
        return scanned.units().items().stream().map(ScannedUnit::unit).filter(kind::isInstance)
                .<Class<?>>map(unit -> unit.type()).toList();
    }

    /** How many units each rule of the rule book in a window checks. */
    private String checked(final Stage window) {
        @SuppressWarnings("unchecked") final TableView<RuleBookView.Entry> table =
                (TableView<RuleBookView.Entry>) window.getScene().getRoot().lookup("#book-rules");
        return String.join(", ", table.getItems().stream()
                .map(entry -> entry.rule().getSimpleName() + " checks " + entry.compliance().total()).toList());
    }

    /** A ruleset of the enforcers it lists. */
    private record Listed(String name, List<Enforcer<?, ?, ?>> listed) implements Ruleset {

        @Override
        public List<Enforcer<?, ?, ?>> enforcers(final ClassLoader loader) {
            return listed;
        }
    }

    /** A rule for the rule book's tests: none of it can be checked. */
    record Unchecked() implements Rule<Neatness, JavaLang> {
    }

    /** Checks {@link Unchecked} on records, and cannot. */
    record UncheckedEnforcer() implements Enforcer<JavaLang, Unchecked, RecordUnit> {

        @Override
        public Result<JavaLang, Unchecked> check(final RecordUnit unit) {
            throw new IllegalStateException("it cannot be checked");
        }
    }

    record Undocumented() {
    }
}
