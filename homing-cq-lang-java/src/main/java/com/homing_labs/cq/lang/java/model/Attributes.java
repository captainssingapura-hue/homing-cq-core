package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;
import java.util.Map;

/**
 * The attributes of a class: for each object of the taxonomy a class of its kind can have, what this class has of it.
 * They are generated from the taxonomy - {@link Entries} makes them - and hold to it: an entry for every object a class
 * of the kind can have and for no other, in the order the taxonomy declares them, each shaped by the object's
 * cardinality, with as many values as that allows, each of the type {@link ValueTypes} gives the object. A vector of
 * its entries, kept by their object.
 */
public record Attributes(ClassKind kind, Map<ObjectKind, Entry> byObject) implements Vector<Entry> {

    public Attributes {
        byObject = Entries.INSTANCE.checked(kind, byObject);
    }

    /** The entries, in the order the taxonomy declares their objects. */
    public List<Entry> entries() {
        return List.copyOf(byObject.values());
    }

    /** Whether a class of this kind can have the object at all. */
    public boolean has(final ObjectKind object) {
        return byObject.containsKey(object);
    }

    /** What the class has of an object, which a class of its kind must be able to have. */
    public Entry entry(final ObjectKind object) {
        final Entry entry = byObject.get(object);
        if (entry == null) {
            throw new IllegalArgumentException(Entries.INSTANCE.name(kind) + " has no "
                    + Entries.INSTANCE.name(object));
        }
        return entry;
    }

    /** Every value the class has of an object, however its cardinality shapes them, as the type the object holds. */
    @SuppressWarnings("unchecked")
    public <V extends Scalar> List<WithSource<V>> all(final ObjectKind object, final Class<V> type) {
        final Class<? extends Scalar> holds = ValueTypes.INSTANCE.of(object);
        if (!holds.equals(type)) {
            throw new IllegalArgumentException(Entries.INSTANCE.name(object) + " holds " + holds.getSimpleName()
                    + ", not " + type.getSimpleName());
        }
        return entry(object).all().stream().map(value -> (WithSource<V>) value).toList();
    }
}
