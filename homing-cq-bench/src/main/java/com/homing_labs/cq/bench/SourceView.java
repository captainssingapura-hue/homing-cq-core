package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaUnit;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Measures the source: a place for now. Source-based measurements will come with the parser; until then it says
 * whether the selected unit's source was found, and where.
 */
public final class SourceView implements BenchView {

    private final Label message = new Label();

    private final StackPane pane = new StackPane(message);

    public SourceView() {
        message.setWrapText(true);
        message.setTextAlignment(TextAlignment.CENTER);
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
        message.setText("Source-based measurements will come with the parser." + selection.map(selected ->
                switch (selected.unit()) {
                    case JavaUnit.WithSource unit -> "\n\nSource of " + unit.type().getSimpleName() + ": "
                            + Path.of(unit.source().file());
                    case JavaUnit.ClassOnly unit -> "\n\nNo source was found for " + unit.type().getSimpleName() + ".";
                }).orElse(""));
    }

    /** The message shown. */
    public String text() {
        return message.getText();
    }
}
