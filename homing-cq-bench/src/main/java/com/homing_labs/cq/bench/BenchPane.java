package com.homing_labs.cq.bench;

import com.homing_labs.cq.scan.Exemptions;
import com.homing_labs.cq.scan.Ruleset;
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
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The bench, as an IDE lays it out: the root packages to scan and the rulesets to check them by at the top; on the
 * left the tree of their units, down to the top-level classes, each package tallying how many of its units comply,
 * over the index of the selected unit's nest; a tab for each view in the middle; the source on the right, always in
 * view; and what the scan found at the bottom. Selecting a unit opens its nest and measures all of it; selecting a
 * class in the nest measures that class alone. The source highlights the class selected, or what a view has selected
 * of it. The tool bar also opens the taxonomy itself and the {@link RuleBookView rule book}, each in a window of its
 * own; the rule book follows each scan. The {@link Scanner} scans; the bench shows what it found.
 */
public record BenchPane() implements StatelessFunctionalObject {

    public static final BenchPane INSTANCE = new BenchPane();

    /**
     * The bench over the units under {@code rootPackages} - one package, or several apart by commas or spaces - as
     * {@code loader} finds them in main code, with their sources from {@code sources} first, checked by the rulesets,
     * with these views, and scanned by the time it returns.
     */
    public Parent content(final String rootPackages, final List<Ruleset> rulesets, final List<Path> sources,
                          final ClassLoader loader, final List<BenchView> views, final SourceView source) {
        return content(rootPackages, rulesets, sources, loader, views, source, List.of(), ScanThread.AtOnce.INSTANCE,
                new BenchMeter());
    }

    /**
     * The bench as above, with {@code exemptions}, its scans run by {@code thread}, each one measured on {@code meter}.
     */
    public Parent content(final String rootPackages, final List<Ruleset> rulesets, final List<Path> sources,
                          final ClassLoader loader, final List<BenchView> views, final SourceView source,
                          final List<Exemptions<?>> exemptions, final ScanThread thread, final BenchMeter meter) {
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
        final Label status = title("");
        status.setId("status");
        final RuleBookView book = new RuleBookView(rulesets, sources, loader);
        final Button button = new Button("Scan");
        button.setId("scan");
        final BenchScan scan = new BenchScan(rulesets, sources, exemptions, loader, thread, meter, units, status,
                button, result -> {
                    views.forEach(view -> view.scanned(result));
                    book.scanned(result);
                }, () -> {
                    nest.setRoot(null);
                    views.forEach(view -> view.show(Optional.empty()));
                    source.show(Optional.empty());
                });
        final Label rules = new Label(scan.names());
        rules.setId("rulesets");
        button.setOnAction(event -> scan.run(roots.getText()));
        roots.setOnAction(event -> scan.run(roots.getText()));
        scan.run(roots.getText());
        final SplitPane indexes = new SplitPane(new BorderPane(units, title("Units"), null, null, null),
                new BorderPane(nest, nestTitle, null, null, null));
        indexes.setOrientation(Orientation.VERTICAL);
        indexes.setDividerPositions(0.5);
        final SplitPane split = new SplitPane(indexes, tabs(views, source), source.content());
        split.setDividerPositions(0.18, 0.56);
        return new BorderPane(split, new ToolBar(new Label("Root packages"), roots, new Label("Rulesets"), rules,
                button, opener("Taxonomy", "taxonomy-window", TaxonomyView.INSTANCE::show),
                opener("Rule book", "rule-book-window", book::show)), null, status, null);
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

    /** A button of the tool bar that opens a window of its own over the bench's. */
    private Button opener(final String text, final String id, final Consumer<Window> open) {
        final Button button = new Button(text);
        button.setId(id);
        button.setOnAction(event -> open.accept(button.getScene().getWindow()));
        return button;
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
}

