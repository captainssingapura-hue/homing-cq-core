package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.Mutable;
import javafx.scene.Node;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A view in the middle of the bench, in a tab of its own: it shows what it measures of what is selected in the tree. A
 * view is a JavaFX node, and changes with the selection.
 */
public interface BenchView extends Mutable {

    /** The title of its tab. */
    String title();

    /** What it shows. */
    Node content();

    /** Shows the selection, or, with nothing selected, says how to make one. */
    void show(Optional<Selection> selection);

    /**
     * Tells the listener what it has selected itself, whenever that changes, for the source to show where it is
     * written: nothing, when its selection is cleared. A view that selects nothing of its own never tells.
     */
    default void onFocus(final Consumer<Optional<Focus>> listener) {
    }
}
