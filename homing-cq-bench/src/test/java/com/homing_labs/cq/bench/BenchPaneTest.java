package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaUnit;
import com.homing_labs.cq.lang.java.taxonomy.MemberOf;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bench over the taxonomy module, laid out in a scene that is never shown.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenchPaneTest {

    private static final String TAXONOMY = "com.homing_labs.cq.lang.java.taxonomy";

    @BeforeAll
    void startJavaFx() {
        FxThread.INSTANCE.start();
    }

    @Test
    void theUnitTreeHoldsEachPackageWithItsUnitsAndEndsAtThem() {
        final TreeView<IndexEntry> units = FxThread.INSTANCE.call(() -> tree(new LaidOut(List.of()), "#units"));
        assertEquals(List.of(TAXONOMY, TAXONOMY + ".scan"), FxThread.INSTANCE.call(() -> labels(units.getRoot())));
        assertTrue(FxThread.INSTANCE.call(() -> units.getRoot().getChildren().stream()
                .flatMap(inPackage -> inPackage.getChildren().stream())
                .allMatch(TreeItem::isLeaf)));
    }

    @Test
    void theNestIndexHoldsEachClassUnderTheClassItIsDeclaredIn() {
        final TreeItem<IndexEntry> memberOf =
                FxThread.INSTANCE.call(() -> NestIndex.INSTANCE.of(new JavaUnit.ClassOnly(MemberOf.class)));
        assertEquals("MemberOf", memberOf.getValue().toString());
        assertEquals(List.of("AbstractClass", "Annotation", "Class", "Enum", "Interface", "Record"), labels(memberOf));
        assertEquals(List.of("ClassObject", "Instance"), labels(child(memberOf, "Class")));
    }

    @Test
    void theNestIndexNamesLocalAndAnonymousClasses() {
        final TreeItem<IndexEntry> fixture =
                FxThread.INSTANCE.call(() -> NestIndex.INSTANCE.of(new JavaUnit.ClassOnly(NestFixture.class)));
        assertEquals(List.of("anonymous 1", "Local (local)", "Inner", "Nested"), labels(fixture));
        assertEquals(List.of("Deeper"), labels(child(fixture, "Inner")));
    }

    @Test
    void thereIsATabForEachView() {
        final List<String> titles = FxThread.INSTANCE.call(() -> {
            final LaidOut bench = new LaidOut(List.of(new ClassObjectView(), new SourceView()));
            return ((TabPane) bench.root().lookup("#views")).getTabs().stream().map(Tab::getText).toList();
        });
        assertEquals(List.of("Class object", "Source"), titles);
    }

    @Test
    void selectingAUnitOpensItsNestAndShowsAllOfItInEveryView() {
        final ClassObjectView classObject = FxThread.INSTANCE.call(ClassObjectView::new);
        final SourceView source = FxThread.INSTANCE.call(SourceView::new);
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(classObject, source)));
        FxThread.INSTANCE.call(() -> select(bench, "ClassKind"));
        assertEquals("ClassKind", FxThread.INSTANCE.call(() -> nest(bench).getSelectionModel().getSelectedItem()
                .getValue().toString()));
        final List<ClassObjectView.Row> rows = FxThread.INSTANCE.call(classObject::rows);
        assertTrue(rows.contains(new ClassObjectView.Row("ClassKind", "-", "ClassKind.Interface",
                TAXONOMY + ".ClassKind")), rows.toString());
        assertTrue(rows.contains(new ClassObjectView.Row("ClassKind$Enum", "Record.ClassObject", "Field",
                "INSTANCE")), rows.toString());
        assertTrue(FxThread.INSTANCE.call(source::text).contains("Source of ClassKind: "));
    }

    @Test
    void selectingAClassInTheNestShowsThatClassAlone() {
        final ClassObjectView classObject = FxThread.INSTANCE.call(ClassObjectView::new);
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(classObject)));
        FxThread.INSTANCE.call(() -> {
            select(bench, "ClassKind");
            final TreeView<IndexEntry> nest = nest(bench);
            nest.getSelectionModel().select(child(nest.getRoot(), "Enum"));
            return nest;
        });
        final List<ClassObjectView.Row> rows = FxThread.INSTANCE.call(classObject::rows);
        assertTrue(!rows.isEmpty() && rows.stream().allMatch(row -> row.type().equals("ClassKind$Enum")),
                rows.toString());
    }

    @Test
    void everyTypeOfTheBenchSaysWhatItIsAndIsHeldToIt() {
        for (final Class<?> type : List.of(ClasspathUnits.class, Selection.class, IndexEntry.class,
                IndexEntry.Package.class, IndexEntry.OfClass.class, UnitTree.class, NestIndex.class,
                BenchView.class, ClassObjectView.class, ClassObjectView.Row.class, SourceView.class,
                BenchPane.class, BenchApp.class, Bench.class)) {
            assertEquals(List.of(), new OntologyEnforcer().enforce(type), type.getName());
        }
    }

    private TreeView<IndexEntry> select(final LaidOut bench, final String unit) {
        final TreeView<IndexEntry> units = tree(bench, "#units");
        units.getSelectionModel().select(units.getRoot().getChildren().stream()
                .flatMap(inPackage -> inPackage.getChildren().stream())
                .filter(item -> item.getValue().toString().equals(unit))
                .findFirst().orElseThrow());
        return units;
    }

    private TreeView<IndexEntry> nest(final LaidOut bench) {
        return tree(bench, "#nest");
    }

    @SuppressWarnings("unchecked")
    private TreeView<IndexEntry> tree(final LaidOut bench, final String id) {
        return (TreeView<IndexEntry>) bench.root().lookup(id);
    }

    private TreeItem<IndexEntry> child(final TreeItem<IndexEntry> item, final String label) {
        return item.getChildren().stream()
                .filter(child -> child.getValue().toString().equals(label))
                .findFirst().orElseThrow();
    }

    private List<String> labels(final TreeItem<IndexEntry> item) {
        return item.getChildren().stream().map(child -> child.getValue().toString()).toList();
    }

    /** The bench over the taxonomy module, with these views, laid out in a scene. */
    private record LaidOut(Parent root) {

        LaidOut(final List<BenchView> views) {
            this(BenchPane.INSTANCE.content(TAXONOMY, BenchPaneTest.class.getClassLoader(), views));
            new Scene(root, 1280, 800);
            root.applyCss();
            root.layout();
        }
    }
}
