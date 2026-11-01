package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.MethodValue;
import com.homing_labs.cq.lang.java.model.Values;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import com.homing_labs.cq.lang.java.unit.JavaUnits;
import com.homing_labs.cq.lang.java.taxonomy.Cardinality;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.Constructor;
import com.homing_labs.cq.lang.java.taxonomy.Dependency;
import com.homing_labs.cq.lang.java.taxonomy.Field;
import com.homing_labs.cq.lang.java.taxonomy.LocalClass;
import com.homing_labs.cq.lang.java.taxonomy.MemberOf;
import com.homing_labs.cq.lang.java.taxonomy.Method;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.StaticField;
import com.homing_labs.cq.lang.java.taxonomy.StaticMemberClass;
import com.homing_labs.cq.lang.java.taxonomy.Superinterface;
import com.homing_labs.cq.lang.java.taxonomy.Taxonomy;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bench over the taxonomy module, laid out in a scene that is never shown.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenchPaneTest {

    private static final String TAXONOMY = "com.homing_labs.cq.lang.java.taxonomy";

    private static final String ENUM_RECORD = TAXONOMY + ".ClassKind$Enum";

    private static final String RULES = "com.homing_labs.cq.bench.rules";

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
        final TreeItem<IndexEntry> memberOf = FxThread.INSTANCE.call(
                () -> NestIndex.INSTANCE.of(JavaUnits.INSTANCE.of(MemberOf.class, Optional.empty())));
        assertEquals("MemberOf", memberOf.getValue().toString());
        assertEquals(List.of("AbstractClass", "Annotation", "Class", "Enum", "Interface", "Record"), labels(memberOf));
        assertTrue(child(memberOf, "Class").isLeaf());
    }

    @Test
    void theNestIndexBadgesEachClassByItsKindAndHowItExists() {
        final TreeItem<IndexEntry> fixture = FxThread.INSTANCE.call(
                () -> NestIndex.INSTANCE.of(JavaUnits.INSTANCE.of(NestFixture.class, Optional.empty())));
        assertTrue(Arrays.stream(NestFixture.class.getNestMembers()).anyMatch(Class::isSynthetic),
                "javac wrote a class of its own into the fixture's nest, for its switch map");
        assertEquals(List.of("1", "Local", "Inner", "Nested"), labels(fixture), "and the index leaves it out");
        assertEquals(List.of(List.of("C", "anon"), List.of("C", "local"), List.of("C", "inner"),
                        List.of("C", "static")),
                fixture.getChildren().stream().map(this::badges).toList());
        assertEquals(List.of("C"), badges(fixture));
        assertEquals(List.of("Made", "Deeper"), labels(child(fixture, "Inner")));
        assertEquals(List.of("C", "local"), badges(child(child(fixture, "Inner"), "Made")));
        assertEquals(List.of("C", "inner"), badges(child(child(fixture, "Inner"), "Deeper")));
        final TreeItem<IndexEntry> classKind = FxThread.INSTANCE.call(
                () -> NestIndex.INSTANCE.of(JavaUnits.INSTANCE.of(ClassKind.class, Optional.empty())));
        assertEquals(List.of("I"), badges(classKind));
        assertEquals(List.of("R", "static"), badges(child(classKind, "Enum")));
    }

    @Test
    void theUnitTreeShowsEachUnitWithTheBadgeOfItsKind() {
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of()));
        final TreeItem<IndexEntry> taxonomy = FxThread.INSTANCE.call(() -> child(tree(bench, "#units").getRoot(),
                TAXONOMY));
        assertEquals(List.of("I"), badges(child(taxonomy, "ClassKind")));
        assertEquals(List.of("R"), badges(child(taxonomy, "Placement")));
        assertEquals(List.of("I"), FxThread.INSTANCE.call(() -> bench.root().lookupAll(".tree-cell").stream()
                .map(cell -> (IndexCell) cell)
                .filter(cell -> "ClassKind".equals(cell.getText()))
                .flatMap(cell -> ((HBox) cell.getGraphic()).getChildren().stream())
                .map(badge -> ((Label) badge).getText())
                .toList()), "the cell shows the badge");
    }

    @Test
    void thereIsATabForEachViewAndTheSourceBesideThem() {
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(new ClassObjectView(),
                new RulesView(BenchPaneTest.class.getClassLoader(), RULES))));
        assertEquals(List.of("Class object", "Rules applied"), FxThread.INSTANCE.call(
                () -> ((TabPane) bench.root().lookup("#views")).getTabs().stream().map(Tab::getText).toList()));
        assertTrue(FxThread.INSTANCE.call(() -> bench.root().lookup("#source") != null
                && ((TabPane) bench.root().lookup("#views")).getTabs().stream()
                .noneMatch(tab -> tab.getContent().lookup("#source") != null)), "the source is in no tab");
    }

    @Test
    void selectingAUnitOpensItsNestAndShowsAllOfItInEveryView() {
        final ClassObjectView classObject = FxThread.INSTANCE.call(ClassObjectView::new);
        final SourceView source = FxThread.INSTANCE.call(SourceView::new);
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(classObject), source));
        FxThread.INSTANCE.call(() -> select(bench, "ClassKind"));
        assertEquals("ClassKind", FxThread.INSTANCE.call(() -> nest(bench).getSelectionModel().getSelectedItem()
                .getValue().toString()));
        final List<ClassObjectView.Row> rows = FxThread.INSTANCE.call(classObject::rows);
        assertTrue(rows.contains(new ClassObjectView.Row("ClassKind", "ClassKind.Interface",
                TAXONOMY + ".ClassKind")), rows.toString());
        assertTrue(rows.stream().anyMatch(row -> row.type().equals("ClassKind$Enum")
                && row.object().equals("StaticField") && row.name().startsWith("INSTANCE: ClassKind$Enum - public")),
                rows.toString());
        assertTrue(FxThread.INSTANCE.call(source::caption).startsWith("Source of ClassKind, "));
        assertTrue(FxThread.INSTANCE.call(source::text).contains("public sealed interface ClassKind extends"));
        final List<String> highlighted = FxThread.INSTANCE.call(source::written);
        assertEquals(1, highlighted.size(), "the unit selected is highlighted");
        assertTrue(highlighted.getFirst().startsWith("/**")
                        && highlighted.getFirst().contains("public sealed interface ClassKind extends"),
                highlighted.getFirst());
    }

    @Test
    void theSourceShowsTheFileThatDeclaresTheUnitOrSaysThereIsNone() {
        final SourceView source = FxThread.INSTANCE.call(SourceView::new);
        final URI file = URI.create("file:///sources/NestFixture.java");
        final String text = "class NestFixture {\n\n    class Inner {\n    }\n}\n";
        final JavaUnit unit = JavaUnits.INSTANCE.of(NestFixture.class, Optional.of(new JavaSource(file, text)));
        final String caption = "Source of NestFixture, 5 lines: " + Path.of(file);
        assertEquals(List.of(caption, text), shown(source, Optional.of(new Selection(unit, NestFixture.class))));
        assertEquals(List.of(caption, text), shown(source, Optional.of(new Selection(unit, NestFixture.Inner.class))),
                "a nested class is in its unit's file");
        assertEquals(List.of("No source was found for NestFixture.", ""), shown(source, Optional.of(
                new Selection(JavaUnits.INSTANCE.of(NestFixture.class, Optional.empty()), NestFixture.class))));
        assertEquals(List.of("Select a unit, or a class nested in one, to see the source file that declares it.", ""),
                shown(source, Optional.empty()));
    }

    /** What the source shows for a selection: its caption, and the file. */
    private List<String> shown(final SourceView source, final Optional<Selection> selection) {
        return FxThread.INSTANCE.call(() -> {
            source.show(selection);
            return List.of(source.caption(), source.text());
        });
    }

    @Test
    void theSourceHighlightsTheClassSelectedAndWhatTheClassObjectHasSelectedOfIt() {
        final ClassObjectView classObject = FxThread.INSTANCE.call(ClassObjectView::new);
        final SourceView source = FxThread.INSTANCE.call(SourceView::new);
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(classObject), source));
        final List<String> members = FxThread.INSTANCE.call(() -> {
            select(bench, "ClassKind");
            summary(bench).getSelectionModel().select(summary(bench).getItems().stream()
                    .filter(line -> line.object() == StaticMemberClass.class)
                    .findFirst().orElseThrow());
            return source.written();
        });
        assertEquals(List.of("Class", "AbstractClass", "Interface", "Enum", "Record", "Annotation"), members.stream()
                .map(member -> member.substring(member.indexOf("record ") + 7, member.indexOf("()")))
                .toList(), "each where it is written, from its documentation comment");
        assertTrue(members.stream().allMatch(member -> member.startsWith("/**") && member.endsWith("}")));
        assertEquals("6 values of StaticMemberClass of ClassKind: 6 written where highlighted.",
                FxThread.INSTANCE.call(source::placed));
        final List<String> one = FxThread.INSTANCE.call(() -> {
            final List<ClassObjectView.Row> rows = classObject.rows();
            details(bench).getSelectionModel().select(rows.indexOf(rows.stream()
                    .filter(row -> row.name().endsWith("ClassKind$Enum"))
                    .findFirst().orElseThrow()));
            return source.written();
        });
        assertEquals(1, one.size(), "a value selected below the summary alone");
        assertTrue(one.getFirst().contains("record Enum() implements ClassKind"), one.getFirst());
        assertEquals(List.of("ClassKind$Enum is declared where highlighted."), FxThread.INSTANCE.call(() -> {
            selectInNest(bench, "ClassKind", "Enum");
            return List.of(source.placed());
        }));
        assertEquals(List.of(one, List.of()), FxThread.INSTANCE.call(() -> List.of(source.written(), source.within())));
        final List<List<String>> field = FxThread.INSTANCE.call(() -> {
            summary(bench).getSelectionModel().select(line(StaticField.class, Cardinality.ZeroOrMore.INSTANCE, 1));
            return List.of(source.written(), source.within(), List.of(source.placed()));
        });
        assertEquals(List.of(List.of("public static final Enum INSTANCE = new Enum();"), List.of(),
                List.of("StaticField of ClassKind$Enum: written where highlighted.")), field,
                "a member type's body is read too");
    }

    @Test
    void theClassObjectShowsHowEachClassIsInitialisedApartFromItsPartsAndTheSourceHighlightsAStep() {
        final ClassObjectView classObject = FxThread.INSTANCE.call(ClassObjectView::new);
        final SourceView source = FxThread.INSTANCE.call(SourceView::new);
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(classObject), source));
        final List<ClassObjectView.Step> steps = FxThread.INSTANCE.call(() -> {
            select(bench, "ClassKind");
            return classObject.steps();
        });
        assertEquals(new ClassObjectView.Step("ClassKind", "-",
                "No code runs: its static fields are constants, or keep their defaults"), steps.getFirst());
        assertEquals(Set.of("INSTANCE = new Class()", "INSTANCE = new AbstractClass()", "INSTANCE = new Interface()",
                        "INSTANCE = new Enum()", "INSTANCE = new Record()", "INSTANCE = new Annotation()"),
                steps.stream().skip(1).map(ClassObjectView.Step::code).collect(Collectors.toSet()),
                "each member class's own");
        assertTrue(steps.stream().skip(1)
                .allMatch(step -> step.kind().equals("InitialValue")), steps.toString());
        assertTrue(FxThread.INSTANCE.call(() -> classObject.lines().stream()
                .noneMatch(line -> line.object().getSimpleName().contains("Initialis"))), "no part of a class");
        final List<List<String>> highlighted = FxThread.INSTANCE.call(() -> {
            initialisation(bench).getSelectionModel().select(steps.indexOf(steps.stream()
                    .filter(step -> step.type().equals("ClassKind$Enum")).findFirst().orElseThrow()));
            return List.of(source.written(), List.of(source.placed()));
        });
        assertEquals(List.of(List.of("INSTANCE = new Enum()"),
                List.of("How ClassKind$Enum is initialised: a step, written where highlighted.")), highlighted);
        assertEquals("ClassKind runs no code to be initialised: its static fields are constants, or keep their"
                + " defaults.", FxThread.INSTANCE.call(() -> {
                    initialisation(bench).getSelectionModel().select(0);
                    return source.placed();
                }));
        assertEquals("6 values of StaticMemberClass of ClassKind: 6 written where highlighted.",
                FxThread.INSTANCE.call(() -> {
                    summary(bench).getSelectionModel().select(summary(bench).getItems().stream()
                            .filter(line -> line.object() == StaticMemberClass.class)
                            .findFirst().orElseThrow());
                    return source.placed();
                }), "selecting a part again leaves the initialisation");
        assertEquals(-1, FxThread.INSTANCE.call(() -> initialisation(bench).getSelectionModel().getSelectedIndex()));
    }

    @Test
    void theSourceSaysWhereWhatIsSelectedIsWrittenOrWhyItCannot() throws IOException {
        final Path file = Path.of("src/test/java/com/homing_labs/cq/bench/NestFixture.java");
        final JavaUnit unit = JavaUnits.INSTANCE.of(NestFixture.class,
                Optional.of(new JavaSource(file.toUri(), Files.readString(file))));
        final SourceView source = FxThread.INSTANCE.call(SourceView::new);
        final ClassName fixture = new ClassName(NestFixture.class.getName());
        final List<String> whole = highlighted(source, new Selection(unit, NestFixture.class), Optional.empty());
        assertEquals("NestFixture is declared where highlighted.", whole.getFirst());
        assertTrue(whole.get(1).startsWith("/** * A class with each kind of class nested in it")
                && whole.get(1).endsWith("static class Nested { } }"), whole.get(1));
        final String inner = "class Inner { Object make() { class Made { } return new Made(); } class Deeper { } }";
        assertEquals(List.of("NestFixture$Inner is declared where highlighted.", inner),
                highlighted(source, new Selection(unit, NestFixture.Inner.class), Optional.empty()));
        assertEquals(List.of("NestFixture$Inner$Deeper is declared where highlighted.", "class Deeper { }"),
                highlighted(source, new Selection(unit, NestFixture.Inner.Deeper.class), Optional.empty()),
                "a member type's body is read, one level down");
        assertEquals(List.of("NestFixture$Inner$1Made is declared somewhere in the lightly highlighted code, which is"
                        + " not read yet.", "within Object make() { class Made { } return new Made(); }"),
                highlighted(source, new Selection(unit, nested("$Inner$1Made")), Optional.empty()),
                "a local class in a member type's method");
        assertEquals(List.of("NestFixture$1Local is declared somewhere in the lightly highlighted code, which is not"
                        + " read yet.", "within void declaresALocalClass() { class Local { } }"),
                highlighted(source, new Selection(unit, nested("$1Local")), Optional.empty()));
        assertEquals(List.of("NestFixture$1 is declared somewhere in the lightly highlighted code, which is not read"
                        + " yet.", "within anonymous = new Runnable() { @Override public void run() { } }"),
                highlighted(source, new Selection(unit, nested("$1")), Optional.empty()),
                "an anonymous class made in a field's initial value");
        final Selection selected = new Selection(unit, NestFixture.class);
        assertEquals(List.of("Method.Implementation of NestFixture: written where highlighted.",
                        "/** A classic switch on another class's enum: javac holds its switch map in a synthetic class"
                                + " of its own. */ int weekend(final DayOfWeek day) { switch (day) { case SATURDAY:"
                                + " case SUNDAY: return 1; default: return 0; } }"),
                highlighted(source, selected, Optional.of(values(unit, fixture, Method.Implementation.INSTANCE,
                        value -> value instanceof MethodValue method && method.name().equals("weekend")))));
        assertEquals(List.of("Constructor of NestFixture: written nowhere: the compiler implied or generated it."),
                highlighted(source, selected, Optional.of(values(unit, fixture, Constructor.INSTANCE, any -> true))));
        final List<String> code = highlighted(source, selected, Optional.of(values(unit, fixture,
                Dependency.Code.INSTANCE, any -> true)));
        assertTrue(code.size() == 1 && code.getFirst().endsWith(" not looked for: dependencies are not placed yet."),
                code.toString());
        assertEquals(whole, highlighted(source, selected, Optional.empty()), "with nothing focused, the class");
    }

    /** What the source says of what it highlights, then the stretches it highlights, each on a line. */
    private List<String> highlighted(final SourceView source, final Selection selection, final Optional<Focus> focus) {
        return FxThread.INSTANCE.call(() -> {
            source.show(Optional.of(selection));
            source.focus(focus);
            return Stream.of(Stream.of(source.placed()), source.written().stream().map(this::squashed),
                            source.within().stream().map(within -> "within " + squashed(within)))
                    .flatMap(each -> each)
                    .toList();
        });
    }

    /** The values of an object of a class of the unit that pass the test, focused on. */
    private Focus values(final JavaUnit unit, final ClassName inClass, final ObjectKind object,
                         final Predicate<Scalar> wanted) {
        return new Focus.OfValues(inClass, object, new Values(unit.attributes().entry(object).all().stream()
                .filter(value -> wanted.test(value.object()))
                .toList()));
    }

    private Class<?> nested(final String suffix) {
        return Arrays.stream(NestFixture.class.getNestMembers())
                .filter(type -> type.getName().equals(NestFixture.class.getName() + suffix))
                .findFirst().orElseThrow();
    }

    private String squashed(final String text) {
        return text.replaceAll("\\s+", " ");
    }

    @Test
    void aNestedClassAloneHasTheLocalClassesItsCodeDeclares() {
        final ClassObjectView classObject = FxThread.INSTANCE.call(ClassObjectView::new);
        final JavaUnit unit = JavaUnits.INSTANCE.of(NestFixture.class, Optional.empty());
        final String inner = NestFixture.Inner.class.getName();
        FxThread.INSTANCE.call(() -> {
            classObject.show(Optional.of(new Selection(unit, NestFixture.Inner.class)));
            return classObject;
        });
        assertTrue(FxThread.INSTANCE.call(classObject::lines).contains(new ClassObjectView.Line(inner,
                LocalClass.class, Optional.of(Cardinality.ZeroOrMore.INSTANCE), 1)), "its local class is counted");
        assertTrue(FxThread.INSTANCE.call(classObject::rows).contains(new ClassObjectView.Row("NestFixture$Inner",
                "LocalClass", "NestFixture$Inner$1Made in make()")));
        assertTrue(FxThread.INSTANCE.call(classObject::lines).stream().allMatch(line -> line.inClass().equals(inner)),
                "only the class selected");
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
    void theSummaryHasALineForTheClassAndForEachObjectItCanHaveCounted() {
        final ClassObjectView classObject = FxThread.INSTANCE.call(ClassObjectView::new);
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(classObject)));
        FxThread.INSTANCE.call(() -> selectInNest(bench, "ClassKind", "Enum"));
        final List<ClassObjectView.Line> lines = FxThread.INSTANCE.call(classObject::lines);
        assertEquals(1 + Taxonomy.INSTANCE.placements(ClassKind.Record.INSTANCE).size(), lines.size());
        assertEquals(new ClassObjectView.Line(ENUM_RECORD, ClassKind.Record.class, Optional.empty(), 1),
                lines.getFirst());
        assertTrue(lines.containsAll(List.of(
                line(Constructor.class, Cardinality.OneOrMore.INSTANCE, 1),
                line(StaticField.class, Cardinality.ZeroOrMore.INSTANCE, 1),
                line(Method.Implementation.class, Cardinality.OneOrMore.INSTANCE, 3),
                line(Field.class, Cardinality.ZeroOrMore.INSTANCE, 0))),
                lines.toString());
    }

    @Test
    void selectingASummaryLineShowsItsInstancesBelow() {
        final ClassObjectView classObject = FxThread.INSTANCE.call(ClassObjectView::new);
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(classObject)));
        final List<ClassObjectView.Row> all = FxThread.INSTANCE.call(() -> {
            selectInNest(bench, "ClassKind", "Enum");
            return classObject.rows();
        });
        final List<ClassObjectView.Row> implementations = FxThread.INSTANCE.call(() -> {
            summary(bench).getSelectionModel().select(line(Method.Implementation.class, Cardinality.OneOrMore.INSTANCE,
                    3));
            return classObject.rows();
        });
        assertEquals(List.of("equals", "hashCode", "toString"), implementations.stream()
                .map(row -> row.name().substring(0, row.name().indexOf('('))).sorted().toList());
        assertTrue(implementations.stream().allMatch(row -> row.object().equals("Method.Implementation")),
                implementations.toString());
        final List<ClassObjectView.Row> again = FxThread.INSTANCE.call(() -> {
            summary(bench).getSelectionModel().clearSelection();
            return classObject.rows();
        });
        assertEquals(all, again);
        assertTrue(all.size() > implementations.size());
    }

    @Test
    void theRulesAppliedAreThoseForEveryUnitAndForTheUnitsKind() {
        final RulesView rules =
                FxThread.INSTANCE.call(() -> new RulesView(BenchPaneTest.class.getClassLoader(), RULES));
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(rules)));
        assertEquals(List.of("EveryUnitIsSeen", "InterfacesAreSeen"), FxThread.INSTANCE.call(() -> {
            select(bench, "ClassKind");
            return ruleNames(rules.applied());
        }));
        assertEquals(List.of("EveryUnitIsSeen"), FxThread.INSTANCE.call(() -> {
            select(bench, "Placement");
            return ruleNames(rules.applied());
        }));
    }

    @Test
    void selectingARuleShowsItsFindings() {
        final RulesView rules =
                FxThread.INSTANCE.call(() -> new RulesView(BenchPaneTest.class.getClassLoader(), RULES));
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of(rules)));
        final List<String> findings = FxThread.INSTANCE.call(() -> {
            select(bench, "ClassKind");
            @SuppressWarnings("unchecked") final TableView<RulesView.Applied> table =
                    (TableView<RulesView.Applied>) bench.root().lookup("#rules");
            table.getSelectionModel().select(table.getItems().stream()
                    .filter(applied -> applied.signature().rule().getSimpleName().equals("InterfacesAreSeen"))
                    .findFirst().orElseThrow());
            return rules.findings();
        });
        assertEquals(List.of("Seen[unit=ClassKind]"), findings);
        assertEquals(List.of("1", "1"), FxThread.INSTANCE.call(
                () -> rules.applied().stream().map(RulesView.Applied::outcome).toList()));
    }

    @Test
    void theTaxonomyWindowShowsTheKindsAsTheTreeTheyAreSealedInto() {
        final TreeTableView<Class<? extends ObjectKind>> taxonomy =
                FxThread.INSTANCE.call(TaxonomyView.INSTANCE::content);
        final TreeItem<Class<? extends ObjectKind>> root = taxonomy.getRoot();
        assertEquals(ObjectKind.class, root.getValue());
        assertEquals(Taxonomy.INSTANCE.permitted(ObjectKind.class),
                root.getChildren().stream().map(TreeItem::getValue).toList());
        assertEquals(Taxonomy.INSTANCE.objects(), leaves(root), "every object once, in the taxonomy's order");
        assertEquals(List.of("Object", "Class", "AbstractClass", "Interface", "Enum", "Record", "Annotation"),
                taxonomy.getColumns().stream().map(TreeTableColumn::getText).toList());
        assertEquals(List.of("Field", "0..*", "0..*", "", "0..*", "0..*", ""), row(taxonomy, Field.class));
        assertEquals(List.of("Superinterface", "0..*", "0..*", "0..*", "0..*", "0..*", "1"),
                row(taxonomy, Superinterface.class));
        assertEquals(List.of("Implementation", "0..*", "0..*", "0..*", "0..*", "1..*", ""),
                row(taxonomy, Method.Implementation.class));
        assertEquals(List.of("ClassKind", "", "", "", "", "", ""), row(taxonomy, ClassKind.class),
                "a kind divided into others has no cardinality of its own");
    }

    @Test
    void theToolBarOpensOneTaxonomyWindowOverTheBench() {
        final LaidOut bench = FxThread.INSTANCE.call(() -> new LaidOut(List.of()));
        assertEquals("Taxonomy", FxThread.INSTANCE.call(() -> ((Button) bench.root().lookup("#taxonomy-window"))
                .getText()));
        final List<String> opened = FxThread.INSTANCE.call(() -> {
            final Stage owner = new Stage();
            owner.setScene(new Scene(new Label("bench"), 200, 100));
            owner.show();
            TaxonomyView.INSTANCE.show(owner);
            TaxonomyView.INSTANCE.show(owner);
            final List<Stage> windows = Window.getWindows().stream()
                    .flatMap(window -> window instanceof Stage stage && stage.getOwner() == owner
                            ? Stream.of(stage) : Stream.empty())
                    .toList();
            final List<String> titles = windows.stream().map(Stage::getTitle).toList();
            windows.forEach(Stage::close);
            owner.close();
            return titles;
        });
        assertEquals(List.of("Taxonomy"), opened, "showing it again brings the one window up");
    }

    /** The objects under a kind of the taxonomy's tree, in order. */
    private List<Class<? extends ObjectKind>> leaves(final TreeItem<Class<? extends ObjectKind>> item) {
        return item.isLeaf() ? List.of(item.getValue())
                : item.getChildren().stream().flatMap(child -> leaves(child).stream()).toList();
    }

    /** The cells of the taxonomy's row for a kind, as they read. */
    private List<String> row(final TreeTableView<Class<? extends ObjectKind>> taxonomy,
                             final Class<? extends ObjectKind> kind) {
        return FxThread.INSTANCE.call(() -> {
            final TreeItem<Class<? extends ObjectKind>> item = find(taxonomy.getRoot(), kind).orElseThrow();
            return taxonomy.getColumns().stream()
                    .map(column -> String.valueOf(column.getCellObservableValue(item).getValue()))
                    .toList();
        });
    }

    private Optional<TreeItem<Class<? extends ObjectKind>>> find(final TreeItem<Class<? extends ObjectKind>> item,
                                                                  final Class<? extends ObjectKind> kind) {
        return item.getValue() == kind ? Optional.of(item)
                : item.getChildren().stream().flatMap(child -> find(child, kind).stream()).findFirst();
    }

    @Test
    void everyTypeOfTheBenchSaysWhatItIsAndIsHeldToIt() {
        for (final Class<?> type : List.of(ClasspathUnits.class, Selection.class, IndexEntry.class,
                IndexEntry.Package.class, IndexEntry.OfClass.class, UnitTree.class, NestIndex.class,
                BenchView.class, ClassObjectView.class, ClassObjectView.Line.class, ClassObjectView.Row.class,
                ClassObjectView.Step.class, SourceView.class, SourceLines.class, SourceLines.Line.class, Focus.class,
                Focus.OfClass.class, Focus.OfValues.class, Focus.OfInitialisation.class, ClasspathEnforcers.class,
                RulesView.class, RulesView.Applied.class,
                IndexCell.class, ClassAttributes.class, UnitClasses.class, ValueText.class, TaxonomyView.class,
                Badge.class, Badges.class, Existence.TopLevel.class, Existence.StaticMember.class,
                Existence.InnerMember.class, Existence.Local.class, Existence.Anonymous.class,
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

    private TreeView<IndexEntry> selectInNest(final LaidOut bench, final String unit, final String nested) {
        select(bench, unit);
        final TreeView<IndexEntry> nest = nest(bench);
        nest.getSelectionModel().select(child(nest.getRoot(), nested));
        return nest;
    }

    @SuppressWarnings("unchecked")
    private TableView<ClassObjectView.Line> summary(final LaidOut bench) {
        return (TableView<ClassObjectView.Line>) bench.root().lookup("#summary");
    }

    private TableView<?> details(final LaidOut bench) {
        return (TableView<?>) bench.root().lookup("#details");
    }

    private TableView<?> initialisation(final LaidOut bench) {
        return (TableView<?>) bench.root().lookup("#initialisation");
    }

    /** A line of the summary for {@code ClassKind.Enum}, a record. */
    private ClassObjectView.Line line(final Class<? extends ObjectKind> object, final Cardinality cardinality,
                                      final long count) {
        return new ClassObjectView.Line(ENUM_RECORD, object, Optional.of(cardinality), count);
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

    private List<String> ruleNames(final List<RulesView.Applied> applied) {
        return applied.stream().map(rule -> rule.signature().rule().getSimpleName()).toList();
    }

    private List<String> labels(final TreeItem<IndexEntry> item) {
        return item.getChildren().stream().map(child -> child.getValue().toString()).toList();
    }

    /** The texts of a class entry's badges, its kind's first. */
    private List<String> badges(final TreeItem<IndexEntry> item) {
        return Badges.INSTANCE.of((IndexEntry.OfClass) item.getValue()).stream().map(Badge::text).toList();
    }

    /** The bench over the taxonomy module, with these views and a source, laid out in a scene. */
    private record LaidOut(Parent root) {

        LaidOut(final List<BenchView> views) {
            this(views, new SourceView());
        }

        LaidOut(final List<BenchView> views, final SourceView source) {
            this(BenchPane.INSTANCE.content(TAXONOMY, BenchPaneTest.class.getClassLoader(), views, source));
            new Scene(root, 1280, 800);
            root.applyCss();
            root.layout();
        }
    }
}
