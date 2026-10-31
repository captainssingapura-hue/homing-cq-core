package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.taxonomy.Cardinality;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Placement;
import com.homing_labs.cq.lang.java.taxonomy.Taxonomy;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the attributes of a class from the taxonomy: an entry for every object a class of its kind can have,
 * shaped by the object's cardinality, from the values found for it - none, where none were found. It holds attributes
 * made any other way to the same.
 */
public record Entries() implements StatelessFunctionalObject {

    public static final Entries INSTANCE = new Entries();

    /** The attributes of a class of a kind, from the values found for each object it has. */
    public Attributes of(final ClassKind kind, final Map<ObjectKind, List<WithSource<? extends Scalar>>> found) {
        final Map<ObjectKind, Entry> entries = new LinkedHashMap<>();
        for (final Placement placement : Taxonomy.INSTANCE.placements(kind)) {
            final ObjectKind object = placement.object();
            final List<WithSource<? extends Scalar>> values = found.getOrDefault(object, List.of());
            entries.put(object, switch (placement.cardinality()) {
                case Cardinality.One _ when values.size() == 1 -> new Entry.One(object, values.getFirst());
                case Cardinality.ZeroOrOne _ when values.size() <= 1 ->
                        new Entry.ZeroOrOne(object, values.stream().findFirst());
                case Cardinality.OneOrMore _, Cardinality.ZeroOrMore _ -> new Entry.Many(object, new Values(values));
                default -> throw new IllegalArgumentException(miscount(kind, placement, values.size()));
            });
        }
        found.keySet().stream()
                .filter(object -> !entries.containsKey(object))
                .findFirst()
                .ifPresent(object -> {
                    throw new IllegalArgumentException(name(kind) + " has no " + name(object));
                });
        return new Attributes(kind, entries);
    }

    /** Attributes' entries held to the taxonomy, in the order it declares their objects. */
    Map<ObjectKind, Entry> checked(final ClassKind kind, final Map<ObjectKind, Entry> byObject) {
        final Map<ObjectKind, Entry> checked = new LinkedHashMap<>();
        for (final Placement placement : Taxonomy.INSTANCE.placements(kind)) {
            final ObjectKind object = placement.object();
            final Entry entry = byObject.get(object);
            if (entry == null) {
                throw new IllegalArgumentException("The attributes of " + name(kind).toLowerCase() + " have no entry"
                        + " for " + name(object));
            }
            if (!entry.object().equals(object)) {
                throw new IllegalArgumentException("The entry for " + name(object) + " is of "
                        + name(entry.object()));
            }
            if (!shaped(entry, placement.cardinality())) {
                throw new IllegalArgumentException("The entry for " + name(object) + " is "
                        + entry.getClass().getSimpleName() + ", but " + name(kind).toLowerCase() + " has "
                        + allowed(placement.cardinality()) + " " + name(object));
            }
            if (!placement.cardinality().allows(entry.all().size())) {
                throw new IllegalArgumentException(miscount(kind, placement, entry.all().size()));
            }
            final Class<? extends Scalar> type = ValueTypes.INSTANCE.of(object);
            entry.all().stream()
                    .filter(value -> !type.isInstance(value.object()))
                    .findFirst()
                    .ifPresent(value -> {
                        throw new IllegalArgumentException(name(object) + " holds " + type.getSimpleName() + ", not "
                                + value.object().getClass().getSimpleName());
                    });
            checked.put(object, entry);
        }
        byObject.keySet().stream()
                .filter(object -> !checked.containsKey(object))
                .findFirst()
                .ifPresent(object -> {
                    throw new IllegalArgumentException(name(kind) + " has no " + name(object));
                });
        return Collections.unmodifiableMap(checked);
    }

    /** An object of the taxonomy as it names it: {@code StaticMethod.Implementation}, {@code Field}. */
    String name(final ObjectKind object) {
        final Class<?> type = object.getClass();
        return type.getName().substring(type.getPackageName().length() + 1).replace('$', '.');
    }

    /** A kind of class in prose, as the subject of a sentence: {@code An enum}. */
    String name(final ClassKind kind) {
        return switch (kind) {
            case ClassKind.Class _ -> "A class";
            case ClassKind.AbstractClass _ -> "An abstract class";
            case ClassKind.Interface _ -> "An interface";
            case ClassKind.Enum _ -> "An enum";
            case ClassKind.Record _ -> "A record";
            case ClassKind.Annotation _ -> "An annotation";
        };
    }

    /** Whether an entry has the shape a cardinality gives. */
    private boolean shaped(final Entry entry, final Cardinality cardinality) {
        return switch (cardinality) {
            case Cardinality.One _ -> entry instanceof Entry.One;
            case Cardinality.ZeroOrOne _ -> entry instanceof Entry.ZeroOrOne;
            case Cardinality.OneOrMore _, Cardinality.ZeroOrMore _ -> entry instanceof Entry.Many;
        };
    }

    private String miscount(final ClassKind kind, final Placement placement, final int count) {
        return name(kind) + " has " + allowed(placement.cardinality()) + " " + name(placement.object()) + ", not "
                + count;
    }

    private String allowed(final Cardinality cardinality) {
        return switch (cardinality) {
            case Cardinality.One _ -> "exactly one";
            case Cardinality.ZeroOrOne _ -> "at most one";
            case Cardinality.OneOrMore _ -> "at least one";
            case Cardinality.ZeroOrMore _ -> "any number of";
        };
    }
}
