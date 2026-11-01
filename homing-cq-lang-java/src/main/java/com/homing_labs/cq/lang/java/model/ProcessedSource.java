package com.homing_labs.cq.lang.java.model;

import com.homing_labs.cq.lang.java.outline.CodeBlock;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.Optional;

/**
 * What the source says of a value of the model, by the kind of value it is. A value written in a block of code has the
 * block, copied out of the file, and what the source tells of the value there that the class file does not - a
 * method's documentation comment, its parameters' names and its body, say. The static initialiser is written in
 * several blocks, which javac puts it together from. A value the source writes no block of has a source all the same,
 * saying why: it is written somewhere inside code not read yet, or nowhere, or it is not looked for, or its unit was
 * read without its source file.
 *
 * @param <T> the kind of value
 */
public sealed interface ProcessedSource<T extends Scalar> extends Scalar {

    /** Written in a block of code: the block, always, and what the source tells of the value there. */
    sealed interface Written<T extends Scalar> extends ProcessedSource<T> {

        /** The code the value is written in, from its documentation comment, if it has one. */
        CodeBlock block();
    }

    /** A member class: its declaration, and its documentation comment, its header and its body as written. */
    record OfNestedClass(CodeBlock block, Optional<CodeBlock> doc, CodeBlock header, Optional<CodeBlock> body)
            implements Written<NestedClass> {
    }

    /**
     * A field: its declaration, which can declare other fields too, and its documentation comment, its own
     * declarator, and its initial value.
     */
    record OfField(CodeBlock block, Optional<CodeBlock> doc, CodeBlock declarator, Optional<CodeBlock> initialValue)
            implements Written<FieldValue> {
    }

    /**
     * A method: its declaration, and its documentation comment, its parameters as written, its body, and the default
     * value of an annotation interface's element.
     */
    record OfMethod(CodeBlock block, Optional<CodeBlock> doc, WrittenParameters parameters, Optional<CodeBlock> body,
                    Optional<CodeBlock> defaultValue) implements Written<MethodValue> {
    }

    /**
     * A constructor: its declaration, and its documentation comment, its parameters as written - a record's compact
     * one takes the record's components - its body, and the instance initialisers and instance fields' initial values
     * it runs, which are none when it hands over to another constructor with {@code this(...)}.
     */
    record OfConstructor(CodeBlock block, Optional<CodeBlock> doc, WrittenParameters parameters,
                         Optional<CodeBlock> body, CodeBlocks initialisers) implements Written<ConstructorValue> {
    }

    /** An enum constant, and its documentation comment, the arguments it is made with and its own class body. */
    record OfEnumConstant(CodeBlock block, Optional<CodeBlock> doc, Optional<CodeBlock> arguments,
                          Optional<CodeBlock> body) implements Written<EnumConstantValue> {
    }

    /** A record's component: its annotations, its type and its name. */
    record OfRecordComponent(CodeBlock block) implements Written<RecordComponentValue> {
    }

    /** A type parameter, and the types it extends as written. */
    record OfTypeParameter(CodeBlock block, CodeBlocks bounds) implements Written<TypeParameterValue> {
    }

    /** An annotation of the class, and its arguments as written, in their parentheses. */
    record OfAnnotation(CodeBlock block, Optional<CodeBlock> arguments) implements Written<AppliedAnnotation> {
    }

    /** A superclass or a superinterface, as written, its type arguments and all. */
    record OfSupertype(CodeBlock block) implements Written<TypeUse> {
    }

    /** The class's visibility: the modifier that says it. */
    record OfVisibility(CodeBlock block) implements Written<Visibility> {
    }

    /** How the class can be extended: the modifier that says it, and, for a sealed class, what it permits. */
    record OfExtensibility(CodeBlock block, Optional<CodeBlock> permits) implements Written<Extensibility> {
    }

    /**
     * The static initialiser: the blocks javac puts it together from, in the order written - an enum's constants, the
     * static blocks, and the static fields' initial values but the constants'.
     */
    record OfInitialiser(CodeBlocks blocks) implements ProcessedSource<InitialiserValue> {
    }

    /**
     * Written somewhere inside these blocks, in code not read yet: a local or an anonymous class in a method's body,
     * say, or a member of a type nested two levels down.
     */
    record Within<T extends Scalar>(CodeBlocks blocks) implements ProcessedSource<T> {
    }

    /**
     * Written nowhere: the compiler implied or generated it - an enum's {@code values()}, a record's accessors, a
     * default constructor, a bridge method - or the source is not the one the class was compiled from.
     */
    record Unwritten<T extends Scalar>() implements ProcessedSource<T> {
    }

    /** Not looked for: a value of its kind - a dependency, named wherever the class uses it - is not placed yet. */
    record Unplaced<T extends Scalar>() implements ProcessedSource<T> {
    }

    /** Not known: the unit was read without its source file. */
    record Unavailable<T extends Scalar>() implements ProcessedSource<T> {
    }
}
