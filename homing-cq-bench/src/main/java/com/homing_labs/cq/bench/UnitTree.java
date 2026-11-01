package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The tree of units on the left of the bench, as an IDE has it: a node for each part of a package's name, from the
 * first - {@code com}, {@code org} - down, all open; under each package the packages below it, then its units. Each
 * node tallies how many of the units under it comply with the rules, out of how many. It ends at the units, the
 * top-level classes; what is nested in a unit is in the {@link NestIndex}.
 */
public record UnitTree() implements StatelessFunctionalObject {

    public static final UnitTree INSTANCE = new UnitTree();

    /**
     * The tree of these units, each with its compliance, under a root that stands for no package and is not shown:
     * its children are the first parts of the units' package names.
     */
    public TreeItem<IndexEntry> of(final List<JavaUnit> units, final Function<JavaUnit, Compliance> compliance) {
        return node("", "", units, compliance);
    }

    /** The node of a package, over the units in it and below it. */
    private TreeItem<IndexEntry> node(final String name, final String label, final List<JavaUnit> under,
                                      final Function<JavaUnit, Compliance> compliance) {
        final List<TreeItem<IndexEntry>> children = new ArrayList<>();
        under.stream()
                .filter(unit -> !unit.type().getPackageName().equals(name))
                .collect(Collectors.groupingBy(unit -> partBelow(name, unit.type().getPackageName()), TreeMap::new,
                        Collectors.toList()))
                .forEach((part, deeper) -> children.add(node(name.isEmpty() ? part : name + "." + part, part, deeper,
                        compliance)));
        under.stream()
                .filter(unit -> unit.type().getPackageName().equals(name))
                .forEach(unit -> children.add(new TreeItem<>(new IndexEntry.OfClass(new Selection(unit, unit.type()),
                        unit.type().getSimpleName(), unit.kind(), Existence.TopLevel.INSTANCE,
                        Optional.of(compliance.apply(unit))))));
        final TreeItem<IndexEntry> node = new TreeItem<>(new IndexEntry.Package(name, label,
                children.stream().map(this::compliance).reduce(Compliance.NONE, Compliance::plus)));
        node.getChildren().setAll(children);
        node.setExpanded(true);
        return node;
    }

    /** The part of a package's name just below a package it is in: {@code cq} below {@code com.homing_labs}. */
    private String partBelow(final String name, final String packageName) {
        final String rest = name.isEmpty() ? packageName : packageName.substring(name.length() + 1);
        final int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    private Compliance compliance(final TreeItem<IndexEntry> item) {
        return switch (item.getValue()) {
            case IndexEntry.Package inPackage -> inPackage.compliance();
            case IndexEntry.OfClass unit -> unit.compliance().orElse(Compliance.NONE);
        };
    }
}
