package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.Mutable;
import javafx.scene.Node;

import java.util.Optional;

/**
 * A view on the right of the bench, in a tab of its own: it shows what it measures of what is selected in the tree. A
 * view is a JavaFX node, and changes with the selection.
 */
public interface BenchView extends Mutable {

    /** The title of its tab. */
    String title();

    /** What it shows. */
    Node content();

    /** Shows the selection, or, with nothing selected, says how to make one. */
    void show(Optional<Selection> selection);
}
