package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The badges of a class in the bench's indexes: one for its kind, as the taxonomy has it, and, for a class nested in a
 * unit, one for how it exists there. A unit needs no second badge: every unit is a top-level class.
 */
public record Badges() implements StatelessFunctionalObject {

    public static final Badges INSTANCE = new Badges();

    private static final String NEUTRAL = "#7a7a7a";

    /** The badges of a class entry, its kind's first. */
    public List<Badge> of(final IndexEntry.OfClass entry) {
        final List<Badge> badges = new ArrayList<>(List.of(kind(entry.kind())));
        switch (entry.existence()) {
            case Existence.TopLevel _ -> {
            }
            case Existence.StaticMember _ -> badges.add(new Badge("static", "a static member class", NEUTRAL, false));
            case Existence.InnerMember _ -> badges.add(new Badge("inner", "an inner class", NEUTRAL, false));
            case Existence.Local _ -> badges.add(new Badge("local", "a local class", NEUTRAL, false));
            case Existence.Anonymous _ -> badges.add(new Badge("anon", "an anonymous class", NEUTRAL, false));
        }
        return List.copyOf(badges);
    }

    private Badge kind(final ClassKind kind) {
        return switch (kind) {
            case ClassKind.Class _ -> new Badge("C", "a class", "#3d7ab8", true);
            case ClassKind.AbstractClass _ -> new Badge("A", "an abstract class", "#5f86a8", true);
            case ClassKind.Interface _ -> new Badge("I", "an interface", "#3e9a5a", true);
            case ClassKind.Enum _ -> new Badge("E", "an enum", "#c0841a", true);
            case ClassKind.Record _ -> new Badge("R", "a record", "#8a5cc2", true);
            case ClassKind.Annotation _ -> new Badge("@", "an annotation", "#b8483d", true);
        };
    }
}
