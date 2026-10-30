package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.def.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The tree of units on the left of the bench, as an IDE has it: a node for each package with units, by name and open,
 * and under each package its units. It ends at the units, the top-level classes; what is nested in a unit is in the
 * {@link NestIndex}.
 */
public record UnitTree() implements StatelessFunctionalObject {

    public static final UnitTree INSTANCE = new UnitTree();

    /** The tree of the units under {@code rootPackage}. */
    public TreeItem<IndexEntry> of(final String rootPackage, final List<JavaUnit> units) {
        final TreeItem<IndexEntry> root = new TreeItem<>(new IndexEntry.Package(rootPackage));
        root.setExpanded(true);
        units.stream()
                .collect(Collectors.groupingBy(unit -> unit.type().getPackageName(), TreeMap::new, Collectors.toList()))
                .forEach((name, inPackage) -> {
                    final TreeItem<IndexEntry> node = new TreeItem<>(new IndexEntry.Package(name));
                    node.setExpanded(true);
                    inPackage.forEach(unit -> node.getChildren().add(new TreeItem<>(new IndexEntry.OfClass(
                            new Selection(unit, unit.type()), unit.type().getSimpleName()))));
                    root.getChildren().add(node);
                });
        return root;
    }
}
