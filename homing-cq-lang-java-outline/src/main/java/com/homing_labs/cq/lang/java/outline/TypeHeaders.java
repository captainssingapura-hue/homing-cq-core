package com.homing_labs.cq.lang.java.outline;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Reads a type's declaration where a scan is: the word it is declared with, its header - its name, its type
 * parameters, its record components and its clauses - and its body, stepped over. The type parameters and the lists of
 * types a method's or a constructor's header has are read the same way.
 */
record TypeHeaders() implements StatelessFunctionalObject {

    static final TypeHeaders INSTANCE = new TypeHeaders();

    /** The word a type is declared with, if one is here. */
    Optional<TypeKeyword> keyword(final TokenScan scan) {
        if (scan.is("class")) {
            return Optional.of(TypeKeyword.Class.INSTANCE);
        }
        if (scan.is("interface")) {
            return Optional.of(TypeKeyword.Interface.INSTANCE);
        }
        if (scan.is("enum")) {
            return Optional.of(TypeKeyword.Enum.INSTANCE);
        }
        if (scan.is("@") && scan.is(scan.next(scan.at()), "interface")) {
            return Optional.of(TypeKeyword.Annotation.INSTANCE);
        }
        if (scan.is("record") && scan.identifier(scan.next(scan.at()))) {
            return Optional.of(TypeKeyword.Record.INSTANCE);
        }
        return Optional.empty();
    }

    /**
     * A type's header, from its modifiers, already read, to its body's brace: its documentation comment is the one
     * before its start. The comments queued are said where the header ends.
     */
    TypeHeader read(final TokenScan scan, final int start, final List<ModifierItem> modifiers,
                    final TypeKeyword keyword) {
        final Optional<DocComment> doc = scan.attach(start);
        if (keyword instanceof TypeKeyword.Annotation) {
            scan.advance();
        }
        scan.advance();
        final Name name = name(scan);
        final List<TypeParameter> parameters = scan.is("<") ? typeParameters(scan) : List.of();
        final Optional<List<RecordComponent>> components = scan.is("(") ? Optional.of(recordComponents(scan))
                : Optional.empty();
        final List<Clause> clauses = clauses(scan);
        scan.docs.flush(scan.at(), doc);
        return new TypeHeader(doc, new Modifiers(modifiers), keyword, name, new TypeParameters(parameters),
                components.map(RecordComponents::new), new Clauses(clauses), new TokenRange(start, scan.last() + 1));
    }

    /**
     * Steps over a type's body: from the brace that opens it - after what cannot be read, up to where a part can
     * start, if the brace is not next - to the brace that closes it, or the end. A body no brace opens is empty, where
     * it should have been opened.
     */
    TypeBody body(final TokenScan scan, final IntPredicate startsPart) {
        if (!scan.is("{")) {
            scan.error("'{' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            while (scan.at() < scan.size() && !scan.is("{") && !startsPart.test(scan.at())) {
                scan.advance();
            }
        }
        if (!scan.is("{")) {
            return new TypeBody(new TokenRange(scan.at(), scan.at()), BodyEnd.Missing.INSTANCE);
        }
        final int open = scan.at();
        return scan.skipBlock() ? new TypeBody(new TokenRange(open, scan.last() + 1), BodyEnd.Closed.INSTANCE)
                : new TypeBody(new TokenRange(open, scan.size()), BodyEnd.Unterminated.INSTANCE);
    }

    List<TypeParameter> typeParameters(final TokenScan scan) {
        final List<TypeParameter> parameters = new ArrayList<>();
        scan.advance();
        if (scan.is(">")) {
            scan.error("<identifier> expected", scan.at(), scan.at() + 1);
        }
        while (scan.at() < scan.size() && !scan.is(">")) {
            final int start = scan.at();
            final List<ModifierItem> annotations = scan.modifiers();
            scan.annotationsOnly(annotations);
            if (!scan.identifier(scan.at())) {
                scan.error("<identifier> expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
                return parameters;
            }
            final Name name = name(scan);
            List<TypeText> bounds = List.of();
            if (scan.is("extends")) {
                scan.advance();
                bounds = types(scan, "&", Set.of(",", ">"));
            }
            parameters.add(new TypeParameter(new Modifiers(annotations), name, new TypeTexts(bounds),
                    new TokenRange(start, scan.last() + 1)));
            if (scan.is(",")) {
                scan.advance();
            } else if (!scan.is(">")) {
                scan.error("'>' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
                return parameters;
            }
        }
        if (scan.is(">")) {
            scan.advance();
        }
        return parameters;
    }

    /**
     * Types as written, with the separator between them. Each runs to the separator, or to one of the stops, outside
     * any angle brackets and parentheses it opens; a brace or a semicolon ends them all, as no type has one.
     */
    List<TypeText> types(final TokenScan scan, final String separator, final Set<String> stops) {
        final List<TypeText> types = new ArrayList<>();
        while (true) {
            final int start = scan.at();
            int angles = 0;
            int parentheses = 0;
            while (scan.at() < scan.size()) {
                if (parentheses == 0 && (scan.is("{") || scan.is(";") || scan.is(")"))
                        || parentheses == 0 && angles == 0
                        && (scan.is(separator) || stops.stream().anyMatch(scan::is))) {
                    break;
                }
                if (scan.is("(")) {
                    parentheses++;
                } else if (scan.is(")")) {
                    parentheses--;
                } else if (parentheses == 0 && scan.is("<")) {
                    angles++;
                } else if (parentheses == 0 && scan.is(">")) {
                    if (angles == 0) {
                        break;
                    }
                    angles--;
                }
                scan.advance();
            }
            if (scan.at() == start) {
                scan.error("<identifier> expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            } else {
                types.add(new TypeText(new TokenRange(start, scan.last() + 1)));
                if (angles > 0) {
                    scan.error("'>' expected", start, scan.last() + 1);
                }
            }
            if (!scan.is(separator)) {
                return types;
            }
            scan.advance();
        }
    }

    /** A type's name: one identifier, which may not be a restricted one. */
    private Name name(final TokenScan scan) {
        if (!scan.identifier(scan.at())) {
            scan.error("<identifier> expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
            return new Name("", new TokenRange(scan.at(), scan.at()));
        }
        final int index = scan.at();
        scan.advance();
        final Name name = new Name(scan.text(index), new TokenRange(index, index + 1));
        if (restrictedName(name.text())) {
            scan.error("'" + name.text() + "' is a restricted name: it cannot name a type", name.range());
        }
        return name;
    }

    /** Whether no type may be named the word, so the grammar can read it as a keyword where it has one (JLS 3.9). */
    private boolean restrictedName(final String word) {
        return switch (word) {
            case "permits", "record", "sealed", "var", "yield" -> true;
            default -> false;
        };
    }

    private List<RecordComponent> recordComponents(final TokenScan scan) {
        final List<RecordComponent> components = new ArrayList<>();
        scan.advance();
        while (scan.at() < scan.size() && !scan.is(")")) {
            final int start = scan.at();
            final List<ModifierItem> annotations = scan.modifiers();
            scan.annotationsOnly(annotations);
            final int typeStart = scan.at();
            final List<Integer> read = new ArrayList<>();
            int angles = 0;
            int parentheses = 0;
            while (scan.at() < scan.size() && !(parentheses == 0
                    && (scan.is("{") || scan.is(";") || scan.is(")") || angles == 0 && scan.is(",")))) {
                if (scan.is("(")) {
                    parentheses++;
                } else if (scan.is(")")) {
                    parentheses--;
                } else if (parentheses == 0 && scan.is("<")) {
                    angles++;
                } else if (parentheses == 0 && scan.is(">")) {
                    angles--;
                }
                read.add(scan.at());
                scan.advance();
            }
            final int count = read.size();
            final boolean variable = count >= 3 && scan.is(read.get(count - 2), "...");
            final int typeTokens = count - (variable ? 2 : 1);
            if (count < 2 || typeTokens < 1 || !scan.identifier(read.getLast())) {
                scan.error("<identifier> expected", start, Math.max(start + 1, scan.last() + 1));
            } else {
                final int name = read.getLast();
                components.add(new RecordComponent(new Modifiers(annotations),
                        new TypeText(new TokenRange(typeStart, read.get(typeTokens - 1) + 1)), variable,
                        new Name(scan.text(name), new TokenRange(name, name + 1)),
                        new TokenRange(start, scan.last() + 1)));
            }
            if (scan.is(",")) {
                scan.advance();
            } else if (!scan.is(")")) {
                scan.error("')' expected", scan.at(), Math.min(scan.at() + 1, scan.size()));
                return components;
            }
        }
        if (scan.is(")")) {
            scan.advance();
        }
        return components;
    }

    private List<Clause> clauses(final TokenScan scan) {
        final List<Clause> clauses = new ArrayList<>();
        while (scan.is("extends") || scan.is("implements") || scan.is("permits")) {
            final int start = scan.at();
            final ClauseWord word = scan.is("extends") ? ClauseWord.Extends.INSTANCE
                    : scan.is("implements") ? ClauseWord.Implements.INSTANCE : ClauseWord.Permits.INSTANCE;
            scan.advance();
            final List<TypeText> types = types(scan, ",", Set.of("extends", "implements", "permits"));
            clauses.add(new Clause(word, new TypeTexts(types), new TokenRange(start, scan.last() + 1)));
        }
        return clauses;
    }
}
