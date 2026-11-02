package com.homing_labs.cq.lang.java.extract;

import com.homing_labs.cq.lang.java.extract.SourceReading.Declared;
import com.homing_labs.cq.lang.java.model.AppliedAnnotation;
import com.homing_labs.cq.lang.java.model.Extensibility;
import com.homing_labs.cq.lang.java.model.ProcessedSource;
import com.homing_labs.cq.lang.java.model.RecordComponentValue;
import com.homing_labs.cq.lang.java.model.TypeParameterValue;
import com.homing_labs.cq.lang.java.model.TypeUse;
import com.homing_labs.cq.lang.java.model.Visibility;
import com.homing_labs.cq.lang.java.outline.ClauseWord;
import com.homing_labs.cq.lang.java.outline.ModifierItem;
import com.homing_labs.cq.lang.java.outline.TokenRange;
import com.homing_labs.cq.lang.java.outline.TypeHeader;
import com.homing_labs.cq.lang.java.outline.TypeText;
import com.homing_labs.cq.lang.java.taxonomy.ObjectKind;
import com.homing_labs.cq.lang.java.taxonomy.Superclass;
import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;
import java.util.Optional;

/**
 * The source of what a class's header says of it: its visibility, how it can be extended, its annotations, its type
 * parameters, its record components and its supertypes, each where the header writes it - or written nowhere.
 */
record HeaderSources() implements StatelessFunctionalObject {

    static final HeaderSources INSTANCE = new HeaderSources();

    ProcessedSource<Visibility> visibility(final Declared declared) {
        return words(declared.header(), "public", "protected", "private").stream()
                .findFirst()
                .<ProcessedSource<Visibility>>map(word -> new ProcessedSource.OfVisibility(
                        declared.lexed().block(word)))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** What a header writes of how its class can be extended: final, sealed and what it permits, or non-sealed. */
    ProcessedSource<Extensibility> extensibility(final Declared declared, final Extensibility value) {
        final TypeHeader header = declared.header();
        final String word = switch (value) {
            case Extensibility.Final _ -> "final";
            case Extensibility.Open _ -> "non-sealed";
            case Extensibility.Sealed _ -> "sealed";
        };
        final Optional<TokenRange> permits = value instanceof Extensibility.Sealed
                ? header.clauses().items().stream()
                .filter(clause -> clause.word().equals(ClauseWord.Permits.INSTANCE))
                .map(clause -> clause.range())
                .findFirst()
                : Optional.empty();
        return words(header, word).stream()
                .findFirst()
                .<ProcessedSource<Extensibility>>map(written -> new ProcessedSource.OfExtensibility(
                        declared.lexed().block(written), permits.map(range -> declared.lexed().block(range))))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** An annotation of the class, by the names its type is written with. */
    ProcessedSource<AppliedAnnotation> annotation(final Declared declared, final AppliedAnnotation value) {
        return declared.header().modifiers().annotations().stream()
                .filter(annotation -> TypeNames.INSTANCE.fits(annotation.name().text(),
                        TypeNames.INSTANCE.qualified(value.annotation().type())))
                .findFirst()
                .<ProcessedSource<AppliedAnnotation>>map(annotation -> new ProcessedSource.OfAnnotation(
                        declared.lexed().block(annotation.range()),
                        annotation.arguments().map(range -> declared.lexed().block(range))))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    ProcessedSource<TypeParameterValue> typeParameter(final Declared declared, final TypeParameterValue value) {
        return declared.header().typeParameters().items().stream()
                .filter(parameter -> parameter.name().text().equals(value.name()))
                .findFirst()
                .<ProcessedSource<TypeParameterValue>>map(parameter -> new ProcessedSource.OfTypeParameter(
                        declared.lexed().block(parameter.range()), declared.lexed().blocks(
                        parameter.bounds().items().stream().map(TypeText::range).toList())))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    ProcessedSource<RecordComponentValue> recordComponent(final Declared declared, final RecordComponentValue value) {
        return declared.header().components().stream()
                .flatMap(components -> components.items().stream())
                .filter(component -> component.name().text().equals(value.name()))
                .findFirst()
                .<ProcessedSource<RecordComponentValue>>map(component -> new ProcessedSource.OfRecordComponent(
                        declared.lexed().block(component.range())))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** The superclass or a superinterface, as the header writes it; one it does not write is implied. */
    ProcessedSource<TypeUse> supertype(final Declared declared, final ObjectKind object, final TypeUse value) {
        final TypeHeader header = declared.header();
        return (object instanceof Superclass ? header.superclass().stream() : header.superinterfaces().stream())
                .filter(type -> TypeNames.INSTANCE.fits(TypeNames.INSTANCE.erased(declared.lexed(), type.range(), 0),
                        TypeNames.INSTANCE.qualified(value)))
                .findFirst()
                .<ProcessedSource<TypeUse>>map(type -> new ProcessedSource.OfSupertype(
                        declared.lexed().block(type.range())))
                .orElseGet(ProcessedSource.Unwritten::new);
    }

    /** The modifier words of a header that are among the words. */
    private List<TokenRange> words(final TypeHeader header, final String... words) {
        final List<String> wanted = List.of(words);
        return header.modifiers().words().stream()
                .filter(word -> wanted.contains(word.word()))
                .map(ModifierItem.Word::range)
                .toList();
    }
}
