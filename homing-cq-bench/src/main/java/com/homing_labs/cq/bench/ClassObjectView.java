package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.Values;
import com.homing_labs.cq.lang.java.taxonomy.Cardinality;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Placement;
import com.homing_labs.cq.lang.java.taxonomy.Taxonomy;
import hue.captains.singapura.tao.ontology.Scalar;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The class object, as the selected unit holds it: the attributes of the class selected - or of every class of the
 * unit, when the unit itself is selected - in two parts. Above, a summary: a line for each class, and one for each
 * object the taxonomy lets a class of its kind have, with how many it can have and how many it has. Below, the values:
 * those of the summary line selected, or every one with no line selected. The unit's attributes were held to the
 * taxonomy when it was read, so every count fits its cardinality. What is selected - a line, or a value below it - is
 * what the source shows.
 */
public final class ClassObjectView implements BenchView {

    private final TableView<Line> summary = new TableView<>();

    private final TableView<Detail> details = new TableView<>();

    private final Label caption = new Label();

    private final BorderPane pane = new BorderPane();

    private final List<Consumer<Optional<Focus>>> listeners = new ArrayList<>();

    /** The summary's lines for the selection shown, each with its values. */
    private Map<Line, List<Detail>> measured = Map.of();

    /** What each line of the summary stands for: a class, or all of its values of an object. */
    private Map<Line, Focus> focuses = Map.of();

    public ClassObjectView() {
        summary.setId("summary");
        summary.getColumns().add(column("Class", (Line line) -> shortName(line.inClass()), 200));
        summary.getColumns().add(column("Object", (Line line) -> kindName(line.object()), 240));
        summary.getColumns().add(column("Cardinality",
                (Line line) -> line.cardinality().map(allowed -> allowed.getClass().getSimpleName()).orElse("-"), 110));
        summary.getColumns().add(column("Count", Line::count, 70));
        summary.setPlaceholder(new Label("Nothing selected"));
        summary.getSelectionModel().selectedItemProperty().addListener((observed, before, line) -> {
            details.getItems().setAll(details(Optional.ofNullable(line)));
            tell();
        });
        details.setId("details");
        details.getColumns().add(column("Class", (Detail detail) -> detail.row().type(), 200));
        details.getColumns().add(column("Object", (Detail detail) -> detail.row().object(), 240));
        details.getColumns().add(column("Value", (Detail detail) -> detail.row().name(), 600));
        details.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        details.setPlaceholder(new Label("No values"));
        details.getSelectionModel().selectedItemProperty().addListener((observed, before, detail) -> tell());
        caption.setPadding(new Insets(6, 8, 6, 8));
        final SplitPane split = new SplitPane(summary, details);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.5);
        pane.setTop(caption);
        pane.setCenter(split);
    }

    @Override
    public String title() {
        return "Class object";
    }

    @Override
    public Node content() {
        return pane;
    }

    @Override
    public void show(final Optional<Selection> selection) {
        focuses = new HashMap<>();
        measured = selection.map(this::measure).orElse(Map.of());
        summary.getItems().setAll(measured.keySet());
        details.getItems().setAll(details(Optional.empty()));
        caption.setText(selection.map(selected -> {
            final long classes = measured.keySet().stream().filter(line -> line.cardinality().isEmpty()).count();
            final long values = measured.values().stream().mapToLong(List::size).sum() - classes;
            return values + " values in " + classes + (classes == 1 ? " class" : " classes")
                    + ", from the attributes the unit holds";
        }).orElse("Select a unit, or a class nested in one, to see the attributes the unit holds for it."));
    }

    @Override
    public void onFocus(final Consumer<Optional<Focus>> listener) {
        listeners.add(listener);
    }

    /** The summary's lines. */
    public List<Line> lines() {
        return List.copyOf(summary.getItems());
    }

    /** The values shown below the summary. */
    public List<Row> rows() {
        return details.getItems().stream().map(Detail::row).toList();
    }

    /** What is selected: the value below the summary, or else the summary's line, or nothing. */
    public Optional<Focus> focused() {
        final Detail detail = details.getSelectionModel().getSelectedItem();
        if (detail != null) {
            return Optional.of(detail.focus());
        }
        return Optional.ofNullable(summary.getSelectionModel().getSelectedItem()).map(focuses::get);
    }

    private void tell() {
        final Optional<Focus> focused = focused();
        listeners.forEach(listener -> listener.accept(focused));
    }

    /**
     * A line for each class selected, and one for each object it can have, counted, with their values: every class of
     * the unit when the unit is selected, or the nested class selected alone.
     */
    private Map<Line, List<Detail>> measure(final Selection selection) {
        final List<ClassAttributes> shown = UnitClasses.INSTANCE.of(selection.unit()).stream()
                .filter(each -> selection.type() == selection.unit().type()
                        || each.name().binaryName().equals(selection.type().getName()))
                .toList();
        final Map<Line, List<Detail>> lines = new LinkedHashMap<>();
        for (final ClassAttributes each : shown) {
            final ClassName className = each.name();
            final String name = className.binaryName();
            final Class<? extends ObjectKind> kind = each.attributes().kind().getClass();
            final Focus ofClass = new Focus.OfClass(className);
            final Line classLine = new Line(name, kind, Optional.empty(), 1);
            focuses.put(classLine, ofClass);
            lines.put(classLine, List.of(new Detail(new Row(shortName(name), kindName(kind), name), ofClass)));
            for (final Placement placement : Taxonomy.INSTANCE.placements(each.attributes().kind())) {
                final Class<? extends ObjectKind> object = placement.object().getClass();
                final Values values = new Values(each.attributes().entry(placement.object()).all());
                final List<Detail> details = values.items().stream()
                        .map(value -> new Detail(
                                new Row(shortName(name), kindName(object), ValueText.INSTANCE.of(value.object())),
                                new Focus.OfValues(className, placement.object(), new Values(List.of(value)))))
                        .toList();
                final Line line = new Line(name, object, Optional.of(placement.cardinality()), details.size());
                focuses.put(line, new Focus.OfValues(className, placement.object(), values));
                lines.put(line, details);
            }
        }
        return lines;
    }

    private List<Detail> details(final Optional<Line> line) {
        return line.map(selected -> measured.getOrDefault(selected, List.of()))
                .orElseGet(() -> measured.values().stream().flatMap(List::stream).toList());
    }

    private <T, V> TableColumn<T, V> column(final String title, final Function<T, V> value, final double width) {
        final TableColumn<T, V> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    /** A class without its package: {@code SampleEnum$1}. */
    private String shortName(final String binaryName) {
        return binaryName.substring(binaryName.lastIndexOf('.') + 1);
    }

    /** A kind of object as the taxonomy names it: {@code StaticMethod.Implementation}, {@code Field}. */
    private String kindName(final Class<?> kind) {
        return kind.getName().substring(kind.getPackageName().length() + 1).replace('$', '.');
    }

    /**
     * A line of the summary: a class itself, with no cardinality, or an object it can have - with how many of it a
     * class can have, and how many it has.
     */
    public record Line(String inClass, Class<? extends ObjectKind> object, Optional<Cardinality> cardinality,
                       long count) implements Scalar {
    }

    /** A row of the values: the class a value is of, the object it is a value of, and the value in a line. */
    public record Row(String type, String object, String name) implements Scalar {
    }

    /** A row of the values, and what selecting it shows in the source. */
    private record Detail(Row row, Focus focus) {
    }
}
