package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.taxonomy.Cardinality;
import com.homing_labs.cq.lang.java.taxonomy.MemberOf;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Placement;
import com.homing_labs.cq.lang.java.taxonomy.Taxonomy;
import com.homing_labs.cq.lang.java.taxonomy.scan.ClassScanner;
import com.homing_labs.cq.lang.java.taxonomy.scan.Occurrence;
import hue.captains.singapura.tao.ontology.Scalar;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Measures the class object, from the class files of the selection alone, in two parts. Above, a summary: a line for
 * each class, and one for each object the taxonomy lets a class of its kind have, in each place it can be - with how
 * many a class can have there, how many it has, and whether the two agree. Below, the instances found: those of the
 * summary line selected, or every one with no line selected.
 */
public final class ClassObjectView implements BenchView {

    private final TableView<Line> summary = new TableView<>();

    private final TableView<Row> details = new TableView<>();

    private final Label caption = new Label();

    private final BorderPane pane = new BorderPane();

    /** The instances found in the selection's class files. */
    private List<Occurrence> found = List.of();

    public ClassObjectView() {
        summary.setId("summary");
        summary.getColumns().add(column("Class", (Line line) -> shortName(line.inClass()), 170));
        summary.getColumns().add(column("Place", (Line line) -> line.place().map(this::placeName).orElse("-"), 170));
        summary.getColumns().add(column("Object", (Line line) -> kindName(line.object()), 200));
        summary.getColumns().add(column("Cardinality",
                (Line line) -> line.cardinality().map(allowed -> allowed.getClass().getSimpleName()).orElse("-"), 110));
        summary.getColumns().add(column("Count", Line::count, 70));
        summary.getColumns().add(column("Fits", (Line line) -> line.fits() ? "yes" : "no", 60));
        summary.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(final Line line, final boolean empty) {
                super.updateItem(line, empty);
                setStyle(line != null && !line.fits() ? "-fx-background-color: #f6d5d5;" : "");
            }
        });
        summary.setPlaceholder(new Label("Nothing selected"));
        summary.getSelectionModel().selectedItemProperty()
                .addListener((observed, before, line) -> details.getItems().setAll(rows(Optional.ofNullable(line))));
        details.setId("details");
        details.getColumns().add(column("Class", Row::type, 170));
        details.getColumns().add(column("Place", Row::place, 170));
        details.getColumns().add(column("Object", Row::object, 200));
        details.getColumns().add(column("Name", Row::name, 440));
        details.setPlaceholder(new Label("No instances"));
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
        found = selection.map(selected -> selected.classes().stream()
                        .flatMap(type -> ClassScanner.INSTANCE.scan(classFile(type)).stream())
                        .toList())
                .orElse(List.of());
        final List<Line> lines = summarise();
        summary.getItems().setAll(lines);
        details.getItems().setAll(rows(Optional.empty()));
        caption.setText(selection.map(selected -> {
            final long misfits = lines.stream().filter(line -> !line.fits()).count();
            return found.size() + " objects in " + selected.classes().size()
                    + (selected.classes().size() == 1 ? " class" : " classes") + ", read from the class files alone - "
                    + (misfits == 0 ? "every count fits its cardinality"
                    : misfits + (misfits == 1 ? " count does" : " counts do") + " not fit its cardinality");
        }).orElse("Select a unit, or a class nested in one, to see the objects in its class files."));
    }

    /** The summary's lines. */
    public List<Line> lines() {
        return List.copyOf(summary.getItems());
    }

    /** The instances shown below the summary. */
    public List<Row> rows() {
        return List.copyOf(details.getItems());
    }

    /** A line for each class found, and one for each object it can have in each of its places, counted. */
    private List<Line> summarise() {
        return found.stream()
                .flatMap(occurrence -> occurrence instanceof Occurrence.OfClass type ? Stream.of(type) : Stream.empty())
                .flatMap(type -> Stream.concat(
                        Stream.of(new Line(type.name(), Optional.empty(), type.kind().getClass(), Optional.empty(), 0)),
                        Taxonomy.INSTANCE.placements(type.kind()).stream()
                                .map(placement -> line(type.name(), placement))))
                .map(line -> line.counted(found))
                .toList();
    }

    private Line line(final String inClass, final Placement placement) {
        return new Line(inClass, Optional.of(placement.place()), placement.object(),
                Optional.of(placement.cardinality()), 0);
    }

    private List<Row> rows(final Optional<Line> line) {
        return found.stream()
                .filter(occurrence -> line.map(selected -> selected.holds(occurrence)).orElse(true))
                .map(this::row)
                .toList();
    }

    private <T, V> TableColumn<T, V> column(final String title, final Function<T, V> value, final double width) {
        final TableColumn<T, V> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private Row row(final Occurrence occurrence) {
        return switch (occurrence) {
            case Occurrence.OfClass type -> new Row(shortName(type.name()), "-", kindName(type.kind().getClass()),
                    type.name().replace('/', '.'));
            case Occurrence.OfMember member -> new Row(shortName(member.inClass()), placeName(member.place()),
                    kindName(member.kind().getClass()), member.name().replace('/', '.'));
        };
    }

    /** A class without its package: {@code SampleEnum$1}. */
    private String shortName(final String internalName) {
        return internalName.substring(internalName.lastIndexOf('/') + 1);
    }

    /** A place as the taxonomy names it, without {@code MemberOf}: {@code Enum.ClassObject}. */
    private String placeName(final Class<?> place) {
        final String name = place.getName();
        return name.substring(name.indexOf('$') + 1).replace('$', '.');
    }

    /** A kind of object as the taxonomy names it: {@code Method.Implementation}, {@code Field}. */
    private String kindName(final Class<?> kind) {
        return kind.getName().substring(kind.getPackageName().length() + 1).replace('$', '.');
    }

    private byte[] classFile(final Class<?> type) {
        final String name = type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("No class file for " + type.getName());
            }
            return in.readAllBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read the class file of " + type.getName(), e);
        }
    }

    /**
     * A line of the summary: a class itself, with no place, or an object in one of its places - with how many of it a
     * class can have there, and how many it has.
     */
    public record Line(String inClass, Optional<Class<? extends MemberOf>> place, Class<? extends ObjectKind> object,
                       Optional<Cardinality> cardinality, long count) implements Scalar {

        /** Whether the count is one the cardinality allows; a class itself always fits. */
        public boolean fits() {
            return cardinality.map(allowed -> allowed.allows(count)).orElse(true);
        }

        /** Whether an instance found is one of this line's. */
        public boolean holds(final Occurrence occurrence) {
            return switch (occurrence) {
                case Occurrence.OfClass type -> place.isEmpty() && type.name().equals(inClass);
                case Occurrence.OfMember member -> place.isPresent() && member.place() == place.get()
                        && member.inClass().equals(inClass) && member.kind().getClass() == object;
            };
        }

        /** This line, with the instances among those found counted. */
        public Line counted(final List<Occurrence> found) {
            return new Line(inClass, place, object, cardinality, found.stream().filter(this::holds).count());
        }
    }

    /** A row of the instances: the class an object was found in, its place there, the object, and its name. */
    public record Row(String type, String place, String object, String name) implements Scalar {
    }
}
