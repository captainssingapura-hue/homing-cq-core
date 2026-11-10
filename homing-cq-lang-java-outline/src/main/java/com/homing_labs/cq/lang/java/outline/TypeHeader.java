package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import hue.captains.singapura.tao.ontology.Scalar;

import java.util.List;
import java.util.Optional;

/**
 * The header of a type as written, up to its body's brace: its documentation comment, its modifiers, the word it is
 * declared with, its name, its type parameters, a record's components, and its clauses. The types in it are kept as
 * written; reading them is left to a later stage.
 */
public record TypeHeader(Optional<DocComment> doc, Modifiers modifiers, TypeKeyword keyword, Name name,
                         TypeParameters typeParameters, Optional<RecordComponents> components, Clauses clauses,
                         TokenRange range) implements Scalar {

    /** The kind of class the header declares, as the taxonomy has it. */
    public ClassKind kind() {
        return TypeKeywords.INSTANCE.kind(keyword, modifiers.has("abstract"));
    }

    /** The class a class extends, as written; none for a class that extends none, or for any other kind. */
    public Optional<TypeText> superclass() {
        return keyword instanceof TypeKeyword.Class ? listed(ClauseWord.Extends.INSTANCE).stream().findFirst()
                : Optional.empty();
    }

    /** The interfaces the type extends - an interface - or implements - any other kind - as written. */
    public List<TypeText> superinterfaces() {
        return listed(keyword instanceof TypeKeyword.Interface ? ClauseWord.Extends.INSTANCE
                : ClauseWord.Implements.INSTANCE);
    }

    /** The types the type permits to extend it, as written. */
    public List<TypeText> permitted() {
        return listed(ClauseWord.Permits.INSTANCE);
    }

    /** The types the first clause with the word lists, or none. */
    private List<TypeText> listed(final ClauseWord word) {
        return clauses.items().stream().filter(clause -> clause.word().equals(word)).findFirst()
                .map(clause -> clause.types().items()).orElse(List.of());
    }
}
