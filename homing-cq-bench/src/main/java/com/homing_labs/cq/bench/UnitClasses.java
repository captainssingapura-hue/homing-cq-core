package com.homing_labs.cq.bench;

import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.model.EnclosedClass;
import com.homing_labs.cq.lang.java.model.Entry;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.unit.JavaUnit;
import hue.captains.singapura.tao.ontology.Scalar;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Every class of a unit with its attributes, read from the unit alone: the top-level class first, then each class
 * nested in it - members, and local and anonymous classes - each before the classes nested in it, in the order the
 * taxonomy declares their objects.
 */
public record UnitClasses() implements StatelessFunctionalObject {

    public static final UnitClasses INSTANCE = new UnitClasses();

    /** The classes of a unit, its top-level class first. */
    public List<ClassAttributes> of(final JavaUnit unit) {
        final List<ClassAttributes> classes = new ArrayList<>();
        collect(unit.identity().name(), unit.attributes(), classes);
        return List.copyOf(classes);
    }

    private void collect(final ClassName name, final Attributes attributes, final List<ClassAttributes> into) {
        into.add(new ClassAttributes(name, attributes));
        for (final Entry entry : attributes.entries()) {
            for (final WithSource<? extends Scalar> value : entry.all()) {
                switch (value.object()) {
                    case NestedClass nested -> collect(nested.name(), nested.attributes(), into);
                    case EnclosedClass enclosed -> collect(enclosed.name(), enclosed.attributes(), into);
                    default -> {
                    }
                }
            }
        }
    }
}
