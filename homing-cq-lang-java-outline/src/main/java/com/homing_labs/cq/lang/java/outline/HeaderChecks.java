package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What the grammar asks of a declaration's header beyond its shape: which modifiers it may have, and - a type's -
 * which type parameters, components and clauses. What a header does not hold to is reported to the scan that read it.
 */
record HeaderChecks() implements StatelessFunctionalObject {

    static final HeaderChecks INSTANCE = new HeaderChecks();

    /**
     * What the grammar asks of a type's header beyond its shape: which modifiers it takes - at the top level, or as a
     * member of a type declared with the enclosing keyword - and which parameters and clauses.
     */
    void checkHeader(final TokenScan scan, final TypeHeader header, final Optional<TypeKeyword> enclosing) {
        final TypeKeyword keyword = header.keyword();
        final Set<String> allowed = new HashSet<>(allowed(keyword));
        if (enclosing.isEmpty()) {
            allowed.removeAll(memberOnly());
        } else if (enclosing.get() instanceof TypeKeyword.Interface
                || enclosing.get() instanceof TypeKeyword.Annotation) {
            allowed.removeAll(Set.of("protected", "private"));
        }
        checkModifiers(scan, header.modifiers(), allowed, true, header.range());
        final List<TypeParameter> parameters = header.typeParameters().items();
        if (!parameters.isEmpty() && (keyword instanceof TypeKeyword.Enum
                || keyword instanceof TypeKeyword.Annotation)) {
            scan.error(noun(keyword) + " takes no type parameters", parameters.getFirst().range().from(),
                    parameters.getLast().range().to());
        }
        if (keyword instanceof TypeKeyword.Record && header.components().isEmpty()) {
            scan.error("a record declares its components, in parentheses after its name", header.name().range());
        } else if (!(keyword instanceof TypeKeyword.Record) && header.components().isPresent()) {
            scan.error("only a record has components", header.name().range());
        }
        checkClauses(scan, keyword, header.clauses().items());
    }

    /**
     * What the grammar asks of the modifier words written before a declaration: each written once, each one it may
     * have, and - when it has none it may not - no two that contradict each other. strictfp, where it may be written,
     * is not needed.
     */
    void checkModifiers(final TokenScan scan, final Modifiers modifiers, final Set<String> allowed,
                        final boolean type, final TokenRange declaration) {
        final Set<String> written = new HashSet<>();
        boolean disallowed = false;
        for (final ModifierItem.Word word : modifiers.words()) {
            if (!written.add(word.word())) {
                scan.error("repeated modifier", word.range());
            } else if (!allowed.contains(word.word())) {
                scan.error("modifier " + word.word() + " not allowed here", word.range());
                disallowed = true;
            }
        }
        if (!disallowed) {
            contradiction(written, type).ifPresent(pair -> scan.error("illegal combination of modifiers: " + pair,
                    declaration));
        }
        if (allowed.contains("strictfp")) {
            modifiers.words().stream().filter(word -> word.word().equals("strictfp")).findFirst()
                    .ifPresent(word -> scan.warning("strictfp is not required: since Java 17 all floating-point"
                            + " arithmetic is strict", word.range()));
        }
    }

    String noun(final TypeKeyword keyword) {
        return switch (keyword) {
            case TypeKeyword.Class _ -> "a class";
            case TypeKeyword.Interface _ -> "an interface";
            case TypeKeyword.Enum _ -> "an enum";
            case TypeKeyword.Record _ -> "a record";
            case TypeKeyword.Annotation _ -> "an annotation interface";
        };
    }

    /** The first two words written that contradict each other, as javac checks them in turn. */
    private Optional<String> contradiction(final Set<String> written, final boolean type) {
        final List<List<List<String>>> contradictions = contradictions();
        final List<String> flagOrder = flagOrder();
        for (int index = 0; index < contradictions.size(); index++) {
            if (type && (index == 0 || index == 6)) {
                continue;
            }
            final Optional<String> first = firstWritten(written, contradictions.get(index).get(0), flagOrder);
            final Optional<String> second = firstWritten(written, contradictions.get(index).get(1), flagOrder);
            if (first.isPresent() && second.isPresent()) {
                return Optional.of(first.get() + " and " + second.get());
            }
        }
        return Optional.empty();
    }

    private Optional<String> firstWritten(final Set<String> written, final List<String> words,
                                          final List<String> flagOrder) {
        return flagOrder.stream().filter(words::contains).filter(written::contains).findFirst();
    }

    /**
     * Modifiers no declaration may have one of the first and one of the second of, in the order javac checks them
     * (Check.checkFlags); it says the first two it finds only. A type is not checked against the first nor the
     * seventh.
     */
    private List<List<List<String>>> contradictions() {
        return List.of(
                List.of(List.of("abstract"), List.of("private", "static", "default")),
                List.of(List.of("static", "private"), List.of("default")),
                List.of(List.of("abstract"), List.of("final", "native", "synchronized")),
                List.of(List.of("public"), List.of("private", "protected")),
                List.of(List.of("private"), List.of("public", "protected")),
                List.of(List.of("final"), List.of("volatile")),
                List.of(List.of("abstract", "native"), List.of("strictfp")),
                List.of(List.of("final"), List.of("sealed", "non-sealed")),
                List.of(List.of("sealed"), List.of("final", "non-sealed")));
    }

    /** The modifier words in the order of javac's flags for them, which is the order it names two of them in. */
    private List<String> flagOrder() {
        return List.of("public", "private", "protected", "static", "final", "synchronized", "volatile", "transient",
                "native", "abstract", "strictfp", "default", "sealed", "non-sealed");
    }

    /** Modifiers that place a type in another type, which a top-level type is not. */
    private Set<String> memberOnly() {
        return Set.of("protected", "private", "static");
    }

    private void checkClauses(final TokenScan scan, final TypeKeyword keyword, final List<Clause> clauses) {
        final Set<ClauseWord> seen = new HashSet<>();
        Optional<ClauseWord> furthest = Optional.empty();
        for (final Clause clause : clauses) {
            final ClauseWord word = clause.word();
            if (!takes(keyword, word)) {
                scan.error(noun(keyword) + " has no " + word.word() + " clause", clause.range());
            } else if (!seen.add(word)) {
                scan.error("a second " + word.word() + " clause", clause.range());
            } else if (furthest.isPresent() && word.order() < furthest.get().order()) {
                scan.error("the " + word.word() + " clause comes before the " + furthest.get().word() + " clause",
                        clause.range());
            } else if (keyword instanceof TypeKeyword.Class && word instanceof ClauseWord.Extends
                    && clause.types().items().size() > 1) {
                scan.error("a class extends one class only", clause.range());
            }
            if (furthest.isEmpty() || word.order() > furthest.get().order()) {
                furthest = Optional.of(word);
            }
        }
    }

    /** The modifiers a type declared with the word may have, wherever it is declared (JLS 8.1.1, 8.9, 8.10, 9.1.1). */
    private Set<String> allowed(final TypeKeyword keyword) {
        return switch (keyword) {
            case TypeKeyword.Class _ -> Set.of("public", "protected", "private", "abstract", "static", "final",
                    "sealed", "non-sealed", "strictfp");
            case TypeKeyword.Interface _ -> Set.of("public", "protected", "private", "abstract", "static", "sealed",
                    "non-sealed", "strictfp");
            case TypeKeyword.Enum _ -> Set.of("public", "protected", "private", "static", "strictfp");
            case TypeKeyword.Record _ -> Set.of("public", "protected", "private", "static", "final", "strictfp");
            case TypeKeyword.Annotation _ -> Set.of("public", "protected", "private", "abstract", "static",
                    "strictfp");
        };
    }

    /** Whether a type declared with the word has a clause starting with the other. */
    private boolean takes(final TypeKeyword keyword, final ClauseWord word) {
        return switch (keyword) {
            case TypeKeyword.Class _ -> true;
            case TypeKeyword.Interface _ -> !(word instanceof ClauseWord.Implements);
            case TypeKeyword.Enum _, TypeKeyword.Record _ -> word instanceof ClauseWord.Implements;
            case TypeKeyword.Annotation _ -> false;
        };
    }
}
