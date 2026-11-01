package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.extract.SourceLocator;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassInitialisation;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.CodeBlocks;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.outline.CodeBlock;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Mutable;
import hue.captains.singapura.tao.ontology.Scalar;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The source file that declares the selected unit, beside the bench's views and always in view: its lines, numbered,
 * under how many there are and where the file is, with what is selected highlighted where its source says it is
 * written - the class selected in the nest, or what a view has selected of it. A class nested in the unit is in the
 * same file, so selecting one shows it too. A unit read without its source says so.
 */
public final class SourceView implements Mutable {

    private final Label caption = new Label();

    private final Label placed = new Label();

    private final SourceLines lines = new SourceLines();

    private final BorderPane pane = new BorderPane(lines.content(), new VBox(caption, placed), null, null, null);

    private Optional<Selection> selection = Optional.empty();

    private Optional<Focus> focus = Optional.empty();

    /** The unit whose source is shown, and where its top-level class is declared in it. */
    private Optional<JavaUnit> shown = Optional.empty();

    private Optional<CodeBlock> declaration = Optional.empty();

    public SourceView() {
        pane.setId("source");
        caption.setPadding(new Insets(6, 8, 0, 8));
        placed.setPadding(new Insets(2, 8, 6, 8));
        placed.setWrapText(true);
        placed.setStyle("-fx-text-fill: #555555;");
    }

    /** What it shows. */
    public Node content() {
        return pane;
    }

    /** Shows the file that declares the selected unit, with the class selected highlighted. */
    public void show(final Optional<Selection> selection) {
        this.selection = selection;
        focus = Optional.empty();
        final Optional<JavaUnit> unit = selection.map(Selection::unit);
        if (unit.orElse(null) != shown.orElse(null)) {
            shown = unit;
            declaration = unit.flatMap(SourceLocator.INSTANCE::declaration);
            lines.show(unit.flatMap(JavaUnit::source).map(JavaSource::text).orElse(""));
        }
        caption.setText(unit.map(selected -> selected.source()
                        .map(found -> "Source of " + selected.type().getSimpleName() + ", " + lines(found.text())
                                + ": " + Path.of(found.file()))
                        .orElse("No source was found for " + selected.type().getSimpleName() + "."))
                .orElse("Select a unit, or a class nested in one, to see the source file that declares it."));
        highlight();
    }

    /** Highlights what a view has selected of the class selected, or, with nothing, the class itself. */
    public void focus(final Optional<Focus> focus) {
        this.focus = focus;
        highlight();
    }

    /** What the caption says. */
    public String caption() {
        return caption.getText();
    }

    /** What it says of what is highlighted. */
    public String placed() {
        return placed.getText();
    }

    /** The source shown, or nothing. */
    public String text() {
        return lines.text();
    }

    /** The stretches highlighted as where what is selected is written, as they read. */
    public List<String> written() {
        return lines.written();
    }

    /** The stretches highlighted, lighter, as where what is selected is written somewhere within. */
    public List<String> within() {
        return lines.within();
    }

    private void highlight() {
        if (selection.isEmpty() || selection.get().unit().source().isEmpty()) {
            lines.highlight(new CodeBlocks(List.of()), new CodeBlocks(List.of()));
            placed.setText("");
            return;
        }
        final Focus highlighted = focus.orElseGet(() -> new Focus.OfClass(
                new ClassName(selection.get().type().getName())));
        if (highlighted instanceof Focus.OfClass ofClass
                && ofClass.name().equals(selection.get().unit().identity().name())) {
            lines.highlight(new CodeBlocks(declaration.stream().toList()), new CodeBlocks(List.of()));
            placed.setText(shortName(ofClass.name()) + (declaration.isPresent() ? " is declared where highlighted."
                    : " is not declared in this file."));
            return;
        }
        final List<ProcessedSource<?>> sources = sources(selection.get().unit(), highlighted);
        final List<CodeBlock> written = new ArrayList<>();
        final List<CodeBlock> within = new ArrayList<>();
        for (final ProcessedSource<?> source : sources) {
            switch (source) {
                case ProcessedSource.OfConstructor constructor -> {
                    written.add(constructor.block());
                    written.addAll(constructor.initialisers().items());
                }
                case ProcessedSource.Written<?> found -> written.add(found.block());
                case ProcessedSource.OfInitialisation initialisation -> initialisation.steps().items()
                        .forEach(step -> written.add(step.block()));
                case ProcessedSource.Within<?> found -> within.addAll(found.blocks().items());
                case ProcessedSource.Unwritten<?> _, ProcessedSource.Unplaced<?> _,
                     ProcessedSource.Unavailable<?> _ -> {
                }
            }
        }
        lines.highlight(ordered(written), ordered(within));
        placed.setText(said(highlighted, sources));
    }

    /**
     * The sources of what is highlighted: of the values a view has selected, or of a class nested in the unit, as the
     * value that holds it - a member, or a local or anonymous class - says.
     */
    private List<ProcessedSource<?>> sources(final JavaUnit unit, final Focus focus) {
        return switch (focus) {
            case Focus.OfValues values -> values.values().items().stream()
                    .<ProcessedSource<?>>map(WithSource::source)
                    .toList();
            case Focus.OfClass ofClass -> List.of(holding(unit.attributes(), ofClass.name())
                    .<ProcessedSource<?>>map(WithSource::source)
                    .orElseGet(ProcessedSource.Unwritten::new));
            case Focus.OfInitialisation initialisation -> List.of(initialisation.initialisation().source());
        };
    }

    /** The value that holds a class nested in the unit, at any depth: a member, or a local or anonymous class. */
    private Optional<WithSource<? extends Scalar>> holding(final Attributes attributes, final ClassName name) {
        return attributes.entries().stream()
                .flatMap(entry -> entry.all().stream())
                .flatMap(value -> switch (value.object()) {
                    case NestedClass nested -> nested.name().equals(name) ? Stream.of(value)
                            : holding(nested.attributes(), name).stream();
                    case EnclosedClass enclosed -> enclosed.name().equals(name) ? Stream.of(value)
                            : holding(enclosed.attributes(), name).stream();
                    default -> Stream.empty();
                })
                .findFirst();
    }

    private CodeBlocks ordered(final List<CodeBlock> blocks) {
        return new CodeBlocks(blocks.stream().distinct().sorted(Comparator.comparingInt(CodeBlock::offset)).toList());
    }

    /** What the sources of what is highlighted come to, in a sentence. */
    private String said(final Focus focus, final List<ProcessedSource<?>> sources) {
        return switch (focus) {
            case Focus.OfClass ofClass -> shortName(ofClass.name()) + switch (sources.getFirst()) {
                case ProcessedSource.Written<?> _, ProcessedSource.OfInitialisation _ ->
                        " is declared where highlighted.";
                case ProcessedSource.Within<?> _ -> " is declared somewhere in the lightly highlighted code, which is"
                        + " not read yet.";
                case ProcessedSource.Unwritten<?> _, ProcessedSource.Unplaced<?> _,
                     ProcessedSource.Unavailable<?> _ -> " is not declared in this file.";
            };
            case Focus.OfValues values -> {
                final String object = kindName(values.object().getClass()) + " of " + shortName(values.inClass());
                if (sources.isEmpty()) {
                    yield "No " + object + ".";
                }
                final boolean one = sources.size() == 1;
                final List<String> said = new ArrayList<>();
                count(sources, one, "written where highlighted", said,
                        source -> source instanceof ProcessedSource.Written<?>);
                count(sources, one, "written somewhere in the lightly highlighted code, which is not read yet", said,
                        source -> source instanceof ProcessedSource.Within<?>);
                count(sources, one, "written nowhere: the compiler implied or generated it", said,
                        source -> source instanceof ProcessedSource.Unwritten<?>);
                count(sources, one, "not looked for: dependencies are not placed yet", said,
                        source -> source instanceof ProcessedSource.Unplaced<?>);
                count(sources, one, "not known: the unit was read without its source", said,
                        source -> source instanceof ProcessedSource.Unavailable<?>);
                yield (one ? object : sources.size() + " values of " + object) + ": " + String.join("; ", said) + ".";
            }
            case Focus.OfInitialisation initialisation -> initialised(initialisation);
        };
    }

    /** How a class is initialised, or the steps of it selected, in a sentence. */
    private String initialised(final Focus.OfInitialisation focus) {
        final String name = shortName(focus.inClass());
        return switch (focus.initialisation().object()) {
            case ClassInitialisation.None _ -> name + " runs no code to be initialised: its static fields are"
                    + " constants, or keep their defaults.";
            case ClassInitialisation.Code _ -> switch (focus.initialisation().source()) {
                case ProcessedSource.OfInitialisation written -> {
                    final int steps = written.steps().items().size();
                    yield "How " + name + " is initialised: " + (steps == 1 ? "a step" : steps + " steps")
                            + ", written where highlighted.";
                }
                case ProcessedSource.Within<?> _ -> name + " is initialised by code written somewhere in the lightly"
                        + " highlighted code, which is not read yet.";
                case ProcessedSource.Unavailable<?> _ -> name + " runs code to be initialised; the unit was read"
                        + " without its source.";
                default -> name + " runs code to be initialised that its source does not write: the compiler's own.";
            };
        };
    }

    private void count(final List<ProcessedSource<?>> sources, final boolean one, final String what,
                       final List<String> said, final Predicate<ProcessedSource<?>> kind) {
        final long count = sources.stream().filter(kind).count();
        if (count > 0) {
            said.add(one ? what : count + " " + what);
        }
    }

    /** A class without its package: {@code SampleEnum$1}. */
    private String shortName(final ClassName name) {
        return name.binaryName().substring(name.binaryName().lastIndexOf('.') + 1);
    }

    /** A kind of object as the taxonomy names it: {@code StaticMethod.Implementation}, {@code Field}. */
    private String kindName(final Class<?> kind) {
        return kind.getName().substring(kind.getPackageName().length() + 1).replace('$', '.');
    }

    private String lines(final String text) {
        final long lines = text.lines().count();
        return lines + (lines == 1 ? " line" : " lines");
    }
}
