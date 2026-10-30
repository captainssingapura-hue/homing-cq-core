package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.taxonomy.scan.ClassScanner;
import com.homing_labs.cq.lang.java.taxonomy.scan.Occurrence;
import hue.captains.singapura.tao.ontology.Scalar;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Measures the class object: every object of the taxonomy in the class files of the selection - the class, its members
 * in their places, its dependencies - as {@link ClassScanner} finds them, a row each, sortable by any column.
 */
public final class ClassObjectView implements BenchView {

    private final TableView<Row> table = new TableView<>();

    private final Label summary = new Label();

    private final BorderPane pane = new BorderPane();

    public ClassObjectView() {
        table.getColumns().addAll(List.of(column("Class", Row::type, 170), column("Place", Row::place, 170),
                column("Object", Row::object, 200), column("Name", Row::name, 440)));
        table.setPlaceholder(new Label("Nothing selected"));
        summary.setPadding(new Insets(6, 8, 6, 8));
        pane.setTop(summary);
        pane.setCenter(table);
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
        selection.ifPresentOrElse(selected -> {
            final List<Occurrence> found = selected.classes().stream()
                    .flatMap(type -> ClassScanner.INSTANCE.scan(classFile(type)).stream())
                    .toList();
            table.getItems().setAll(found.stream().map(this::row).toList());
            summary.setText(found.size() + " objects of "
                    + found.stream().map(occurrence -> occurrence.kind().getClass()).distinct().count() + " kinds in "
                    + selected.classes().size() + (selected.classes().size() == 1 ? " class" : " classes")
                    + ", read from the class files alone");
        }, () -> {
            table.getItems().clear();
            summary.setText("Select a unit, or a class nested in one, to see the objects in its class files.");
        });
    }

    /** The rows shown. */
    public List<Row> rows() {
        return List.copyOf(table.getItems());
    }

    private TableColumn<Row, String> column(final String title, final Function<Row, String> value,
                                            final double width) {
        final TableColumn<Row, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
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

    /** A row of the table: the class an object was found in, its place there, the object, and its name. */
    public record Row(String type, String place, String object, String name) implements Scalar {
    }
}
