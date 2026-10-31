package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

/**
 * The words types are declared with, as the taxonomy's kinds of class are written: each kind is declared with one
 * word, and a word, written {@code abstract} or not, declares one kind. A kind added to the taxonomy, or a word added
 * here, does not compile until both directions say what it goes with.
 */
public record TypeKeywords() implements StatelessFunctionalObject {

    public static final TypeKeywords INSTANCE = new TypeKeywords();

    /** The word a class of the kind is declared with. */
    public TypeKeyword of(final ClassKind kind) {
        return switch (kind) {
            case ClassKind.Class _, ClassKind.AbstractClass _ -> TypeKeyword.Class.INSTANCE;
            case ClassKind.Interface _ -> TypeKeyword.Interface.INSTANCE;
            case ClassKind.Enum _ -> TypeKeyword.Enum.INSTANCE;
            case ClassKind.Record _ -> TypeKeyword.Record.INSTANCE;
            case ClassKind.Annotation _ -> TypeKeyword.Annotation.INSTANCE;
        };
    }

    /**
     * The kind of a class declared with the word, written {@code abstract} or not. Only a class is made another kind
     * by it: an interface is abstract anyway, and an enum, a record or an annotation interface cannot be written so.
     */
    public ClassKind kind(final TypeKeyword keyword, final boolean writtenAbstract) {
        return switch (keyword) {
            case TypeKeyword.Class _ -> writtenAbstract ? ClassKind.AbstractClass.INSTANCE : ClassKind.Class.INSTANCE;
            case TypeKeyword.Interface _ -> ClassKind.Interface.INSTANCE;
            case TypeKeyword.Enum _ -> ClassKind.Enum.INSTANCE;
            case TypeKeyword.Record _ -> ClassKind.Record.INSTANCE;
            case TypeKeyword.Annotation _ -> ClassKind.Annotation.INSTANCE;
        };
    }
}
