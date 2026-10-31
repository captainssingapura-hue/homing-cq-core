package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The source file that declares the selected unit, as it was read: its text, read-only and in a fixed-width font, under
 * how many lines it has and where it is. A class nested in the unit is in the same file, so selecting one shows it too.
 * A unit read without its source says so.
 */
public final class SourceView implements BenchView {

    private final Label caption = new Label();

    private final TextArea source = new TextArea();

    private final BorderPane pane = new BorderPane(source, caption, null, null, null);

    public SourceView() {
        caption.setPadding(new Insets(6, 8, 6, 8));
        source.setId("source");
        source.setEditable(false);
        source.setWrapText(false);
        source.setStyle("-fx-font-family: monospace;");
    }

    @Override
    public String title() {
        return "Source";
    }

    @Override
    public Node content() {
        return pane;
    }

    @Override
    public void show(final Optional<Selection> selection) {
        final Optional<JavaUnit> unit = selection.map(Selection::unit);
        final Optional<JavaSource> file = unit.flatMap(JavaUnit::source);
        caption.setText(unit.map(shown -> file
                        .map(found -> "Source of " + shown.type().getSimpleName() + ", " + lines(found.text()) + ": "
                                + Path.of(found.file()))
                        .orElse("No source was found for " + shown.type().getSimpleName() + "."))
                .orElse("Select a unit, or a class nested in one, to see the source file that declares it."));
        source.setText(file.map(JavaSource::text).orElse(""));
        source.positionCaret(0);
        source.setScrollTop(0);
    }

    /** What the caption says. */
    public String caption() {
        return caption.getText();
    }

    /** The source shown, or nothing. */
    public String text() {
        return source.getText();
    }

    private String lines(final String text) {
        final long lines = text.lines().count();
        return lines + (lines == 1 ? " line" : " lines");
    }
}
