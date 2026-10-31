package com.homing_labs.cq.bench;

import com.homing_labs.cq.core.def.QcUnit;
import com.homing_labs.cq.enforcer.def.Enforcer;
import com.homing_labs.cq.enforcer.def.EnforcerSignature;
import com.homing_labs.cq.enforcer.def.Enforcers;
import com.homing_labs.cq.enforcer.def.Result;
import com.homing_labs.cq.lang.java.unit.AbstractClassUnit;
import com.homing_labs.cq.lang.java.unit.AnnotationUnit;
import com.homing_labs.cq.lang.java.unit.ClassUnit;
import com.homing_labs.cq.lang.java.unit.EnumUnit;
import com.homing_labs.cq.lang.java.unit.InterfaceUnit;
import com.homing_labs.cq.lang.java.unit.RecordUnit;
import com.homing_labs.cq.lang.java.unit.JavaNestedUnit;
import com.homing_labs.cq.lang.java.unit.JavaNests;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.model.ClassName;
import hue.captains.singapura.tao.ontology.Scalar;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The rules applied to the selected unit: every enforcer found under a rules package whose unit type the unit is of,
 * run on it - a rule for every unit, or one for its kind of class alone. Above, a line for each rule applied, with
 * its principle, the units it is for and its findings counted; below, the findings of the rule selected. A rule is
 * checked on the whole unit; selecting a member class nested in it shows the sub-rules on that class alone.
 */
public final class RulesView implements BenchView {

    private final ClassLoader loader;

    private final TextField rulesPackage;

    private final Label caption = new Label();

    private final TableView<Applied> rules = new TableView<>();

    private final ListView<String> findings = new ListView<>();

    private final BorderPane pane = new BorderPane();

    /** The enforcers found under the rules package. */
    private List<Enforcer<?, ?, ?>> enforcers = List.of();

    /** The selection shown. */
    private Optional<Selection> shown = Optional.empty();

    public RulesView(final ClassLoader loader, final String rulesPackage) {
        this.loader = loader;
        this.rulesPackage = new TextField(rulesPackage);
        this.rulesPackage.setPrefColumnCount(30);
        rules.setId("rules");
        rules.getColumns().add(column("Rule", (Applied applied) -> applied.signature().rule().getSimpleName(), 220));
        rules.getColumns().add(column("Principle",
                (Applied applied) -> applied.signature().principle().getSimpleName(), 220));
        rules.getColumns().add(column("For", (Applied applied) -> unitName(applied.signature().unit()), 150));
        rules.getColumns().add(column("Findings", Applied::outcome, 100));
        rules.setPlaceholder(new Label("No rule applies"));
        rules.getSelectionModel().selectedItemProperty().addListener((observed, before, applied) ->
                findings.getItems().setAll(Optional.ofNullable(applied).map(Applied::lines).orElse(List.of())));
        findings.setId("findings");
        findings.setPlaceholder(new Label("Select a rule to see its findings"));
        final Button find = new Button("Find");
        find.setOnAction(event -> find());
        this.rulesPackage.setOnAction(event -> find());
        caption.setPadding(new Insets(6, 8, 6, 8));
        final SplitPane split = new SplitPane(rules, findings);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.5);
        pane.setTop(new VBox(new ToolBar(new Label("Rules under"), this.rulesPackage, find), caption));
        pane.setCenter(split);
        find();
    }

    @Override
    public String title() {
        return "Rules applied";
    }

    @Override
    public Node content() {
        return pane;
    }

    @Override
    public void show(final Optional<Selection> selection) {
        shown = selection;
        final Optional<QcUnit<?>> checked = selection.map(this::checked);
        final List<Applied> applied = checked.map(this::apply).orElse(List.of());
        rules.getItems().setAll(applied);
        findings.getItems().clear();
        caption.setText(selection.map(selected -> applied.size() + " of " + count(enforcers.size(), "rule")
                        + " under " + rulesPackage.getText().trim() + " apply to " + what(selected, checked.get()))
                .orElse(count(enforcers.size(), "rule") + " found under " + rulesPackage.getText().trim()
                        + ". Select a unit to apply them."));
    }

    /**
     * The unit the selection is checked as: the top-level unit, or, for a member class nested in it, its nested unit -
     * so that sub-rules show on it. A local or anonymous class is no member, and is checked as its top-level unit.
     */
    private QcUnit<?> checked(final Selection selection) {
        if (selection.type() != selection.unit().type()) {
            final ClassName name = new ClassName(selection.type().getName());
            return JavaNests.INSTANCE.of(selection.unit()).stream()
                    .filter(nested -> nested.nested().object().name().equals(name))
                    .<QcUnit<?>>map(nested -> nested)
                    .findFirst().orElse(selection.unit());
        }
        return selection.unit();
    }

    private String what(final Selection selection, final QcUnit<?> checked) {
        return switch (checked) {
            case JavaNestedUnit.Static nested -> selection.type().getSimpleName() + ", a static nested class of "
                    + nested.top().type().getSimpleName() + " - sub-rules on it alone";
            case JavaNestedUnit.Inner nested -> selection.type().getSimpleName() + ", an inner class of "
                    + nested.top().type().getSimpleName() + " - sub-rules on it alone";
            case JavaUnit unit -> unit.type().getSimpleName() + ", " + kindName(unit) + " - checked on the whole unit";
            default -> selection.type().getSimpleName();
        };
    }

    /** The rules applied to the selection shown. */
    public List<Applied> applied() {
        return List.copyOf(rules.getItems());
    }

    /** The findings shown, of the rule selected. */
    public List<String> findings() {
        return List.copyOf(findings.getItems());
    }

    /** Finds the enforcers under the rules package again, and applies them to the selection shown. */
    private void find() {
        try {
            enforcers = ClasspathEnforcers.INSTANCE.read(rulesPackage.getText().trim(), loader);
            show(shown);
        } catch (final RuntimeException e) {
            enforcers = List.of();
            show(shown);
            caption.setText("Cannot find the rules under " + rulesPackage.getText().trim() + ": " + e.getMessage());
        }
    }

    /** Each enforcer that checks units like this one, run on it. */
    private List<Applied> apply(final QcUnit<?> unit) {
        return enforcers.stream()
                .filter(enforcer -> Enforcers.INSTANCE.signatureOf(enforcer).checks(unit))
                .map(enforcer -> run(enforcer, unit))
                .sorted(Comparator.comparing(applied -> applied.signature().rule().getSimpleName()))
                .toList();
    }

    private Applied run(final Enforcer<?, ?, ?> enforcer, final QcUnit<?> unit) {
        final EnforcerSignature signature = Enforcers.INSTANCE.signatureOf(enforcer);
        try {
            return new Applied(signature, Optional.of(Enforcers.INSTANCE.check(enforcer, unit)), Optional.empty());
        } catch (final RuntimeException e) {
            return new Applied(signature, Optional.empty(), Optional.of(String.valueOf(e)));
        }
    }

    private <T, V> TableColumn<T, V> column(final String title, final Function<T, V> value, final double width) {
        final TableColumn<T, V> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    /** A unit type by its name where it is declared: {@code RecordUnit}, {@code JavaNestedUnit.Inner}. */
    private String unitName(final Class<?> unit) {
        return unit.getEnclosingClass() == null ? unit.getSimpleName()
                : unit.getEnclosingClass().getSimpleName() + "." + unit.getSimpleName();
    }

    private String kindName(final JavaUnit unit) {
        return switch (unit) {
            case ClassUnit _ -> "a class";
            case AbstractClassUnit _ -> "an abstract class";
            case InterfaceUnit _ -> "an interface";
            case EnumUnit _ -> "an enum";
            case RecordUnit _ -> "a record";
            case AnnotationUnit _ -> "an annotation";
        };
    }

    private String count(final int count, final String thing) {
        return count + " " + thing + (count == 1 ? "" : "s");
    }

    /**
     * A rule applied to a unit: what its enforcer is for, and its result - or why it gave none, when it failed.
     */
    public record Applied(EnforcerSignature signature, Optional<Result<?, ?>> result, Optional<String> failure)
            implements Scalar {

        /** How the rule came out: its findings counted, {@code none}, or {@code failed}. */
        public String outcome() {
            return result.map(given -> given.hasFindings() ? String.valueOf(given.findings().items().size()) : "none")
                    .orElse("failed");
        }

        /**
         * Its findings, each shown without the packages of the classes it names, then its notices; or why it failed.
         */
        public List<String> lines() {
            return result.map(given -> Stream.concat(
                            given.findings().items().stream().map(Object::toString).sorted(),
                            given.notices().items().stream().map(notice -> "notice: " + notice).sorted())
                            .map(line -> line.replaceAll("(?:class )?(?:[a-z_][a-z0-9_]*\\.)+", ""))
                            .toList())
                    .orElseGet(() -> failure.stream().toList());
        }
    }
}
