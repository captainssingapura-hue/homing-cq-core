package com.homing_labs.cq.lang.java.def;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Makes the unit of a top-level class, of the class's kind. The kind is read from the loaded class, as reflection sees
 * it - the same kind the taxonomy's scanner reads from its class file: an annotation, an interface, an enum, a record,
 * an abstract class, or else a class.
 */
public record JavaUnits() implements StatelessFunctionalObject {

    public static final JavaUnits INSTANCE = new JavaUnits();

    /** The unit of a top-level class, with its source if it was found. */
    public JavaUnit of(final Class<?> type, final Optional<JavaSource> source) {
        return switch (kindOf(type)) {
            case ClassKind.Class _ -> new JavaUnit.OfClass(type, source);
            case ClassKind.AbstractClass _ -> new JavaUnit.OfAbstractClass(type, source);
            case ClassKind.Interface _ -> new JavaUnit.OfInterface(type, source);
            case ClassKind.Enum _ -> new JavaUnit.OfEnum(type, source);
            case ClassKind.Record _ -> new JavaUnit.OfRecord(type, source);
            case ClassKind.Annotation _ -> new JavaUnit.OfAnnotation(type, source);
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

    /** Refuses a class that is nested, or that is not of the kind a unit is for. */
    void check(final Class<?> type, final ClassKind kind) {
        if (type.getEnclosingClass() != null) {
            throw new IllegalArgumentException("A JavaUnit is a top-level class, but " + type.getName()
                    + " is nested in " + type.getEnclosingClass().getName());
        }
        final ClassKind actual = kindOf(type);
        if (!actual.equals(kind)) {
            throw new IllegalArgumentException(type.getName() + " is " + name(actual) + ", not " + name(kind));
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
