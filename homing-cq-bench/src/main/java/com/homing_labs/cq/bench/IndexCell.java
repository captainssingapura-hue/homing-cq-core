package com.homing_labs.cq.bench;

import hue.captains.singapura.tao.ontology.Mutable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

/**
 * A cell of the bench's indexes: an entry's name, before a class's name its {@link Badges}, and after the name of a
 * package or a unit in the tree of units its compliance.
 */
public final class IndexCell extends TreeCell<IndexEntry> implements Mutable {

    @Override
    protected void updateItem(final IndexEntry entry, final boolean empty) {
        super.updateItem(entry, empty);
        setText(null);
        if (empty || entry == null) {
            setGraphic(null);
            return;
        }
        final HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        before(entry).forEach(badge -> row.getChildren().add(Badges.INSTANCE.label(badge)));
        row.getChildren().add(new Label(entry.toString()));
        after(entry).ifPresent(badge -> row.getChildren().add(Badges.INSTANCE.label(badge)));
        setGraphic(row);
    }

    private List<Badge> before(final IndexEntry entry) {
        return entry instanceof IndexEntry.OfClass ofClass ? Badges.INSTANCE.of(ofClass) : List.of();
    }

    private Optional<Badge> after(final IndexEntry entry) {
        return switch (entry) {
            case IndexEntry.Package inPackage -> Optional.of(Badges.INSTANCE.ofUnits(inPackage.compliance()));
            case IndexEntry.OfClass ofClass -> ofClass.compliance().map(Badges.INSTANCE::ofUnit);
        };
    }
}
