package com.homing_labs.cq.bench;

import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.enforcer.def.Enforcers;
import com.homing_labs.cq.scan.Applied;
import com.homing_labs.cq.scan.Compliance;
import com.homing_labs.cq.scan.Ruleset;
import com.homing_labs.cq.scan.ScanResult;
import com.homing_labs.cq.scan.ScannedUnit;
import com.homing_labs.cq.scan.ScannedUnits;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Scalar;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The rule book, in a window of its own over the bench's: every rule of the bench's rulesets, once, with the principle
 * it serves and how far the units the bench last scanned comply with it. Selecting a rule explains it - the
 * principle, the rule, and each enforcer that checks it, in the words of their documentation comments - and lists all
 * it found on those units: each finding, and each unit it failed on. A rule is counted on whole units, as a build's
 * {@link com.homing_labs.cq.scan.Gate} counts it: what a sub-rule finds on a nested class is found by the rule it is
 * delegated from, on the unit the class is nested in.
 */
public final class RuleBookView implements Mutable {

    private static final String TITLE = "Rule book";

    private static final String FAILED = "failed: ";

    private final List<Ruleset> rulesets;

    private final List<Path> sources;

    private final ClassLoader loader;

    private final Label caption = new Label();

    private final TableView<Entry> rules = new TableView<>();

    private final VBox explanation = new VBox(4);

    private final Label found = new Label();

    private final TableView<Violation> violations = new TableView<>();

    private final BorderPane pane = new BorderPane();

    /** The documentation comment of each class explained, as read since the bench last scanned. */
    private final Map<Class<?>, Optional<String>> docs = new HashMap<>();

    /** The enforcers of the rulesets, when the bench last scanned: none, before it has. */
    private List<Enforcer<?, ?, ?>> enforcers = List.of();

    /** What the bench found when it last scanned: nothing, before it has. */
    private ScanResult result = new ScanResult(new ScannedUnits(List.of()), 0);

    /** The window, once it has been opened. */
    private Optional<Stage> window = Optional.empty();

    /**
     * The rule book of the rulesets, their enforcers as {@code loader} finds them, explained from the sources in
     * {@code sources} first, then from those beside each class's code base.
     */
    public RuleBookView(final List<Ruleset> rulesets, final List<Path> sources, final ClassLoader loader) {
        this.rulesets = List.copyOf(rulesets);
        this.sources = List.copyOf(sources);
        this.loader = loader;
        rules.setId("book-rules");
        rules.getColumns().add(column("Rule", (Entry entry) -> entry.rule().getSimpleName(), 220));
        rules.getColumns().add(column("Principle", (Entry entry) -> entry.principle().getSimpleName(), 170));
        final TableColumn<Entry, Label> comply = column("Comply",
                (Entry entry) -> Badges.INSTANCE.label(Badges.INSTANCE.ofRule(entry.compliance())), 60);
        comply.setSortable(false);
        rules.getColumns().add(comply);
        rules.getColumns().add(column("Findings", (Entry entry) -> entry.compliance().findings(), 70));
        rules.setPlaceholder(new Label("No rule in the rulesets"));
        rules.getSelectionModel().selectedItemProperty().addListener((observed, before, entry) ->
                present(Optional.ofNullable(entry)));
        violations.setId("violations");
        violations.getColumns().add(column("Unit", (Violation violation) -> violation.unit().getSimpleName(), 150));
        violations.getColumns().add(column("Package",
                (Violation violation) -> violation.unit().getPackageName(), 220));
        final TableColumn<Violation, String> finding = column("Finding", Violation::finding, 400);
        finding.setStyle("-fx-wrap-text: true;");
        violations.getColumns().add(finding);
        violations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        violations.setPlaceholder(new Label("Nothing found"));
        pane.setTop(caption);
        pane.setCenter(layout());
        present(Optional.empty());
        caption.setText("The bench has not scanned yet.");
    }

    /** Shows the rule book's window over the bench's, opening it if it is not open yet. */
    public void show(final Window owner) {
        final Stage stage = window.orElseGet(() -> {
            final Stage made = new Stage();
            made.initOwner(owner);
            made.setTitle(TITLE);
            made.setScene(new Scene(pane, 1400, 800));
            return made;
        });
        window = Optional.of(stage);
        stage.show();
        stage.toFront();
    }

    /**
     * Takes what the bench found whenever it scans: lists the rules again, counted on the units found, and keeps the
     * rule selected, if any.
     */
    public void scanned(final ScanResult result) {
        final Optional<Class<?>> selected = Optional.ofNullable(rules.getSelectionModel().getSelectedItem())
                .map(Entry::rule);
        this.result = result;
        docs.clear();
        rules.getSelectionModel().clearSelection();
        caption.setText(list());
        selected.ifPresent(this::select);
    }

    /** Selects a rule, by its class, if it is in the rulesets. */
    public void select(final Class<?> rule) {
        rules.getItems().stream().filter(entry -> entry.rule() == rule).findFirst()
                .ifPresent(entry -> rules.getSelectionModel().select(entry));
    }

    /** What the window shows. */
    public Parent content() {
        return pane;
    }

    /** The rules listed, by name. */
    public List<Entry> entries() {
        return List.copyOf(rules.getItems());
    }

    /** The explanation of the rule selected, line by line: headings and paragraphs. */
    public List<String> explanation() {
        return explanation.getChildren().stream().map(line -> ((Label) line).getText()).toList();
    }

    /** What the rule selected found, as said above its findings. */
    public String found() {
        return found.getText();
    }

    /** All the rule selected found: each of its findings on a unit, and each unit it failed on. */
    public List<Violation> violations() {
        return List.copyOf(violations.getItems());
    }

    /** The rules on the left; on the right, the rule selected explained, over all it found. */
    private SplitPane layout() {
        explanation.setId("explanation");
        explanation.setPadding(new Insets(8));
        final ScrollPane explained = new ScrollPane(explanation);
        explained.setFitToWidth(true);
        found.setPadding(new Insets(6, 8, 6, 8));
        final SplitPane right = new SplitPane(explained, new BorderPane(violations, found, null, null, null));
        right.setOrientation(Orientation.VERTICAL);
        right.setDividerPositions(0.42);
        caption.setPadding(new Insets(6, 8, 6, 8));
        final SplitPane split = new SplitPane(rules, right);
        split.setDividerPositions(0.4);
        return split;
    }

    /**
     * Lists the rules of the rulesets, counted on what the bench found, and says how many; or, if it cannot, says why
     * and lists none.
     */
    private String list() {
        try {
            enforcers = rulesets.stream().flatMap(ruleset -> ruleset.enforcers(loader).stream()).distinct().toList();
        } catch (final RuntimeException e) {
            enforcers = List.of();
            rules.getItems().clear();
            return "Cannot list the rules of " + names() + ": " + e.getMessage();
        }
        rules.getItems().setAll(ruleEntries());
        return count(rules.getItems().size(), "rule") + " of " + names() + ", checked by "
                + count(enforcers.size(), "enforcer") + " on the " + count(result.units().items().size(), "unit")
                + " the bench last scanned.";
    }

    /** Each rule of the enforcers once, by its simple name, with the principle it serves. */
    private List<Entry> ruleEntries() {
        final Map<Class<?>, Class<?>> principles = new LinkedHashMap<>();
        enforcers.stream().map(Enforcers.INSTANCE::signatureOf)
                .sorted(Comparator.comparing((EnforcerSignature signature) -> signature.rule().getSimpleName())
                        .thenComparing(signature -> signature.rule().getName()))
                .forEach(signature -> principles.putIfAbsent(signature.rule(), signature.principle()));
        return principles.entrySet().stream().map(rule -> entry(rule.getKey(), rule.getValue())).toList();
    }

    private Entry entry(final Class<?> rule, final Class<?> principle) {
        final List<List<Applied>> checked = result.units().items().stream().map(scanned -> applied(scanned, rule))
                .filter(applied -> !applied.isEmpty())
                .toList();
        return new Entry(principle, rule, checked.stream().map(Compliance::new)
                .reduce(Compliance.NONE, Compliance::plus),
                (int) checked.stream().filter(applied -> applied.stream().anyMatch(one -> one.result().isEmpty()))
                        .count());
    }

    /** The rule's enforcers as the scan applied them to the unit whole: none, when none checks units like it. */
    private List<Applied> applied(final ScannedUnit scanned, final Class<?> rule) {
        return scanned.applied().items().stream().filter(applied -> applied.signature().rule() == rule).toList();
    }

    private void present(final Optional<Entry> entry) {
        explanation.getChildren().setAll(entry.map(this::explained).orElseGet(() -> List.of(paragraph(
                "Select a rule to see the principle it serves, what it asks of the code, and how it is checked."))));
        violations.getItems().setAll(entry.map(this::violations).orElse(List.of()));
        found.setText(entry.map(this::found).orElse(""));
    }

    /** The principle, the rule, and each enforcer of it, each by its name, then in the words of its documentation. */
    private List<Node> explained(final Entry entry) {
        final List<Node> lines = new ArrayList<>(List.of(heading("Principle: " + entry.principle().getSimpleName()),
                paragraph(doc(entry.principle())), heading("Rule: " + entry.rule().getSimpleName()),
                paragraph(doc(entry.rule())), heading("Checked by")));
        enforcers.stream().filter(enforcer -> Enforcers.INSTANCE.signatureOf(enforcer).rule() == entry.rule())
                .forEach(enforcer -> {
                    lines.add(subheading(enforcer.getClass().getSimpleName() + ", on "
                            + unitName(Enforcers.INSTANCE.signatureOf(enforcer).unit())));
                    lines.add(paragraph(doc(enforcer.getClass())));
                });
        final List<Class<?>> excuses = excuses(entry);
        if (!excuses.isEmpty()) {
            lines.add(heading("Excused by"));
        }
        excuses.forEach(excuse -> {
            lines.add(subheading(excuse.getSimpleName()));
            lines.add(paragraph(doc(excuse)));
        });
        return lines;
    }

    /** Each excuse pleaded to the rule on the units scanned, once, by name: its reason is in its documentation. */
    private List<Class<?>> excuses(final Entry entry) {
        return result.units().items().stream()
                .flatMap(scanned -> applied(scanned, entry.rule()).stream())
                .flatMap(applied -> applied.exemption().stream())
                .<Class<?>>map(exemption -> exemption.excuse())
                .distinct()
                .sorted(Comparator.comparing((Class<?> excuse) -> excuse.getSimpleName()))
                .toList();
    }

    private String doc(final Class<?> type) {
        return docs.computeIfAbsent(type, documented -> ClassDocs.INSTANCE.of(documented, sources))
                .orElse("No documentation comment in the sources at hand.");
    }

    /** Each finding of the rule on each unit, by the unit's name, then each unit it failed on, saying why. */
    private List<Violation> violations(final Entry entry) {
        return result.units().items().stream()
                .flatMap(scanned -> applied(scanned, entry.rule()).stream()
                        .flatMap(applied -> applied.result().isPresent() ? applied.markedFindings().stream()
                                : applied.failure().stream().map(failure -> FAILED + failure))
                        .map(finding -> new Violation(scanned.unit().type(), finding)))
                .toList();
    }

    private String found(final Entry entry) {
        final Compliance compliance = entry.compliance();
        if (compliance.total() == 0) {
            return entry.rule().getSimpleName() + " checks none of the units scanned.";
        }
        final int exempted = result.units().items().stream()
                .flatMap(scanned -> applied(scanned, entry.rule()).stream())
                .filter(Applied::exempted).mapToInt(Applied::findingCount).sum();
        return compliance.compliant() + " of the " + count(compliance.total(), "unit") + " it checks "
                + (compliance.compliant() == 1 ? "complies" : "comply")
                + (compliance.findings() > 0 ? "; " + count(compliance.findings(), "finding") + " on the rest" : "")
                + (exempted > 0 ? ", " + exempted + " of them exempted" : "")
                + (entry.failed() > 0 ? "; it failed on " + count(entry.failed(), "unit") : "") + ".";
    }

    private Label heading(final String text) {
        final Label heading = paragraph(text);
        heading.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        heading.setPadding(new Insets(8, 0, 0, 0));
        return heading;
    }

    private Label subheading(final String text) {
        final Label subheading = paragraph(text);
        subheading.setStyle("-fx-font-weight: bold;");
        return subheading;
    }

    private Label paragraph(final String text) {
        final Label paragraph = new Label(text);
        paragraph.setWrapText(true);
        return paragraph;
    }

    private <T, V> TableColumn<T, V> column(final String title, final Function<T, V> value, final double width) {
        final TableColumn<T, V> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    /** The rulesets by their names: {@code no ruleset}, when there is none. */
    private String names() {
        return rulesets.isEmpty() ? "no ruleset"
                : rulesets.stream().map(Ruleset::name).collect(Collectors.joining(", "));
    }

    /** A unit type by its name where it is declared: {@code RecordUnit}, {@code JavaNestedUnit.Inner}. */
    private String unitName(final Class<?> unit) {
        return unit.getEnclosingClass() == null ? unit.getSimpleName()
                : unit.getEnclosingClass().getSimpleName() + "." + unit.getSimpleName();
    }

    private String count(final int count, final String thing) {
        return count + " " + thing + (count == 1 ? "" : "s");
    }

    /**
     * A rule of the rulesets, as the rule book lists it: the principle it serves, how far the units it checks comply
     * with it, and how many of them it failed on, which cannot be said to comply.
     */
    public record Entry(Class<?> principle, Class<?> rule, Compliance compliance, int failed) implements Scalar {
    }

    /** All a rule found on a unit, one line each: a finding, or, when it failed on the unit, why. */
    public record Violation(Class<?> unit, String finding) implements Scalar {
    }
}
