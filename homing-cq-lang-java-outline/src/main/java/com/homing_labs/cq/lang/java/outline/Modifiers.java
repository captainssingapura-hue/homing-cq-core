package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.Vector;

import java.util.List;
import java.util.stream.Stream;

/** The modifiers written before a declaration, words and annotations in the order they are written. */
public record Modifiers(List<ModifierItem> items) implements Vector<ModifierItem> {

    public Modifiers {
        items = List.copyOf(items);
    }

    /** The modifier words, in order. */
    public List<ModifierItem.Word> words() {
        return items.stream().flatMap(item -> item instanceof ModifierItem.Word word
                ? Stream.of(word) : Stream.empty()).toList();
    }

    /** The annotations, in order. */
    public List<ModifierItem.Annotation> annotations() {
        return items.stream().flatMap(item -> item instanceof ModifierItem.Annotation annotation
                ? Stream.of(annotation) : Stream.empty()).toList();
    }

    /** Whether the word is among them. */
    public boolean has(final String word) {
        return words().stream().anyMatch(written -> written.word().equals(word));
    }
}
