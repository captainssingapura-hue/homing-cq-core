package com.homing_labs.cq.lang.java.unit;

import com.homing_labs.cq.lang.java.def.JavaSource;
import com.homing_labs.cq.lang.java.extract.ClassFileAttributes;
import com.homing_labs.cq.lang.java.extract.SourceLocator;
import com.homing_labs.cq.lang.java.model.Attributes;
import com.homing_labs.cq.lang.java.model.ClassName;
import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Makes the unit of a top-level class, of the class's kind, from the loaded class and its source if it was found: the
 * one way in. The kind is read from the class, as reflection sees it - the same kind the taxonomy's scanner reads from
 * its class file: an annotation, an interface, an enum, a record, an abstract class, or else a class. Its attributes
 * are read from its class files, and, when it has its source, each value is given its source, processed by the
 * {@link SourceLocator}.
 */
public record JavaUnits() implements StatelessFunctionalObject {

    public static final JavaUnits INSTANCE = new JavaUnits();

    /** The unit of a top-level class, with its source if it was found. */
    public JavaUnit of(final Class<?> type, final Optional<JavaSource> source) {
        final Identity identity = new Identity(new ClassName(type.getName()), kindOf(type));
        final JavaUnit read = unit(type, identity, ClassFileAttributes.INSTANCE.of(type), source);
        return source.isEmpty() ? read : unit(type, identity, SourceLocator.INSTANCE.sourced(read), source);
    }

    private JavaUnit unit(final Class<?> type, final Identity identity, final Attributes attributes,
                          final Optional<JavaSource> source) {
        return switch (identity.kind()) {
            case ClassKind.Class _ -> new ClassUnit(type, identity, attributes, source);
            case ClassKind.AbstractClass _ -> new AbstractClassUnit(type, identity, attributes, source);
            case ClassKind.Interface _ -> new InterfaceUnit(type, identity, attributes, source);
            case ClassKind.Enum _ -> new EnumUnit(type, identity, attributes, source);
            case ClassKind.Record _ -> new RecordUnit(type, identity, attributes, source);
            case ClassKind.Annotation _ -> new AnnotationUnit(type, identity, attributes, source);
        };
    }

    /** The kind of a class. An enum is an enum even when its methods are abstract. */
    public ClassKind kindOf(final Class<?> type) {
        if (type.isAnnotation()) {
            return ClassKind.Annotation.INSTANCE;
        }
        if (type.isInterface()) {
            return ClassKind.Interface.INSTANCE;
        }
        if (type.isEnum()) {
            return ClassKind.Enum.INSTANCE;
        }
        if (type.isRecord()) {
            return ClassKind.Record.INSTANCE;
        }
        return Modifier.isAbstract(type.getModifiers()) ? ClassKind.AbstractClass.INSTANCE : ClassKind.Class.INSTANCE;
    }

    /**
     * Refuses a class that is nested, or that is not of the kind a unit is for, and an identity or attributes that are
     * not the class's.
     */
    void check(final Class<?> type, final Identity identity, final Attributes attributes, final ClassKind kind) {
        if (type.getEnclosingClass() != null) {
            throw new IllegalArgumentException("A JavaUnit is a top-level class, but " + type.getName()
                    + " is nested in " + type.getEnclosingClass().getName());
        }
        final ClassKind actual = kindOf(type);
        if (!actual.equals(kind)) {
            throw new IllegalArgumentException(type.getName() + " is " + name(actual) + ", not " + name(kind));
        }
        if (!identity.equals(new Identity(new ClassName(type.getName()), kind))) {
            throw new IllegalArgumentException("The identity of " + identity.name().binaryName() + ", "
                    + name(identity.kind()) + ", is not that of " + type.getName() + ", " + name(kind));
        }
        if (!attributes.kind().equals(kind)) {
            throw new IllegalArgumentException("The attributes are those of " + name(attributes.kind()) + ", but "
                    + type.getName() + " is " + name(kind));
        }
    }

    private String name(final ClassKind kind) {
        return switch (kind) {
            case ClassKind.Class _ -> "a class";
            case ClassKind.AbstractClass _ -> "an abstract class";
            case ClassKind.Interface _ -> "an interface";
            case ClassKind.Enum _ -> "an enum";
            case ClassKind.Record _ -> "a record";
            case ClassKind.Annotation _ -> "an annotation";
        };
    }
}
