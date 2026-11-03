package com.homing_labs.cq.bench;

import com.homing_labs.cq.scan.Ruleset;
import com.homing_labs.cq.scan.ScanResult;
import com.homing_labs.cq.scan.ScannedUnit;
import com.homing_labs.cq.scan.ScannedUnits;
import com.homing_labs.cq.scan.Scanner;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The bench, as an IDE lays it out: the root packages to scan and the rulesets to check them by at the top; on the
 * left the tree of their units, down to the top-level classes, each package tallying how many of its units comply,
 * over the index of the selected unit's nest; a tab for each view in the middle; the source on the right, always in
 * view; and what the scan found at the bottom. Selecting a unit opens its nest and measures all of it; selecting a
 * class in the nest measures that class alone. The source highlights the class selected, or what a view has selected
 * of it. The tool bar also opens the taxonomy itself, in a window of its own. The {@link Scanner} scans; the bench
 * shows what it found.
 */
public record BenchPane() implements StatelessFunctionalObject {

    public static final BenchPane INSTANCE = new BenchPane();

    /**
     * The bench over the units under {@code rootPackages} - one package, or several apart by commas or spaces - as
     * {@code loader} finds them in main code, with their sources from {@code sources} first, checked by the rulesets,
     * with these views.
     */
    public Parent content(final String rootPackages, final List<Ruleset> rulesets, final List<Path> sources,
                          final ClassLoader loader, final List<BenchView> views, final SourceView source) {
        final TreeView<IndexEntry> units = tree("units");
        units.setShowRoot(false);
        final TreeView<IndexEntry> nest = tree("nest");
        final Label nestTitle = title("Nest");
        units.getSelectionModel().selectedItemProperty().addListener((observed, before, selected) -> {
            final Optional<Selection> unit = selection(selected);
            nest.setRoot(unit.map(Selection::unit).map(NestIndex.INSTANCE::of).orElse(null));
            nestTitle.setText(unit.map(chosen -> "Nest of " + chosen.type().getSimpleName()).orElse("Nest"));
            if (nest.getRoot() != null) {
                nest.getSelectionModel().select(nest.getRoot());
            }
        });
        nest.getSelectionModel().selectedItemProperty().addListener((observed, before, selected) -> {
            views.forEach(view -> view.show(selection(selected)));
            source.show(selection(selected));
        });
        final TextField roots = field("roots", rootPackages, 30);
        final Label rules = new Label(names(rulesets));
        rules.setId("rulesets");
        final Label status = title("");
        status.setId("status");
        final Runnable scan = () -> {
            scan(roots.getText(), rulesets, sources, loader, units, views, status);
            nest.setRoot(null);
            views.forEach(view -> view.show(Optional.empty()));
            source.show(Optional.empty());
        };
        final Button button = new Button("Scan");
        button.setOnAction(event -> scan.run());
        roots.setOnAction(event -> scan.run());
        scan.run();
        final Button taxonomy = new Button("Taxonomy");
        taxonomy.setId("taxonomy-window");
        taxonomy.setOnAction(event -> TaxonomyView.INSTANCE.show(taxonomy.getScene().getWindow()));
        final SplitPane indexes = new SplitPane(new BorderPane(units, title("Units"), null, null, null),
                new BorderPane(nest, nestTitle, null, null, null));
        indexes.setOrientation(Orientation.VERTICAL);
        indexes.setDividerPositions(0.5);
        final SplitPane split = new SplitPane(indexes, tabs(views, source), source.content());
        split.setDividerPositions(0.18, 0.56);
        return new BorderPane(split, new ToolBar(new Label("Root packages"), roots, new Label("Rulesets"), rules,
                button, taxonomy), null, status, null);
    }

    /**
     * Scans the root packages by the rulesets, and shows what it found in the tree; or, if it cannot, says why and
     * shows none.
     */
    private void scan(final String rootPackages, final List<Ruleset> rulesets, final List<Path> sources,
                      final ClassLoader loader, final TreeView<IndexEntry> units, final List<BenchView> views,
                      final Label status) {
        final List<String> roots = Arrays.stream(rootPackages.split("[,\\s]+")).filter(root -> !root.isEmpty())
                .distinct().toList();
        try {
            final ScanResult result = Scanner.INSTANCE.scan(roots, rulesets, sources, loader);
            views.forEach(view -> view.scanned(result));
            final List<ScannedUnit> found = result.units().items();
            units.setRoot(UnitTree.INSTANCE.of(found));
            status.setText(count(found.size(), "unit") + " in "
                    + count((int) found.stream().map(scanned -> scanned.unit().type().getPackageName()).distinct()
                    .count(), "package") + " under " + String.join(", ", roots) + "; "
                    + result.compliance().compliant() + " comply with the " + count(result.rules(), "rule") + " of "
                    + names(rulesets));
        } catch (final RuntimeException e) {
            views.forEach(view -> view.scanned(new ScanResult(new ScannedUnits(List.of()), 0)));
            units.setRoot(null);
            status.setText("Cannot scan " + String.join(", ", roots) + " by " + names(rulesets) + ": "
                    + e.getMessage());
        }
    }

    /** The rulesets by their names: {@code no ruleset}, when there is none. */
    private String names(final List<Ruleset> rulesets) {
        return rulesets.isEmpty() ? "no ruleset" : rulesets.stream().map(Ruleset::name)
                .collect(Collectors.joining(", "));
    }

    /** A tab for each view, each telling the source what it has selected. */
    private TabPane tabs(final List<BenchView> views, final SourceView source) {
        final TabPane tabs = new TabPane();
        tabs.setId("views");
        views.forEach(view -> {
            final Tab tab = new Tab(view.title(), view.content());
            tab.setClosable(false);
            tabs.getTabs().add(tab);
            view.onFocus(source::focus);
        });
        return tabs;
    }

    private TreeView<IndexEntry> tree(final String id) {
        final TreeView<IndexEntry> tree = new TreeView<>();
        tree.setId(id);
        tree.setCellFactory(view -> new IndexCell());
        return tree;
    }

    private TextField field(final String id, final String text, final int columns) {
        final TextField field = new TextField(text);
        field.setId(id);
        field.setPrefColumnCount(columns);
        return field;
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
