package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.NestedClass;
import com.homing_labs.cq.lang.java.model.WithSource;
import com.homing_labs.cq.lang.java.taxonomy.MemberClass;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.StaticMemberClass;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The nested units of a unit, for a rule to delegate to: every member class in its attributes, of any kind and at any
 * depth, each as a static or an inner unit - the static member classes before the inner ones, and a class before those
 * nested in it.
 */
public record JavaNests() implements StatelessFunctionalObject {

    public static final JavaNests INSTANCE = new JavaNests();

    /** The nested units of a unit. */
    public List<JavaNestedUnit> of(final JavaUnit top) {
        final List<JavaNestedUnit> units = new ArrayList<>();
        collect(top, top.attributes(), units);
        return List.copyOf(units);
    }

    private void collect(final JavaUnit top, final Attributes enclosing, final List<JavaNestedUnit> into) {
        for (final WithSource<NestedClass> member : members(enclosing, StaticMemberClass.INSTANCE)) {
            into.add(new JavaNestedUnit.Static(top, member));
            collect(top, member.object().attributes(), into);
        }
        for (final WithSource<NestedClass> member : members(enclosing, MemberClass.INSTANCE)) {
            into.add(new JavaNestedUnit.Inner(top, member));
            collect(top, member.object().attributes(), into);
        }
    }

    /** The member classes of one kind, or none where a class of its kind cannot have them. */
    private List<WithSource<NestedClass>> members(final Attributes enclosing, final ObjectKind kind) {
        return enclosing.has(kind) ? enclosing.all(kind, NestedClass.class) : List.of();
    }
}
