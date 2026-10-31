package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.def.SourcePlace;
import com.homing_labs.cq.lang.java.def.SourceSpan;
import com.homing_labs.cq.lang.java.def.SourceSpans;
import com.homing_labs.cq.lang.java.extract.SourceLocator;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.outline.Outline;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Mutable;
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

/**
 * The source file that declares the selected unit, beside the bench's views and always in view: its lines, numbered,
 * under how many there are and where the file is, with what is selected highlighted where the {@link SourceLocator}
 * finds it written - the class selected in the nest, or what a view has selected of it. A class nested in the unit is
 * in the same file, so selecting one shows it too. A unit read without its source says so.
 */
public final class SourceView implements Mutable {

    private final Label caption = new Label();

    private final Label placed = new Label();

    private final SourceLines lines = new SourceLines();

    private final BorderPane pane = new BorderPane(lines.content(), new VBox(caption, placed), null, null, null);

    private Optional<Selection> selection = Optional.empty();

    private Optional<Focus> focus = Optional.empty();

    /** The unit whose source is shown, and that source outlined, to look in. */
    private Optional<JavaUnit> shown = Optional.empty();

    private Optional<Outline> outline = Optional.empty();

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
            outline = unit.flatMap(SourceLocator.INSTANCE::outline);
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
        if (selection.isEmpty() || outline.isEmpty()) {
            lines.highlight(new SourceSpans(List.of()), new SourceSpans(List.of()));
            placed.setText("");
            return;
        }
        final Focus highlighted = focus.orElseGet(() -> new Focus.OfClass(
                new ClassName(selection.get().type().getName())));
        final List<SourcePlace> places = places(selection.get().unit(), outline.get(), highlighted);
        final List<SourceSpan> written = new ArrayList<>();
        final List<SourceSpan> within = new ArrayList<>();
        for (final SourcePlace place : places) {
            switch (place) {
                case SourcePlace.Written found -> written.addAll(found.spans().items());
                case SourcePlace.Within found -> within.addAll(found.spans().items());
                case SourcePlace.Unwritten _, SourcePlace.Unplaced _ -> {
                }
            }
        }
        lines.highlight(ordered(written), ordered(within));
        placed.setText(said(highlighted, places));
    }

    private List<SourcePlace> places(final JavaUnit unit, final Outline outline, final Focus focus) {
        return switch (focus) {
            case Focus.OfClass ofClass -> List.of(SourceLocator.INSTANCE.ofClass(unit, outline, ofClass.name()));
            case Focus.OfValues values -> values.values().items().stream()
                    .map(value -> SourceLocator.INSTANCE.of(unit, outline, values.inClass(), values.object(),
                            value.object()))
                    .toList();
        };
    }

    private SourceSpans ordered(final List<SourceSpan> spans) {
        return new SourceSpans(spans.stream().distinct().sorted(Comparator.comparingInt(SourceSpan::start)).toList());
    }

    /** What the places of what is highlighted come to, in a sentence. */
    private String said(final Focus focus, final List<SourcePlace> places) {
        return switch (focus) {
            case Focus.OfClass ofClass -> shortName(ofClass.name()) + switch (places.getFirst()) {
                case SourcePlace.Written _ -> " is declared where highlighted.";
                case SourcePlace.Within _ -> " is declared somewhere in the lightly highlighted code, which is not"
                        + " read yet.";
                case SourcePlace.Unwritten _, SourcePlace.Unplaced _ -> " is not declared in this file.";
            };
            case Focus.OfValues values -> {
                final String object = kindName(values.object().getClass()) + " of " + shortName(values.inClass());
                if (places.isEmpty()) {
                    yield "No " + object + ".";
                }
                final boolean one = places.size() == 1;
                final List<String> said = new ArrayList<>();
                count(places, SourcePlace.Written.class, one, "written where highlighted", said);
                count(places, SourcePlace.Within.class, one,
                        "written somewhere in the lightly highlighted code, which is not read yet", said);
                count(places, SourcePlace.Unwritten.class, one,
                        "written nowhere: the compiler implied or generated it", said);
                count(places, SourcePlace.Unplaced.class, one,
                        "not looked for: dependencies are not placed yet", said);
                yield (one ? object : places.size() + " values of " + object) + ": " + String.join("; ", said) + ".";
            }
        };
    }

    private void count(final List<SourcePlace> places, final Class<? extends SourcePlace> kind, final boolean one,
                       final String what, final List<String> said) {
        final long count = places.stream().filter(kind::isInstance).count();
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
