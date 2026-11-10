package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.scan.Compliance;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The badges of the bench's indexes. A class has one for its kind, as the taxonomy has it, and, when nested in a unit,
 * one for how it exists there; a unit needs no second badge, as every unit is a top-level class. In the tree of
 * units, a package has one for how many of its units comply, and a unit one for whether it does: lively green when
 * all do; from yellow to red as the share that does falls to 70%; and below that, alarm red. In the rule book, a rule
 * has one for how many of the units it checks comply with it, coloured alike.
 */
public record Badges() implements StatelessFunctionalObject {

    public static final Badges INSTANCE = new Badges();

    private static final String NEUTRAL = "#7a7a7a";

    /** All the units comply. */
    public static final String COMPLIANT = "#22c55e";

    /** Fewer than {@link #ALARM_BELOW} of the units comply. */
    public static final String ALARM = "#d50000";

    /** The share of units complying below which the colour stops going from yellow to red, and is alarm red. */
    public static final double ALARM_BELOW = 0.7;

    /** The hue, in degrees, just short of all the units complying: a yellow. */
    private static final double YELLOW = 52;

    /** The hue at {@link #ALARM_BELOW} of them: a red. */
    private static final double RED = 6;

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

    /** The badge of a package: how many of the units in it and below it comply, out of how many. */
    public Badge ofUnits(final Compliance compliance) {
        return new Badge(compliance.compliant() + "/" + compliance.total(), compliance.compliant() + " of "
                + compliance.total() + " units comply with every rule applied to them; the rest have "
                + count(compliance.findings(), "finding") + " between them", colour(compliance), true);
    }

    /** The badge of a unit: a tick if it complies; else its findings counted, or {@code !} if a rule failed on it. */
    public Badge ofUnit(final Compliance compliance) {
        if (compliance.complete()) {
            return new Badge("✓", "complies with every rule applied to it", colour(compliance), true);
        }
        return compliance.findings() > 0
                ? new Badge(String.valueOf(compliance.findings()), count(compliance.findings(), "finding")
                + " of the rules applied to it", colour(compliance), true)
                : new Badge("!", "a rule applied to it failed, so it cannot be said to comply", colour(compliance),
                true);
    }

    /**
     * The badge of a rule in the rule book: how many of the units it checks comply with it, out of how many; grey, when
     * it checks none.
     */
    public Badge ofRule(final Compliance compliance) {
        return compliance.total() == 0
                ? new Badge("0/0", "it checks none of the units scanned", NEUTRAL, true)
                : new Badge(compliance.compliant() + "/" + compliance.total(), compliance.compliant() + " of "
                + count(compliance.total(), "unit") + " it checks comply with it", colour(compliance), true);
    }

    /** A badge as it is shown: its text in a small label, filled or outlined in its colour, its meaning on hovering. */
    public Label label(final Badge badge) {
        final Label label = new Label(badge.text());
        label.setMinWidth(16);
        label.setAlignment(Pos.CENTER);
        label.setTooltip(new Tooltip(badge.meaning()));
        label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 0 4 0 4; "
                + "-fx-background-radius: 3; -fx-border-radius: 3; "
                + (badge.filled() ? "-fx-background-color: " + badge.colour() + "; -fx-text-fill: " + badge.ink() + ";"
                : "-fx-border-color: " + badge.colour() + "; -fx-text-fill: " + badge.colour() + ";"));
        return label;
    }

    /** The colour of a share of units complying. */
    public String colour(final Compliance compliance) {
        if (compliance.complete()) {
            return COMPLIANT;
        }
        if (compliance.share() < ALARM_BELOW) {
            return ALARM;
        }
        final double towardsRed = (1 - compliance.share()) / (1 - ALARM_BELOW);
        final Color colour = Color.hsb(YELLOW + (RED - YELLOW) * towardsRed, 0.9, 1);
        return String.format(Locale.ROOT, "#%02x%02x%02x", Math.round(colour.getRed() * 255),
                Math.round(colour.getGreen() * 255), Math.round(colour.getBlue() * 255));
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

    private String count(final int count, final String thing) {
        return count + " " + thing + (count == 1 ? "" : "s");
    }
}
