package com.homing_labs.cq.bench;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * The class object, as the selected unit holds it: the attributes of the class selected - or of every class of the
 * unit, when the unit itself is selected - in two parts. Above, a summary: a line for each class, and one for each
 * object the taxonomy lets a class of its kind have, with how many it can have and how many it has. Below, the values:
 * those of the summary line selected, or every one with no line selected. The unit's attributes were held to the
 * taxonomy when it was read, so every count fits its cardinality.
 */
public final class ClassObjectView implements BenchView {

    private final TableView<Line> summary = new TableView<>();

    private final TableView<Row> details = new TableView<>();

    private final Label caption = new Label();

    private final BorderPane pane = new BorderPane();

    /** The summary's lines for the selection shown, each with its values. */
    private Map<Line, List<Row>> measured = Map.of();

    public ClassObjectView() {
        summary.setId("summary");
        summary.getColumns().add(column("Class", (Line line) -> shortName(line.inClass()), 200));
        summary.getColumns().add(column("Object", (Line line) -> kindName(line.object()), 240));
        summary.getColumns().add(column("Cardinality",
                (Line line) -> line.cardinality().map(allowed -> allowed.getClass().getSimpleName()).orElse("-"), 110));
        summary.getColumns().add(column("Count", Line::count, 70));
        summary.setPlaceholder(new Label("Nothing selected"));
        summary.getSelectionModel().selectedItemProperty()
                .addListener((observed, before, line) -> details.getItems().setAll(rows(Optional.ofNullable(line))));
        details.setId("details");
        details.getColumns().add(column("Class", Row::type, 200));
        details.getColumns().add(column("Object", Row::object, 240));
        details.getColumns().add(column("Value", Row::name, 600));
        details.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        details.setPlaceholder(new Label("No values"));
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
        measured = selection.map(this::measure).orElse(Map.of());
        summary.getItems().setAll(measured.keySet());
        details.getItems().setAll(rows(Optional.empty()));
        caption.setText(selection.map(selected -> {
            final long classes = measured.keySet().stream().filter(line -> line.cardinality().isEmpty()).count();
            final long values = measured.values().stream().mapToLong(List::size).sum() - classes;
            return values + " values in " + classes + (classes == 1 ? " class" : " classes")
                    + ", from the attributes the unit holds";
        }).orElse("Select a unit, or a class nested in one, to see the attributes the unit holds for it."));
    }

    /** The summary's lines. */
    public List<Line> lines() {
        return List.copyOf(summary.getItems());
    }

    /** The values shown below the summary. */
    public List<Row> rows() {
        return List.copyOf(details.getItems());
    }

    /**
     * A line for each class selected, and one for each object it can have, counted, with their values: every class of
     * the unit when the unit is selected, or the nested class selected alone.
     */
    private Map<Line, List<Row>> measure(final Selection selection) {
        final List<ClassAttributes> shown = UnitClasses.INSTANCE.of(selection.unit()).stream()
                .filter(each -> selection.type() == selection.unit().type()
                        || each.name().binaryName().equals(selection.type().getName()))
                .toList();
        final Map<Line, List<Row>> lines = new LinkedHashMap<>();
        for (final ClassAttributes each : shown) {
            final String name = each.name().binaryName();
            final Class<? extends ObjectKind> kind = each.attributes().kind().getClass();
            lines.put(new Line(name, kind, Optional.empty(), 1),
                    List.of(new Row(shortName(name), kindName(kind), name)));
            for (final Placement placement : Taxonomy.INSTANCE.placements(each.attributes().kind())) {
                final Class<? extends ObjectKind> object = placement.object().getClass();
                final List<Row> values = each.attributes().entry(placement.object()).all().stream()
                        .map(value -> new Row(shortName(name), kindName(object), ValueText.INSTANCE.of(value.object())))
                        .toList();
                lines.put(new Line(name, object, Optional.of(placement.cardinality()), values.size()), values);
            }
        }
        return lines;
    }

    private List<Row> rows(final Optional<Line> line) {
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
}
