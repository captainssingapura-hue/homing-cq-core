package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.taxonomy.Cardinality;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Placement;
import com.homing_labs.cq.lang.java.taxonomy.Taxonomy;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The taxonomy itself, in a small window of its own: its kinds as the tree they are sealed into, from
 * {@link ObjectKind} down to the objects a class is made of, and beside each object how many of it each kind of class
 * can have - {@code 1}, {@code 0..1}, {@code 1..*} or {@code 0..*} - or nothing, where that kind cannot have it.
 */
public record TaxonomyView() implements StatelessFunctionalObject {

    public static final TaxonomyView INSTANCE = new TaxonomyView();

    private static final String TITLE = "Taxonomy";

    /** Shows the taxonomy's window over the bench's, opening it if it is not open yet. */
    public void show(final Window owner) {
        final Stage window = Window.getWindows().stream()
                .flatMap(open -> open instanceof Stage stage && stage.getOwner() == owner
                        && TITLE.equals(stage.getTitle()) ? Stream.of(stage) : Stream.empty())
                .findFirst()
                .orElseGet(() -> {
                    final Stage stage = new Stage();
                    stage.initOwner(owner);
                    stage.setTitle(TITLE);
                    stage.setScene(new Scene(content(), 750, 640));
                    return stage;
                });
        window.show();
        window.toFront();
    }

    /** The taxonomy as a tree, every kind open, with a column for each kind of class. */
    public TreeTableView<Class<? extends ObjectKind>> content() {
        final TreeTableView<Class<? extends ObjectKind>> table = new TreeTableView<>(item(ObjectKind.class));
        table.setId("taxonomy");
        table.getColumns().add(column("Object", Class::getSimpleName, 190));
        for (final ClassKind kind : Taxonomy.INSTANCE.kinds()) {
            final Map<Class<?>, Cardinality> held = Taxonomy.INSTANCE.placements(kind).stream()
                    .collect(Collectors.toMap(placement -> placement.object().getClass(), Placement::cardinality));
            final TreeTableColumn<Class<? extends ObjectKind>, String> column = column(kind.getClass().getSimpleName(),
                    object -> Optional.ofNullable(held.get(object)).map(this::multiplicity).orElse(""), 90);
            column.setStyle("-fx-alignment: CENTER;");
            table.getColumns().add(column);
        }
        return table;
    }

    private TreeItem<Class<? extends ObjectKind>> item(final Class<? extends ObjectKind> kind) {
        final TreeItem<Class<? extends ObjectKind>> item = new TreeItem<>(kind);
        Taxonomy.INSTANCE.permitted(kind).forEach(permitted -> item.getChildren().add(item(permitted)));
        item.setExpanded(true);
        return item;
    }

    private TreeTableColumn<Class<? extends ObjectKind>, String> column(
            final String title, final Function<Class<? extends ObjectKind>, String> value, final double width) {
        final TreeTableColumn<Class<? extends ObjectKind>, String> column = new TreeTableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue().getValue())));
        column.setPrefWidth(width);
        column.setSortable(false);
        return column;
    }

    /** A cardinality as a multiplicity is written: {@code 0..*}. */
    private String multiplicity(final Cardinality cardinality) {
        return switch (cardinality) {
            case Cardinality.One _ -> "1";
            case Cardinality.ZeroOrOne _ -> "0..1";
            case Cardinality.OneOrMore _ -> "1..*";
            case Cardinality.ZeroOrMore _ -> "0..*";
        };
    }
}
