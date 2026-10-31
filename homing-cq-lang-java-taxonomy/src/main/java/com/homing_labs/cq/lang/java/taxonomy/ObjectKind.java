package com.homing_labs.cq.lang.java.taxonomy;

import hue.captains.singapura.tao.ontology.Stateless;

/**
 * A kind of object a compiled Java class is made of, as it is loaded and run: the root of the taxonomy.
 *
 * <p>A class is made of what it has of its own: what it declares, and what the compiler adds to it. What it inherits is
 * not part of it - that belongs to the superclass or interface that has it, and is found by walking up the inheritance
 * tree.</p>
 *
 * <p>Beside its members, a class has properties of its own - its {@link ClassProperty}s; they are objects of the
 * taxonomy too.</p>
 *
 * <p>A kind holds nothing, so its type is all there is to it, and any two instances of one kind are equal. The
 * taxonomy is sealed: the kinds it permits, and those they permit in turn, are all the kinds there are, and it grows by
 * adding them here.</p>
 */
public sealed interface ObjectKind extends Stateless
        permits ClassKind, ClassProperty, ClassAnnotation, TypeParameter, Constructor, StaticInitialiser, Superclass,
        Superinterface, Dependency, StaticField, EnumConstant, Field, RecordComponent, StaticMethod, Method,
        StaticMemberClass, MemberClass, LocalClass, AnonymousClass {
}
