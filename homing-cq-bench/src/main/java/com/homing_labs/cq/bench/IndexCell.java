package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.Mutable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;

/**
 * A cell of the bench's indexes: an entry's name, and before a class's name its {@link Badges}.
 */
public final class IndexCell extends TreeCell<IndexEntry> implements Mutable {

    @Override
    protected void updateItem(final IndexEntry entry, final boolean empty) {
        super.updateItem(entry, empty);
        if (empty || entry == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        setText(entry.toString());
        setGraphic(entry instanceof IndexEntry.OfClass ofClass ? badges(ofClass) : null);
    }

    private HBox badges(final IndexEntry.OfClass entry) {
        final HBox badges = new HBox(3);
        badges.setAlignment(Pos.CENTER_LEFT);
        Badges.INSTANCE.of(entry).forEach(badge -> badges.getChildren().add(label(badge)));
        return badges;
    }

    private Label label(final Badge badge) {
        final Label label = new Label(badge.text());
        label.setMinWidth(16);
        label.setAlignment(Pos.CENTER);
        label.setTooltip(new Tooltip(badge.meaning()));
        label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 0 4 0 4; "
                + "-fx-background-radius: 3; -fx-border-radius: 3; "
                + (badge.filled() ? "-fx-background-color: " + badge.colour() + "; -fx-text-fill: white;"
                : "-fx-border-color: " + badge.colour() + "; -fx-text-fill: " + badge.colour() + ";"));
        return label;
    }
}
