package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.scene.control.TreeItem;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The index of a unit's nest, the bench's second layer: the unit's top-level class at the top, and under each class the
 * classes declared in it - member classes, and local and anonymous ones - by their names in the class file, all open.
 * Selecting the top measures the whole nest; selecting a nested class, that class alone.
 */
public record NestIndex() implements StatelessFunctionalObject {

    public static final NestIndex INSTANCE = new NestIndex();

    /** The index of the unit's nest. */
    public TreeItem<IndexEntry> of(final JavaUnit unit) {
        final Class<?> top = unit.type();
        final TreeItem<IndexEntry> root = item(unit, top);
        final List<Class<?>> nested = Arrays.stream(top.getNestMembers())
                .filter(type -> type != top)
                .sorted(Comparator.comparing(Class::getName))
                .toList();
        final Map<Class<?>, TreeItem<IndexEntry>> items = new HashMap<>(Map.of(top, root));
        nested.forEach(type -> items.put(type, item(unit, type)));
        nested.forEach(type -> items.getOrDefault(type.getEnclosingClass(), root).getChildren().add(items.get(type)));
        return root;
    }

    private TreeItem<IndexEntry> item(final JavaUnit unit, final Class<?> type) {
        final TreeItem<IndexEntry> item =
                new TreeItem<>(new IndexEntry.OfClass(new Selection(unit, type), label(type)));
        item.setExpanded(true);
        return item;
    }

    /** A class by its name where it is declared: {@code Slot}; {@code Point (local)}; {@code anonymous 1}. */
    private String label(final Class<?> type) {
        if (type.isAnonymousClass()) {
            return "anonymous " + type.getName().substring(type.getName().lastIndexOf('$') + 1);
        }
        return type.isLocalClass() ? type.getSimpleName() + " (local)" : type.getSimpleName();
    }
}
