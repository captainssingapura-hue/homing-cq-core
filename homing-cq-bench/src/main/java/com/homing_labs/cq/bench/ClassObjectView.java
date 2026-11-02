package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.model.ClassInitialisation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.InitialisationStep;
import com.homing_labs.cq.lang.java.model.InitialisationSteps;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.Values;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.taxonomy.Cardinality;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Placement;
import com.homing_labs.cq.lang.java.taxonomy.Taxonomy;
import com.homing_labs.cq.scan.ClassAttributes;
import com.homing_labs.cq.scan.UnitClasses;
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
 * unit, when the unit itself is selected - in two parts, and how each is initialised apart from them. Above, a
 * summary: a line for each class, naming it and its kind, then one for each object the taxonomy lets a class of its
 * kind have, with how many it can have and how many it has. Below, the values: those of the summary line selected, or
 * every one with no line selected, each class's led by the class itself. The unit's attributes were held to the
 * taxonomy when it was read, so every count fits its cardinality. Last, the initialisation: a line naming each class,
 * with how it is initialised, then each step its source writes for it. What is selected - a line, a value, or a step
 * - is what the source shows.
 */
public final class ClassObjectView implements BenchView {

    private final TableView<Line> summary = new TableView<>();

    private final TableView<Detail> details = new TableView<>();

    private final TableView<Stage> initialisation = new TableView<>();

    private final Label caption = new Label();

    private final BorderPane pane = new BorderPane();

    private final List<Consumer<Optional<Focus>>> listeners = new ArrayList<>();

    /** The summary's lines for the selection shown, each with its values. */
    private Map<Line, List<Detail>> measured = Map.of();

    /** What each line of the summary stands for: a class, or all of its values of an object. */
    private Map<Line, Focus> focuses = Map.of();

    public ClassObjectView() {
        summary.setId("summary");
        summary.getColumns().add(column("Object", this::object, 400));
        summary.getColumns().add(column("Cardinality",
                (Line line) -> line.cardinality().map(allowed -> allowed.getClass().getSimpleName()).orElse("-"), 110));
        summary.getColumns().add(column("Count", Line::count, 70));
        summary.setPlaceholder(new Label("Nothing selected"));
        summary.getSelectionModel().selectedItemProperty().addListener((observed, before, line) -> {
            if (line != null) {
                initialisation.getSelectionModel().clearSelection();
            }
            details.getItems().setAll(details(Optional.ofNullable(line)));
            tell();
        });
        details.setId("details");
        details.getColumns().add(column("Object", (Detail detail) -> detail.row().object(), 240));
        details.getColumns().add(column("Value", (Detail detail) -> detail.row().name(), 600));
        details.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        details.setPlaceholder(new Label("No values"));
        details.getSelectionModel().selectedItemProperty().addListener((observed, before, detail) -> {
            if (detail != null) {
                initialisation.getSelectionModel().clearSelection();
            }
            tell();
        });
        initialisation.setId("initialisation");
        initialisation.getColumns().add(column("Step", (Stage stage) -> stage.step().kind(), 240));
        initialisation.getColumns().add(column("Code", (Stage stage) -> stage.step().code(), 600));
        initialisation.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        initialisation.setPlaceholder(new Label("Nothing selected"));
        initialisation.getSelectionModel().selectedItemProperty().addListener((observed, before, stage) -> {
            if (stage != null) {
                details.getSelectionModel().clearSelection();
                summary.getSelectionModel().clearSelection();
            }
            tell();
        });
        caption.setPadding(new Insets(6, 8, 6, 8));
        final Label initialised = new Label("Initialisation - how each class's static state is set up, no part of it");
        initialised.setPadding(new Insets(6, 8, 6, 8));
        final SplitPane split = new SplitPane(summary, details,
                new BorderPane(initialisation, initialised, null, null, null));
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.4, 0.75);
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
        initialisation.getItems().setAll(selection.map(this::stages).orElse(List.of()));
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

    /** The steps of how the classes shown are initialised, or why one has none. */
    public List<Step> steps() {
        return initialisation.getItems().stream().map(Stage::step).toList();
    }

    /**
     * What is selected: the value below the summary, or else the summary's line, or else a step of the initialisation,
     * or nothing.
     */
    public Optional<Focus> focused() {
        final Detail detail = details.getSelectionModel().getSelectedItem();
        if (detail != null) {
            return Optional.of(detail.focus());
        }
        final Line line = summary.getSelectionModel().getSelectedItem();
        if (line != null) {
            return Optional.ofNullable(focuses.get(line));
        }
        return Optional.ofNullable(initialisation.getSelectionModel().getSelectedItem()).map(Stage::focus);
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
        final Map<Line, List<Detail>> lines = new LinkedHashMap<>();
        for (final ClassAttributes each : shown(selection)) {
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

    /** The classes selected: every class of the unit when the unit is selected, or the nested class selected alone. */
    private List<ClassAttributes> shown(final Selection selection) {
        return UnitClasses.INSTANCE.of(selection.unit()).stream()
                .filter(each -> selection.type() == selection.unit().type()
                        || each.name().binaryName().equals(selection.type().getName()))
                .toList();
    }

    /**
     * How each class selected is initialised: a row naming the class, with how it is initialised - which selecting
     * shows whole - then a row for each step its source writes, which selecting shows alone.
     */
    private List<Stage> stages(final Selection selection) {
        final List<Stage> stages = new ArrayList<>();
        for (final ClassAttributes each : shown(selection)) {
            final String type = shortName(each.name().binaryName());
            final WithSource<ClassInitialisation> initialised = each.initialisation();
            stages.add(new Stage(new Step(type, type, initialisation(initialised)),
                    new Focus.OfInitialisation(each.name(), initialised)));
            if (initialised.source() instanceof ProcessedSource.OfInitialisation written) {
                for (final InitialisationStep step : written.steps().items()) {
                    stages.add(new Stage(new Step(type, step.getClass().getSimpleName(), ValueText.INSTANCE.of(step)),
                            new Focus.OfInitialisation(each.name(), new WithSource<>(initialised.object(),
                                    new ProcessedSource.OfInitialisation(new InitialisationSteps(List.of(step)))))));
                }
            }
        }
        return stages;
    }

    /** How a class is initialised, in a line: in how many steps its source writes, or why it has none. */
    private String initialisation(final WithSource<ClassInitialisation> initialised) {
        return switch (initialised.object()) {
            case ClassInitialisation.None _ -> "No code runs: its static fields are constants, or keep their defaults";
            case ClassInitialisation.Code _ -> switch (initialised.source()) {
                case ProcessedSource.OfInitialisation written -> "Code runs, in " + written.steps().items().size()
                        + (written.steps().items().size() == 1 ? " step" : " steps") + ", written below";
                case ProcessedSource.Within<?> _ -> "Code runs, written somewhere in code not read yet";
                case ProcessedSource.Unavailable<?> _ -> "Code runs; the unit was read without its source";
                default -> "Code runs that the source does not write: the compiler's own";
            };
        };
    }

    /** What a summary line is of: an object of the taxonomy, or - on a class's own line - the class, and its kind. */
    private String object(final Line line) {
        return line.cardinality().isPresent() ? kindName(line.object())
                : shortName(line.inClass()) + " (" + kindName(line.object()) + ")";
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

    /**
     * A row of the initialisation: the class it is of, then what it shows - the class itself, with how it is
     * initialised, or a kind of step, with its code in a line.
     */
    public record Step(String type, String kind, String code) implements Scalar {
    }

    /** A row of the initialisation, and what selecting it shows in the source. */
    private record Stage(Step step, Focus focus) {
    }
}
