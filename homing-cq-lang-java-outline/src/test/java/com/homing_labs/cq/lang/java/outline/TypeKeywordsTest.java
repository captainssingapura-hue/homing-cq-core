package com.homing_labs.cq.lang.java.outline;

import com.homing_labs.cq.lang.java.taxonomy.ClassKind;
import com.homing_labs.cq.lang.java.taxonomy.Taxonomy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeKeywordsTest {

    @Test
    void everyKindOfClassIsDeclaredWithAWordThatDeclaresItBack() {
        for (final ClassKind kind : Taxonomy.INSTANCE.kinds()) {
            final TypeKeyword keyword = TypeKeywords.INSTANCE.of(kind);
            assertEquals(kind, TypeKeywords.INSTANCE.kind(keyword, kind instanceof ClassKind.AbstractClass),
                    kind + " is declared with " + keyword.word());
        }
    }

    @Test
    void everyWordDeclaresAKindOfClass() {
        final Set<Class<?>> words = Arrays.stream(TypeKeyword.class.getPermittedSubclasses())
                .collect(Collectors.toSet());
        assertEquals(words, Taxonomy.INSTANCE.kinds().stream()
                .map(kind -> TypeKeywords.INSTANCE.of(kind).getClass()).collect(Collectors.toSet()));
    }

    @Test
    void theWordsAreWrittenAsJavaWritesThem() {
        assertEquals(List.of("class", "class", "interface", "enum", "record", "@interface"),
                Taxonomy.INSTANCE.kinds().stream().map(kind -> TypeKeywords.INSTANCE.of(kind).word()).toList());
    }
}
