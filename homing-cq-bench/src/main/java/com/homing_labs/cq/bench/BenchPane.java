package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import java.util.List;
import java.util.Optional;

/**
 * The bench, as an IDE lays it out: the root package to scan at the top; on the left the tree of its units, down to
 * the top-level classes; beside it the index of the selected unit's nest; a tab for each view on the right; and what
 * the scan found at the bottom. Selecting a unit opens its nest and measures all of it; selecting a class in the nest
 * measures that class alone. The tool bar also opens the taxonomy itself, in a window of its own.
 */
public record BenchPane() implements StatelessFunctionalObject {

    public static final BenchPane INSTANCE = new BenchPane();

    /** The bench over the units under {@code rootPackage}, as {@code loader} finds them, with these views. */
    public Parent content(final String rootPackage, final ClassLoader loader, final List<BenchView> views) {
        final TreeView<IndexEntry> units = tree("units");
        units.setShowRoot(false);
        final TreeView<IndexEntry> nest = tree("nest");
        final TabPane tabs = new TabPane();
        tabs.setId("views");
        views.forEach(view -> {
            final Tab tab = new Tab(view.title(), view.content());
            tab.setClosable(false);
            tabs.getTabs().add(tab);
        });
        final Label nestTitle = title("Nest");
        units.getSelectionModel().selectedItemProperty().addListener((observed, before, selected) -> {
            final Optional<Selection> unit = selection(selected);
            nest.setRoot(unit.map(Selection::unit).map(NestIndex.INSTANCE::of).orElse(null));
            nestTitle.setText(unit.map(chosen -> "Nest of " + chosen.type().getSimpleName()).orElse("Nest"));
            if (nest.getRoot() != null) {
                nest.getSelectionModel().select(nest.getRoot());
            }
        });
        nest.getSelectionModel().selectedItemProperty()
                .addListener((observed, before, selected) -> views.forEach(view -> view.show(selection(selected))));
        final TextField root = new TextField(rootPackage);
        root.setPrefColumnCount(36);
        final Label status = new Label();
        status.setPadding(new Insets(4, 8, 4, 8));
        final Runnable scan = () -> {
            final String scanned = root.getText().trim();
            try {
                final List<JavaUnit> found = ClasspathUnits.INSTANCE.read(scanned, loader);
                final TreeItem<IndexEntry> top = UnitTree.INSTANCE.of(scanned, found);
                units.setRoot(top);
                status.setText(count(found.size(), "unit") + " in " + count(top.getChildren().size(), "package")
                        + " under " + scanned);
            } catch (final RuntimeException e) {
                units.setRoot(null);
                status.setText("Cannot scan " + scanned + ": " + e.getMessage());
            }
            nest.setRoot(null);
            views.forEach(view -> view.show(Optional.empty()));
        };
        final Button button = new Button("Scan");
        button.setOnAction(event -> scan.run());
        root.setOnAction(event -> scan.run());
        scan.run();
        final Button taxonomy = new Button("Taxonomy");
        taxonomy.setId("taxonomy-window");
        taxonomy.setOnAction(event -> TaxonomyView.INSTANCE.show(taxonomy.getScene().getWindow()));
        final SplitPane split = new SplitPane(new BorderPane(units, title("Units"), null, null, null),
                new BorderPane(nest, nestTitle, null, null, null), tabs);
        split.setDividerPositions(0.2, 0.38);
        return new BorderPane(split, new ToolBar(new Label("Root package"), root, button, taxonomy), null, status,
                null);
    }

    private TreeView<IndexEntry> tree(final String id) {
        final TreeView<IndexEntry> tree = new TreeView<>();
        tree.setId(id);
        tree.setCellFactory(view -> new IndexCell());
        return tree;
    }

    private Label title(final String text) {
        final Label title = new Label(text);
        title.setPadding(new Insets(4, 8, 4, 8));
        return title;
    }

    private Optional<Selection> selection(final TreeItem<IndexEntry> item) {
        return Optional.ofNullable(item).map(TreeItem::getValue)
                .flatMap(entry -> entry instanceof IndexEntry.OfClass ofClass
                        ? Optional.of(ofClass.selection()) : Optional.empty());
    }

    private String count(final int count, final String thing) {
        return count + " " + thing + (count == 1 ? "" : "s");
    }
}
