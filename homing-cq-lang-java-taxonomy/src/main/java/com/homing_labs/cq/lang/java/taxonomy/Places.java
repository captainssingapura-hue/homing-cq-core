package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.Scalar;

import java.util.List;

/**
 * Where a kind of class keeps its members: the place of its top-level members, and those of its class object's and its
 * instances' members.
 */
public record Places(Class<? extends MemberOf> top, Class<? extends MemberOf> classObject,
                     Class<? extends MemberOf> instance) implements Scalar {

    /** The class object's place for a static member, the instances' for any other. */
    public Class<? extends MemberOf> subordinate(final boolean isStatic) {
        return isStatic ? classObject : instance;
    }

    /** The three places, the top-level one first. */
    public List<Class<? extends MemberOf>> all() {
        return List.of(top, classObject, instance);
    }
}
