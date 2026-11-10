package com.homing_labs.cq.bench;

import com.homing_labs.cq.core.def.QcUnit;
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
import com.homing_labs.cq.scan.Applied;
import com.homing_labs.cq.scan.ScanResult;
import com.homing_labs.cq.scan.ScannedUnits;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The rules applied to the selected unit: every rule of the bench's rulesets whose unit type the unit is of, as the
 * scan ran it - a rule for every unit, or one for its kind of class alone. Above, a line for each rule applied, with
 * its principle, the units it is for and its findings counted; below, the findings of the rule selected. A rule is
 * checked on the whole unit; selecting a member class nested in it shows the sub-rules on that class alone.
 */
public final class RulesView implements BenchView {

    private final Label caption = new Label();

    private final TableView<Applied> rules = new TableView<>();

    private final ListView<String> findings = new ListView<>();

    private final BorderPane pane = new BorderPane();

    /** What the bench found when it last scanned: nothing, before it has. */
    private ScanResult result = new ScanResult(new ScannedUnits(List.of()), 0);

    /** The selection shown. */
    private Optional<Selection> shown = Optional.empty();

    public RulesView() {
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
        caption.setPadding(new Insets(6, 8, 6, 8));
        final SplitPane split = new SplitPane(rules, findings);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.5);
        pane.setTop(caption);
        pane.setCenter(split);
        show(shown);
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
    public void scanned(final ScanResult result) {
        this.result = result;
        show(shown);
    }

    @Override
    public void show(final Optional<Selection> selection) {
        shown = selection;
        final List<Applied> applied = selection.flatMap(selected -> result.of(selected.unit())
                .map(scanned -> scanned.appliedTo(new ClassName(selected.type().getName())).items())).orElse(List.of());
        rules.getItems().setAll(applied);
        findings.getItems().clear();
        caption.setText(selection.map(selected -> applied.size() + " of " + count(result.rules(), "rule")
                        + " apply to " + what(selected, checked(selected)))
                .orElse(count(result.rules(), "rule") + " in the rulesets. Select a unit to see those that apply"
                        + " to it."));
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
}
